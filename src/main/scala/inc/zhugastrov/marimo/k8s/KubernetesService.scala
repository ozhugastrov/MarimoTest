package inc.zhugastrov.marimo.k8s

import cats.effect.IO
import inc.zhugastrov.marimo.k8s.utils.OperationResult

import scala.util.Try

trait KubernetesService {

  def createDeployment(name: String): IO[OperationResult]
  
  def restartDeployment(name: String): IO[OperationResult]

  def deleteDeployment(name:String): IO[OperationResult]

  def findServiceAddress(user: String): IO[Try[Option[String]]]

}
