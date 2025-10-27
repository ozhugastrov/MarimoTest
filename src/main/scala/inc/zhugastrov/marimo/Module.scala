package inc.zhugastrov.marimo

import com.google.inject.AbstractModule
import com.typesafe.config.Config
import inc.zhugastrov.marimo.config.ConfigProvider
import inc.zhugastrov.marimo.k8s.KubernetesService
import inc.zhugastrov.marimo.k8s.client.ClientProvider
import inc.zhugastrov.marimo.k8s.impl.MiniKubServiceImpl
import io.fabric8.kubernetes.client.KubernetesClient

class Module extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[KubernetesClient]).toProvider(classOf[ClientProvider]).asEagerSingleton()
    bind(classOf[Config]).toProvider(classOf[ConfigProvider]).asEagerSingleton()
    bind(classOf[KubernetesService]).to(classOf[MiniKubServiceImpl]).asEagerSingleton()
  }

}
