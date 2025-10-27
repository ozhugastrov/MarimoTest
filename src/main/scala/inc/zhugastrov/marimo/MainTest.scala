package inc.zhugastrov.marimo

import org.apache.pekko.actor.ActorSystem
import skuber.Container.PullPolicy
import skuber.api.Configuration
import skuber.apps.v1.Deployment
import skuber.json.format.*
import skuber.networking.Ingress
import skuber.{Container, LabelSelector, Pod, PodList, Service, k8sInit}

import scala.concurrent.{ExecutionContext, Future}


object MainTest extends App {

  given system: ActorSystem = ActorSystem()

  given dispatcher: ExecutionContext = system.dispatcher

  val k8s = k8sInit
  val listPodsRequest = k8s.list[PodList](Some("kube-system"))
  val ksysPods: Future[PodList] = k8s.list[PodList](Some("kube-system"))
  val printKSysPods = ksysPods.map { podList => podList.items.foreach(p => println(p.name)) }
  printKSysPods.failed.foreach { ex => System.err.println("Failed => " + ex) }

  val httpPort = 80
  val httpsPort = 443

  val nodeIngressHttpPort = 30080
  val nodeIngressHttpsPort = 30443


  val marimoSelector = "app" -> "marimo"

  val marimoContainer = Container(name = "marimo",
    image = "marimo-local:latest", ports = List(Container.Port(8080)))
    .exposePort(8080)
    .withImagePullPolicy(Option(PullPolicy.IfNotPresent))

  val marimoTemplate = Pod.Template.Spec
    .named("marimo")
    .addContainer(marimoContainer)
    .addLabel("app" -> "marimo")

  import LabelSelector.dsl.*

  import scala.reflect.Selectable.reflectiveSelectable

  val appReq = "app" `is` "marimo"
  val nginxSelector = LabelSelector(appReq)

  val marimoDeployment = Deployment("marimo")
    .withTemplate(marimoTemplate)
    .withLabelSelector(nginxSelector)

  val marimoService = Service("marimo-svc")
    .withSelector("app" -> "marimo")
    .exposeOnPort(Service.Port(port = 80, targetPort = Some(Left(8080))))

//  def marimoIngress: Ingress = {
//    Ingress("marimoing")
//      .addHttpRule("foo.bar.com", Map("/foo" -> "marimo:80"))
//  }

  val createOnK8s = for {
    ser <- k8s.create(marimoDeployment)
    pod <- k8s.create(marimoService)
//    ing <- k8s.create(marimoIngress)
  } yield (ser, pod)


}
