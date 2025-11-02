package inc.zhugastrov.marimo

import cats.effect.*
import com.google.inject.{Guice, Injector}
import inc.zhugastrov.marimo.db.NotebookRepository
import inc.zhugastrov.marimo.db.migration.Migrator
import inc.zhugastrov.marimo.db.provider.TransactorProvider
import inc.zhugastrov.marimo.k8s.KubernetesService
import inc.zhugastrov.marimo.routes.MarimoRoute.marimoRoutes
import inc.zhugastrov.marimo.server.Server
import org.http4s.client.websocket.WSConnection
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.jdkhttpclient.JdkWSClient
import org.http4s.server.{Router, Server}

import java.net.http.HttpClient

object Main extends IOApp {
  private val injector: Injector = Guice.createInjector(List(new Module, new TransactorProvider) *)

  private val migrator = injector.getInstance(classOf[Migrator])

  private val k8sService = Main.injector.getInstance(classOf[KubernetesService])
  private val notebookRepo = Main.injector.getInstance(classOf[NotebookRepository])

  migrator.migrate()


  private def program: Resource[IO, Server] = {
    val connectionsRef: Ref[IO, Map[(String, String), WSConnection[IO]]] = Ref.unsafe[IO, Map[(String, String), WSConnection[IO]]](Map.empty)

    for {
      k8s <- Resource.pure(k8sService)
      repo <- Resource.pure(notebookRepo)
      client <- EmberClientBuilder.default[IO].build
      wsClient <- Resource.pure(JdkWSClient[IO](HttpClient.newHttpClient()))
      server <- Server.createServer[IO](wsb => Router("/api/v1" -> marimoRoutes(client, wsClient, wsb, connectionsRef, k8s, repo)))
    } yield server
  }

  def run(args: List[String]): IO[ExitCode] = {

    program.use(_ => IO.never).as(ExitCode.Success)
  }
}