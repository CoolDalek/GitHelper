package service.impl

import cats.Traverse
import cats.effect.kernel.Ref.Make
import cats.effect.{MonadCancelThrow, Ref}
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import service.MigrationService

class MigrationServiceImpl[F[_]: MonadCancelThrow](
                                                    xa: Transactor[F],
                                                    dbVersion: Ref[F, Int],
                                                  ) extends MigrationService[F]:

  private def migration(sql: Fragment): F[Unit] =
    sql.update.run.transact(xa).void

  private val tokenTable =
    sql"""create table api_tokens(
         body text primary key
       )"""

  private val migrations = Vector(
    tokenTable,
  )

  private val drops = Vector(
    sql"drop table if exists schema_history",
    sql"drop table if exists api_tokens",
  )

  private val actualVersion: Int = migrations.length

  override def needMigration: F[Boolean] =
    val create = sql"""create table if not exists schema_version(
         version int primary key
       )""".update.run

    val get = sql"select * from schema_version"
      .query[Int]
      .option

    val version = create >> get

    for {
      maybeVer <- version.transact(xa)
      ver = maybeVer.getOrElse(0)
      _ <- dbVersion.set(ver)
    } yield ver != actualVersion
  end needMigration

  private def updateVersion(old: Int, fresh: Int): F[Unit] =
    val delete = sql"""delete from schema_version where version = $old"""
      .update.run
    val insert = sql"""insert into schema_version values($fresh)"""
      .update.run
    val update = delete >> insert
    update.transact(xa).void
  end updateVersion

  override def migrate: F[Unit] =
    for {
      version <- dbVersion.get
      needApply = migrations.drop(version)
      queries = needApply.map(migration)
      _ <- Traverse[Vector].sequence(queries)
      _ <- updateVersion(version, actualVersion)
    } yield ()
  end migrate

  def dropDb: F[Unit] =
    val script = drops.map(migration)
    Traverse[Vector].sequence(script).void
  end dropDb

object MigrationServiceImpl:

  def make[F[+_]: MonadCancelThrow: Make](xa: Transactor[F]): F[MigrationService[F]] =
    for {
      dbVersion <- Ref.of[F, Int](0)
    } yield new MigrationServiceImpl[F](xa, dbVersion)

end MigrationServiceImpl
