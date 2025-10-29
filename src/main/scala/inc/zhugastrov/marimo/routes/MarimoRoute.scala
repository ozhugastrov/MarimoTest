package inc.zhugastrov.marimo.routes

import cats.effect.std.Queue
import cats.effect.{IO, Ref}
import fs2.Stream
import inc.zhugastrov.marimo.Main
import inc.zhugastrov.marimo.k8s.KubernetesService
import inc.zhugastrov.marimo.utils.Utils.serviceName
import inc.zhugastrov.marimo.utils.WebSocketUtils.*
import io.fabric8.kubernetes.client.{KubernetesClient, KubernetesClientBuilder}
import org.http4s.*
import org.http4s.Uri.Path.Segment
import org.http4s.Uri.Scheme
import org.http4s.client.Client
import org.http4s.client.websocket.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.request.Path
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import org.typelevel.ci.CIString
import scodec.bits.ByteVector

import scala.language.implicitConversions
import inc.zhugastrov.marimo.utils.WebSocketUtils

import scala.concurrent.duration.DurationInt
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

          val newReq = req.withUri(newUri).withHeaders(req.headers.headers)

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
        k8sService.createDeployment(name).flatMap(_ => Ok(s"created $name"))

      case DELETE -> Root / "notebook" / name =>
        k8sService.deleteDeployment(name).flatMap(_ => Ok(s"deleted $name"))

      case req@_ -> Root / "user" / user / "ws" =>

        import WebSocketUtils.given
        val httpUri = Uri.unsafeFromString(findServiceAddress(user).get)
        val wsUri = httpUri.copy(
          scheme = Some(Scheme.unsafeFromString("ws")),
          path = Uri.Path.unsafeFromString("/ws"),
          query = req.uri.query
        )
        for {
          heartbeatSend <- IO.apply(Stream.awakeEvery[IO](30.seconds).map(_ =>
            WebSocketFrame.Ping()))
          heartbeatReceive <- IO.apply(Stream.awakeEvery[IO](30.seconds).map(_ =>
            WebSocketFrame.Ping()))

          heartbeatForward1 <- IO.apply(Stream.awakeEvery[IO](30.seconds).map(_ =>
            WebSocketFrame.Ping()))

          heartbeatForward2 <- IO.apply(Stream.awakeEvery[IO](30.seconds).map[WSFrame](_ =>
            WSFrame.Ping(ByteVector.empty)))

          toTarget <- Queue.unbounded[IO, WebSocketFrame]
          toClient <- Queue.unbounded[IO, WebSocketFrame]

          response <- wsBuilder.build(
            send = fs2.Stream.fromQueueUnterminated(toClient).merge(heartbeatSend)
            ,
            receive = _.merge(heartbeatReceive).evalMap(toTarget.offer)
          ).flatTap { _ =>
            wsClient
              .connect(WSRequest(wsUri))
              .use { targetConn =>
                val forward1 = fs2.Stream.fromQueueUnterminated(toTarget)
                  .merge(heartbeatForward1)
                  .evalMap(targetConn.send(_))
                val forward2 = targetConn.receiveStream
                  .merge(heartbeatForward2)
                  .evalMap(toClient.offer(_))
                forward1.concurrently(forward2)
                  .compile.drain
              }.start
          }
        } yield response


      case req@_ -> "user" /:userPath =>
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
      case req@_ =>
        IO.println(s"MISSING PATH ${req.uri}") >> Ok("MISSING PATH MAPPING")
    }
  }

}
