package inc.zhugastrov.marimo.config

import com.google.inject.Provider
import com.typesafe.config.{Config, ConfigFactory}

class ConfigProvider extends Provider[Config] {

  override def get(): Config = ConfigFactory.load()

}
