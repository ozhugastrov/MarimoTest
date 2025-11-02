package inc.zhugastrov.marimo.db.provider

import cats.effect.IO
import com.google.inject.{AbstractModule, Provides, Singleton}
import com.typesafe.config.Config
import doobie.Transactor

class TransactorProvider extends AbstractModule {

  @Provides
  @Singleton
  def xa(config: Config): Transactor[IO] = Transactor.fromDriverManager[IO](
    driver = "org.postgresql.Driver",
    url = config.getString("db.url"),
    user = config.getString("db.user"),
    password = config.getString("db.password"),
    logHandler = None
  )

}
