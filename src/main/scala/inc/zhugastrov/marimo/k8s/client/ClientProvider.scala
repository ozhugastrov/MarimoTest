package inc.zhugastrov.marimo.k8s.client

import com.google.inject.Provider
import io.fabric8.kubernetes.client.{KubernetesClient, KubernetesClientBuilder}

class ClientProvider extends Provider[KubernetesClient] {
  override def get(): KubernetesClient = new KubernetesClientBuilder().build()
}