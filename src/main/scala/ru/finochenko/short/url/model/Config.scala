package ru.finochenko.short.url.model

import cats.effect.Sync
import pureconfig.ConfigReader.Result
import pureconfig.ConfigSource
import pureconfig.generic.auto._

final case class DataBaseConfig(driver: String, url: String, user: String, password: String)

final case class ServerConfig(host: String, port: Int)

final case class Config(server: ServerConfig, database: DataBaseConfig)

object Config {

  def load[F[_]: Sync]: F[Result[Config]] = {
    Sync[F].delay(ConfigSource.default.load[Config])
  }

}