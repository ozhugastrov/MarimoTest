package inc.zhugastrov.marimo.db

import cats.effect.{Async, IO}

import java.time.Instant
import java.util.UUID

trait NotebookRepository {

  def store(name: String,
            createdAt: Instant,
            lastEditTime: Instant,
            status: String): IO[UUID]

}
