package inc.zhugastrov.marimo.k8s

import cats.effect.IO
import inc.zhugastrov.marimo.k8s.utils.OperationResult

trait KubernetesService {

  def createDeployment(name: String): IO[OperationResult]
  
  def restartDeployment(name: String): IO[OperationResult]

  def deleteDeployment(name:String): IO[OperationResult]


}
