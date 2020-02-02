package ru.finochenko.short.url

import cats.effect.{ContextShift, Sync}
import cats.implicits._
import org.http4s.HttpRoutes
import ru.finochenko.short.url.handler.UrlsHandler
import ru.finochenko.short.url.model.{OriginalUrl, ResponseShortUrl}
import sttp.tapir.server.http4s._

class Routes[F[_]: Sync: ContextShift](urlsHandler: UrlsHandler[F]) {

  def services: HttpRoutes[F] = findOrGenerateShortUrl <+> redirect

  def redirect: HttpRoutes[F] = Endpoints.redirect.toRoutes(shortUrl =>
    urlsHandler.findOriginalUrl(shortUrl).map { _.bimap(
      _ => "Original url is not exist",
      originalUrl => List(("Location", originalUrl.value)))
    }
  )

  def findOrGenerateShortUrl: HttpRoutes[F] = {
    Endpoints.getShortUrl.toRoutes { request =>
      val originalUrl = OriginalUrl(request.originalUrl)
      urlsHandler.findOrGenerateShortUrl(originalUrl).map(_.map(x => ResponseShortUrl(x.value)))
    }
  }

}

object Routes {

  def apply[F[_]: Sync: ContextShift](urlsHandler: UrlsHandler[F]): Routes[F] = new Routes[F](urlsHandler)

}
