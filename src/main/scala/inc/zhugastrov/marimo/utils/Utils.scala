package inc.zhugastrov.marimo.utils

import cats.effect.IO
import inc.zhugastrov.marimo.k8s.utils.{OperationFailed, OperationFailedNotFound, OperationResult, OperationSuccessful}
import org.http4s.*
import org.http4s.dsl.Http4sDsl

object Utils {

  def serviceName(name: String) = s"marimo-service-$name"

  def opResultToResponse(res: OperationResult): IO[Response[IO]] = {
    import dsl.*
    val dsl = Http4sDsl[IO]
    res match {
      case OperationFailed(message, ex) => IO(ex.foreach(_.printStackTrace())) >> InternalServerError(s"$message. ${ex.map(e => "Reason: " + e.getMessage).getOrElse("")}")
      case OperationFailedNotFound(s) => NotFound(s)
      case OperationSuccessful => Ok("Operation successful")
    }
  }

}
