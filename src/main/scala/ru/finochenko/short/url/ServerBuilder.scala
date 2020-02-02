package ru.finochenko.short.url

import cats.{Applicative, Monad}
import cats.data.Kleisli
import cats.effect.{Blocker, ConcurrentEffect, ContextShift, ExitCode, Resource, Timer}
import cats.implicits._
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import io.chrisdavenport.log4cats.Logger
import org.http4s.implicits._
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeServerBuilder
import ru.finochenko.short.url.dao.UrlsDaoImpl
import ru.finochenko.short.url.handler.UrlsHandler
import ru.finochenko.short.url.model.Config
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.swagger.http4s.SwaggerHttp4s

class ServerBuilder[F[_]: ConcurrentEffect: Timer: Monad: ContextShift: Logger] {

  def build(): F[ExitCode] = {

    Config.load[F].flatMap { errorsOrConfig =>
      errorsOrConfig.fold(
        { errors =>
          Logger[F].error(errors.toList.mkString(", ")) *> ConcurrentEffect[F].pure(ExitCode.Error)
        },
        { config =>
          buildTransactorAndServer
              .run(config)
              .use(_ =>
                Logger[F].info(s"Documentation is available on http://${config.server.host}:${config.server.port}/docs") *>
                    ConcurrentEffect[F].never[Unit] *>
                    ExitCode.Success.pure[F])
        })
    }
  }

  private def buildTransactorAndServer: Kleisli[Resource[F, *], Config, Server[F]] = {
    for {
      _          <- migrate()
      transactor <- buildTransactor
      server     <- buildServer(transactor)
    } yield server
  }

  private def migrate(): Kleisli[Resource[F, *], Config, Unit] = {
    Kleisli { config =>
      val migrateAndLog = for {
        migration <- Migration[F]
        successMigrations <- migration.migrate(config.database)
        _ <- Logger[F].info(s"Successful migrations = $successMigrations")
      } yield () -> Applicative[F].unit
      Resource(migrateAndLog)
    }
  }

  private def buildServer(transactor: Transactor[F]): Kleisli[Resource[F, *], Config, Server[F]] = {
    Kleisli { config =>
      val docs = List(Endpoints.getShortUrl, Endpoints.redirect).toOpenAPI("Docs short-url", "1.0.0")
      val swaggerDocs = new SwaggerHttp4s(docs.toYaml).routes
      val shortUrlDao = UrlsDaoImpl[F](transactor)
      val logic = UrlsHandler[F](shortUrlDao)
      val services = Routes[F](logic).services
      val allRoutes = (swaggerDocs <+> services).orNotFound
      BlazeServerBuilder[F]
          .bindHttp(config.server.port, config.server.host)
          .withHttpApp(allRoutes)
          .resource
    }
  }

  private def buildTransactor: Kleisli[Resource[F, *], Config, Transactor[F]] = {
    Kleisli { config =>
      for {
        blocker <- Blocker[F]
        executionContext <- ExecutionContexts.fixedThreadPool[F](10)
        transactor <- HikariTransactor.newHikariTransactor[F](
          config.database.driver,
          config.database.url,
          config.database.user,
          config.database.password,
          executionContext,
          blocker
        )
      } yield transactor
    }
  }
}

object ServerBuilder {

  def apply[F[_]: ConcurrentEffect: Timer: Monad: ContextShift: Logger]: ServerBuilder[F] = new ServerBuilder[F]()
}
