package ru.finochenko.short.url

import cats.effect.Sync
import org.flywaydb.core.Flyway
import ru.finochenko.short.url.model.DataBaseConfig

class Migration[F[_]: Sync] {

  def migrate(dbConfig: DataBaseConfig): F[Int] = {
    Sync[F].delay {
      val flywayMigration = Flyway.configure()
          .dataSource(dbConfig.url, dbConfig.user, dbConfig.password)
          .load()
      flywayMigration.migrate()
    }
  }

}

object Migration {

  def apply[F[_]: Sync]: F[Migration[F]] = Sync[F].delay(new Migration[F])

}
