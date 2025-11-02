package inc.zhugastrov.marimo.db.impl

import cats.effect.IO
import com.google.inject.Inject
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.{Meta, Transactor}
import inc.zhugastrov.marimo.db.NotebookRepository

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class NotebookRepositoryImpl @Inject()(xa: Transactor[IO]) extends NotebookRepository {

  implicit val instantMeta: Meta[Instant] =
    Meta[Timestamp].imap(_.toInstant)(Timestamp.from)

  override def store(name: String,
                     createdAt: Instant,
                     lastEditTime: Instant,
                     status: String): IO[UUID] = {
    sql"""
          INSERT INTO notebook (name, created_at, last_edit_time, status)
          VALUES ($name, $createdAt, $lastEditTime, $status)
          RETURNING id
        """.query[UUID].unique.transact(xa).handleErrorWith { e =>
      IO.println(s"Failed to insert notebook '$name': ${e.getMessage}") *>
        IO.raiseError(new RuntimeException(s"Failed to insert notebook '$name': ${e.getMessage}", e))
    }
  }
}
