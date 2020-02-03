package ru.finochenko.short.url.handler

import cats.effect.Sync
import cats.implicits._
import org.http4s.{ParseFailure, ParseResult, Uri}
import ru.finochenko.short.url.dao.UrlsDao
import ru.finochenko.short.url.model.OriginalUrlIsNotExist
import ru.finochenko.short.url.model.Urls.{OriginalUrl, ShortUrl}

class UrlsHandler[F[_]: Sync](shortUrlDao: UrlsDao[F]) {

  def findOrGenerateShortUrl(originalUrl: OriginalUrl): F[ParseResult[ShortUrl]] = {
    Uri.fromString(originalUrl.value).fold(
      failure => failure.asLeft[ShortUrl].pure[F],
      _ =>
        for {
          maybeShortUrl <- shortUrlDao.queryShortUrl(originalUrl)
          shortUrl <- maybeShortUrl.map(_.pure[F]).getOrElse(generateAndSaveShortUrl(originalUrl))
        } yield shortUrl.asRight[ParseFailure]
    )
  }

  private def generateAndSaveShortUrl(originalUrl: OriginalUrl): F[ShortUrl] = {
    for {
      shortUrl <- generateAndCheckShortUrl
      _        <- shortUrlDao.insertUrls(shortUrl, originalUrl)
    } yield shortUrl
  }

  private def generateAndCheckShortUrl: F[ShortUrl] = {
    for {
      shortUrl         <- ShortUrl.generate[F]
      maybeOriginalUrl <- shortUrlDao.queryOriginalUrl(shortUrl)
      uniqueShortUrl   <- maybeOriginalUrl.fold(shortUrl.pure[F])(_ => generateAndCheckShortUrl)
    } yield uniqueShortUrl
  }

  def findOriginalUrl(shortUrl: ShortUrl): F[Either[OriginalUrlIsNotExist.type, OriginalUrl]] = {
    shortUrlDao.queryOriginalUrl(shortUrl).map {
      case Some(originalUrl) => originalUrl.asRight[OriginalUrlIsNotExist.type]
      case None => OriginalUrlIsNotExist.asLeft[OriginalUrl]
    }
  }

}

object UrlsHandler {

  def apply[F[_]: Sync](shortUrlDao: UrlsDao[F]): UrlsHandler[F] = new UrlsHandler[F](shortUrlDao)

}
