package inc.zhugastrov.marimo.db.migration

import com.typesafe.config.Config
import jakarta.inject.Inject
import org.flywaydb.core.Flyway

class Migrator @Inject()(config: Config) {

  
  private val url = config.getString("db.url")
  private val user = config.getString("db.user")
  private val password = config.getString("db.password")


  val flyway: Flyway =  Flyway.configure().dataSource(url, user, password).locations("classpath:migrations").load()

  def migrate(): Unit = {
    flyway.migrate()
  }

}
