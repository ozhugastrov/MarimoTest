package inc.zhugastrov.marimo.k8s

import cats.effect.IO

trait KubernetesService {

  def createDeployment(name: String): IO[Unit]

  def deleteDeployment(name:String): IO[String]


}
