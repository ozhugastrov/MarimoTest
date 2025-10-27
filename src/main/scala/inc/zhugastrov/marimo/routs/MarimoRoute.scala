package inc.zhugastrov.marimo.routs

import cats.effect.{IO, Ref}
import inc.zhugastrov.marimo.Main
import inc.zhugastrov.marimo.k8s.KubernetesService
import inc.zhugastrov.marimo.utils.Utils.serviceName
import io.fabric8.kubernetes.client.{KubernetesClient, KubernetesClientBuilder}
import org.http4s.*
import org.http4s.Uri.Path.Segment
import org.http4s.Uri.Scheme
import org.http4s.client.Client
import org.http4s.client.websocket.{WSClient, WSConnection, WSFrame, WSRequest}
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.request.Path
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import scodec.bits.ByteVector

import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

object MarimoRoute {

  private val k8sService = Main.injector.getInstance(classOf[KubernetesService])

  def marimoRoutes(client: Client[IO],
                   wsClient: WSClient[IO],
                   wsBuilder: WebSocketBuilder2[IO],
                   connectionsRef: Ref[IO, Map[(String, String), WSConnection[IO]]]
                  ): HttpRoutes[IO] = {

    import dsl.*
    val dsl = Http4sDsl[IO]
    val k8sClient: KubernetesClient = new KubernetesClientBuilder().build()

    def findServiceAddress(user: String): Option[String] = {
      val name = serviceName(user)
      val svc = k8sClient.services().inNamespace("default").withName(name).get()
      Option(svc).flatMap { s =>
        Option(s.getSpec.getClusterIP).map { ip =>
          val port = s.getSpec.getPorts.asScala.headOption.map(_.getPort).getOrElse(8080)
          s"http://$ip:$port"
        }
      }
    }


    def queryProxy(req: Request[IO], user: String, rest: Vector[Segment]) = {
      val relativePath = rest.mkString("/")

      Try(findServiceAddress(user)) match {
        case Failure(e) =>
          IO(e.printStackTrace()) >> IO.raiseError(e)

        case Success(Some(base)) =>
          val baseUri = Uri.unsafeFromString(base)
          val newUri = baseUri
            .withPath(baseUri.path.concat(Path.apply(rest)))
            .withQueryParams(req.uri.query.params)

          val newReq = req.withUri(newUri) //.withHeaders(req.headers.headers)

          client.run(newReq).allocated.flatMap { case (resp, release) =>
            IO {
              Response[IO](
                status = resp.status,
                headers = Headers(resp.headers.headers),
                body = resp.body.onFinalize(release)
              ).addCookie(ResponseCookie("marimo_user", user))
            }
          }
        case Success(None) =>
          NotFound(s"Service for user '$user' not found in Kubernetes")
      }
    }

    HttpRoutes.of[IO] {
      case PUT -> Root / "notebook" / name =>
        println("starting creating deployment")
        k8sService.createDeployment(name).flatMap(_ => Ok(s"created $name"))
      case DELETE -> Root / "notebook" / name =>
        k8sService.deleteDeployment(name).flatMap(_ => Ok(s"deleted $name"))
      case req@_ -> Root / "user" / user / "ws" =>
        findServiceAddress(user) match {
          case Some(base) =>
            val httpUri = Uri.unsafeFromString(base)
            val sessionId = req.uri.params.getOrElse("session_id", "unknown")
            val key = (user, sessionId)

            val wsUri = httpUri.copy(
              scheme = Some(Scheme.unsafeFromString("ws")), // or "wss" for secure pods
              path = Uri.Path.unsafeFromString("/ws"),
              query = req.uri.query
            )

            println(s"forward ws connection $wsUri")
            for {
              maybeConn <- connectionsRef.get.map(_.get(key))
              conn <- maybeConn.fold(
                wsClient.connect(WSRequest(wsUri)).allocated.flatMap {
                  case (newConn, _) =>
                    connectionsRef.update(_ + (key -> newConn)) *> IO.pure(newConn)
                }
              )(IO.pure)
              resp <- wsClient.connect(WSRequest(wsUri)).allocated.flatMap {
                case (conn, release) =>
                  wsBuilder.build(
                    conn.receiveStream.collect {
                      case WSFrame.Text(msg, _) => WebSocketFrame.Text(msg)
                      case WSFrame.Binary(data, _) => WebSocketFrame.Binary(data)
                      case WSFrame.Close(code, reason) => WebSocketFrame.Close(ByteVector.apply(Array(code.toByte).appendedAll(reason.getBytes)))
                      case WSFrame.Ping(data) => WebSocketFrame.Ping(data)
                      case WSFrame.Pong(data) => WebSocketFrame.Pong(data)
                    },
                    _.evalMap {
                      case WebSocketFrame.Text(msg, _) => conn.send(WSFrame.Text(msg))
                      case WebSocketFrame.Binary(data, _) => conn.send(WSFrame.Binary(data))
                      case cl@WebSocketFrame.Close(data) => conn.send(WSFrame.Close(cl.closeCode, cl.reason))
                      case WebSocketFrame.Ping(data) => conn.send(WSFrame.Ping(data))
                      case WebSocketFrame.Pong(data) => conn.send(WSFrame.Pong(data))
                    }
                  ).guarantee(release) // ensure the upstream connection is released when the WS closes
              }
            } yield resp
          //            responseResource

          case None =>
            NotFound(s"Service for user '$user' not found")
        }
      case req@_ -> "user" /: userPath =>
        val segments = userPath.segments
        req.cookies.find(_.name == "marimo_user").map(nameCookie =>
          queryProxy(req, nameCookie.content, segments.filterNot(_.decoded() == nameCookie.content))
        ).getOrElse(
          segments.toList match {
            case user :: rest =>
              queryProxy(req, user.decoded(), rest.toVector)
            case _ =>
              BadRequest("Missing user in path")
          }
        )
    }
  }

}
