package io.getquill.example.postgres

import io.getquill.example.common.entity.Person
import io.getquill.example.common.{Decoders, Encoders, Quotes}
import io.getquill.{PostgresAsyncContext, SnakeCase}
import model.persistence.{AsyncCtxLike, TableNameSnakeCase}
import org.joda.time.{DateTimeZone, LocalDateTime}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait PostgresAsyncCtx extends AsyncCtxLike {
  lazy val ctx = new PostgresAsyncContext[TableNameSnakeCase]("quill-cache.postgres-async") with Encoders with Decoders with Quotes
}

object PostgresAsyncCtx extends PostgresAsyncCtx

package object async extends PostgresAsyncCtx {
  import ctx._

  def create(person: Person): Future[Long] =
    ctx.run(query[Person].insert(lift(person)))

  def read(id: Int): Future[Seq[Person]] =
    ctx.run(query[Person].filter(_.id == lift(id)))

  def readNamesOfPersonsOlderThan(age: Int): Future[Seq[String]] = {
    val longAgo: LocalDateTime = LocalDateTime.now(DateTimeZone.UTC).minusYears(age)
    ctx.run {
      quote {
        query[Person]
//          .filter(t => t.birthDate.isAfter(lift(longAgo)) && t.deathDate.isEmpty)
            /* Error:(29, 13) Tree 't.birthDate.isAfter((`package`.this.ctx.liftScalar[org.joda.time.LocalDateTime](longAgo)(scala.Predef.implicitly[io.getquill.example.postgres.async.package.ctx.Encoder[org.joda.time.LocalDateTime]](`package`.this.ctx.mappedEncoder[org.joda.time.LocalDateTime, java.util.Date](`package`.this.ctx.jodaLocalDateTimeEncoder, `package`.this.ctx.dateEncoder))): org.joda.time.LocalDateTime))' can't be parsed to 'Ast' */
          .map(_.name)
      }
    }
  }

  def update(person: Person): Future[Long] =
    ctx.run(query[Person].update(lift(person)))

  def update(id: Int, name: String): Future[Long] =
    ctx.run(query[Person].filter(_.id == lift(id)).update(_.name -> lift(name)))

  def delete(id: Int): Future[Long] =
    ctx.run(query[Person].filter(_.id == lift(id)).delete)

  def runExample(): Future[Unit] = {
    model.persistence.ProcessEvolutionUp(PostgresAsyncCtx, "evolutions/default/1.sql")
    create(Person(1, "David", new LocalDateTime(1947, 1, 8, 0, 0), None)).flatMap { _ =>
      read(1)
    }.flatMap { _ =>
      readNamesOfPersonsOlderThan(30)
    }.flatMap { _ =>
      update(Person(1, "David", new LocalDateTime(1947, 1, 8, 0, 0), Some(new LocalDateTime(2016, 1, 10, 0, 0))))
    }.flatMap { _ =>
      update(1, "David Bowie")
    }.flatMap { _ =>
      delete(1)
    }.map(_ => ())
  }
}
