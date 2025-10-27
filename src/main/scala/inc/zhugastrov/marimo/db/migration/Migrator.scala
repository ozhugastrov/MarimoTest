package inc.zhugastrov.marimo.db.migration

import com.typesafe.config.Config
import jakarta.inject.Inject
import org.flywaydb.core.Flyway
import org.postgresql.ds.PGSimpleDataSource

class Migrator @Inject()(config: Config) {

  val dataSource = new PGSimpleDataSource()
  dataSource.setServerNames(Array(config.getString("db.serverName")))
  dataSource.setUser(config.getString("db.user"))
  dataSource.setPassword(config.getString("db.password"))
  dataSource.setDatabaseName(config.getString("db.databaseName"))
  dataSource.setPortNumbers(Array(config.getInt("db.portNumber")))


  val flyway: Flyway =  Flyway.configure().dataSource(dataSource).locations("classpath:migrations").load()

  def migrate(): Unit = {
    flyway.migrate()
  }

}
