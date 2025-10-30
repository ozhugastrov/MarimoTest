package inc.zhugastrov.marimo.server

import cats.effect.std.Console
import cats.effect.{Async, Resource}
import com.comcast.ip4s.*
import fs2.text
import org.http4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.middleware.Logger
import org.http4s.server.websocket.WebSocketBuilder2


object Server {

  def createServer[F[_] : {Async, Console}](
                                           service:  WebSocketBuilder2[F] => HttpRoutes[F]
                                         ): Resource[F, Server] = {
    def loggerService(wsb:  WebSocketBuilder2[F]) = Logger.httpRoutesLogBodyText[F](
      logHeaders = true,
      logBody = r => Some(r.through(text.utf8.decode).compile.string),
      redactHeadersWhen = _ => false,
      logAction = Some((msg: String) => Console[F].println(msg.take(1000)))
    )(service(wsb)).orNotFound

    EmberServerBuilder
      .default[F]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpWebSocketApp(wsb => loggerService(wsb))
      .build
  }
}




