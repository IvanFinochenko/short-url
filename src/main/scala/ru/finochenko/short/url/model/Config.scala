package ru.finochenko.short.url.model

import cats.data.Kleisli
import cats.effect.Sync
import cats.implicits._
import pureconfig.ConfigSource
import pureconfig.generic.auto._

final case class DataBaseConfig(driver: String)

final case class DataBaseConnection(url: String, user: String, password: String)

final case class ServerConfig(host: String, port: Int)

final case class ServerAndDb(server: ServerConfig, database: DataBaseConfig)

final case class Config(server: ServerConfig, database: DataBaseConfig, dbConnection: DataBaseConnection)

object DataBaseConnection {

  type EnvironmentVariables = Map[String, String]
  type ErrorOrEnvVariable = Kleisli[Either[String, *], EnvironmentVariables, String]

  def load: Either[String, DataBaseConnection] = {
    val dataBaseConnection = for {
      url <- getEnvVariableOrError("DATABASE_URL")
      user <- getEnvVariableOrError("DATABASE_USER")
      password <- getEnvVariableOrError("DATABASE_PASSWORD")
    } yield DataBaseConnection(url, user, password)
    dataBaseConnection.run(sys.env)
  }

  private def getEnvVariableOrError(key: String): ErrorOrEnvVariable = {
    Kleisli(_.get(key).toRight(s"Environment variable=$key is not found"))
  }

}

object Config {

  def load[F[_] : Sync]: F[Either[String, Config]] = {
    Sync[F].delay {
      for {
        dataBaseConnection <- DataBaseConnection.load
        config <- ConfigSource.default.load[ServerAndDb].leftMap(_.toList.mkString(", "))
      } yield Config(config.server, config.database, dataBaseConnection)
    }
  }

}