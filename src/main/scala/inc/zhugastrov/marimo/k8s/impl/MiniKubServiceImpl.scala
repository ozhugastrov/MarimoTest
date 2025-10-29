package inc.zhugastrov.marimo.k8s.impl

import cats.effect.IO
import com.google.inject.Inject
import inc.zhugastrov.marimo.k8s.KubernetesService
import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.apps.{DeploymentBuilder, DeploymentSpecBuilder}
import io.fabric8.kubernetes.client.KubernetesClient
import inc.zhugastrov.marimo.utils.Utils.*
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}


class MiniKubServiceImpl @Inject()(k8sClient: KubernetesClient) extends KubernetesService {

  private def createService(name: String) = IO.apply {

    val meta = new ObjectMetaBuilder().withName(serviceName(name)).build()

    val port8080 = new ServicePortBuilder().withName("8080").withPort(8080).withProtocol("TCP").build()
//    val port3119 = new ServicePortBuilder().withName("3119").withPort(3119).withProtocol("TCP").build()

    val spec = new ServiceSpecBuilder()
      .withSelector(Map("app" -> s"marimo-$name").asJava)
      .withPorts(port8080).build()

    val namespace = "default"

    val service = new ServiceBuilder()
      .withMetadata(meta)
      .withSpec(spec)
      .build()

    Try(k8sClient.services()
      .inNamespace(namespace)
      .resource(service)
      .create())
  }.flatMap {
    case Success(_) =>
      IO.println(s"Service $name created or updated") >>
        IO.unit
    case Failure(ex) =>
      IO.println(s"Failed to create service: ${ex.getMessage}") >>
        IO.raiseError(ex)
  }


  override def createDeployment(name: String): IO[Unit] = IO.apply {
    val meta = new ObjectMetaBuilder().withName(s"marimo-deployment-$name").build()
    val templateMeta = new ObjectMetaBuilder().addToLabels("app", s"marimo-$name").build()

    val container = new ContainerBuilder().withName(s"marimo-$name")
      .withImage("marimo-local:latest")
//      .withImage("ghcr.io/marimo-team/marimo:latest-sql")
      .withImagePullPolicy("IfNotPresent")
      .withPorts(new ContainerPortBuilder().withContainerPort(8080).build()).build()

    val podSpec = new PodSpecBuilder().withContainers(container).build()

    val template = new PodTemplateSpecBuilder()
      .withMetadata(templateMeta)
      .withSpec(podSpec)
      .build()

    val selector = new LabelSelectorBuilder()
      .addToMatchLabels("app", s"marimo-$name")
      .build()

    val spec = new DeploymentSpecBuilder()
      .withSelector(selector)
      .withTemplate(template)
      .build()

    val deployment = new DeploymentBuilder()
      .withMetadata(meta).withSpec(spec).build()
    println("creating deployment")
    Try(
      k8sClient.apps().deployments().inNamespace("default").resource(deployment).create()
    )
  }.flatMap {
    case Failure(exception) =>
      exception.printStackTrace()
      IO.raiseError(exception)
    case Success(value) => IO.unit
  } >> createService(name)


  private def deleteService(name: String): IO[String] = IO.apply {
    Try(k8sClient.services().withName(s"marimo-service-$name").delete())
  }.flatMap {
    case Success(deleted) =>
      if (deleted.isEmpty) {
        IO.println(s"Deployment $name not found") >>
          IO.pure("Not found. Nothing to delete")
      }
      else {
        IO.println(s"Deployment $name deleted") >>
          IO.pure(s"Deployment $name deleted")
      }
    case Failure(ex) =>
      IO.println(s"Failed to delete deployment: ${ex.getMessage}") >>
        IO.raiseError(ex)
  }

  override def deleteDeployment(name: String): IO[String] = IO.apply {
    Try(k8sClient.apps().deployments()
      .inNamespace("default")
      .withName(s"marimo-deployment-$name")
      .delete())
  }.flatMap {
    case Success(deleted) =>
      if (deleted.isEmpty) {
        IO.println(s"Deployment $name not found") >>
          IO.pure("Not found. Nothing to delete")
      }
      else {
        IO.println(s"Deployment $name deleted") >>
          IO.pure(s"Deployment $name deleted")
      }
    case Failure(ex) =>
      IO.println(s"Failed to delete deployment: ${ex.getMessage}") >>
        IO.raiseError(ex)
  } >> deleteService(name)


}
