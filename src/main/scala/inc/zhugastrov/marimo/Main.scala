package inc.zhugastrov.marimo

import cats.effect.*
import com.google.inject.{Guice, Injector}
import inc.zhugastrov.marimo.db.migration.Migrator
import inc.zhugastrov.marimo.routs.MarimoRoute.marimoRoutes
import inc.zhugastrov.marimo.server.Server
import org.http4s.client.websocket.WSConnection
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.jdkhttpclient.JdkWSClient
import org.http4s.server.{Router, Server}

import java.net.http.HttpClient

object Main extends IOApp {
  lazy val injector: Injector = Guice.createInjector(new Module)

  private val migrator = injector.getInstance(classOf[Migrator])

  migrator.migrate()



  private def program: Resource[IO, Server] = {
    val connectionsRef: Ref[IO, Map[(String, String), WSConnection[IO]]] = Ref.unsafe[IO, Map[(String, String), WSConnection[IO]]](Map.empty)

    for {
      client <- EmberClientBuilder.default[IO].build
      wsClient <- Resource.pure(JdkWSClient[IO](HttpClient.newHttpClient()))
      server <- Server.createServer[IO](wsb => Router("/api/v1" -> marimoRoutes(client, wsClient, wsb, connectionsRef)))
    } yield server
  }

  def run(args: List[String]): IO[ExitCode] = {

    program.use(_ => IO.never).as(ExitCode.Success)
  }
}