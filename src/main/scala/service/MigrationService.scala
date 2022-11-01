package service

trait MigrationService[F[_]]:

  def dropDb: F[Unit]

  def needMigration: F[Boolean]

  def migrate: F[Unit]

end MigrationService
