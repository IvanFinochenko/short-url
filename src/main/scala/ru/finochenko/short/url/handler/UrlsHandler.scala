package ru.finochenko.short.url.handler

import cats.data.EitherT
import cats.effect.Sync
import cats.implicits._
import org.http4s.{ParseFailure, ParseResult, Uri}
import ru.finochenko.short.url.dao.UrlsDao
import ru.finochenko.short.url.model.{OriginalUrl, OriginalUrlIsNotExist, RedirectError, ShortUrl, ShortUrlIsInvalid}

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

  /**
   * TODO Если случится так, что генератор выдаст сокращенный URL, уже существующий в БД, что будет?
   */
  private def generateAndSaveShortUrl(originalUrl: OriginalUrl): F[ShortUrl] = {
    for {
      shortUrl <- ShortUrl.generate[F]
      _ <- shortUrlDao.insertUrls(shortUrl, originalUrl)
    } yield shortUrl
  }

  /**
   * FIXME Unused!!!
   */
  def validateAndFindOriginalUrl(shortUrl: ShortUrl): F[Either[RedirectError, OriginalUrl]] = {
    val errorOrOriginalUrl = for {
      validShortUrl <- EitherT(validateShortUrl(shortUrl).pure[F])
      originalUrl <- EitherT.fromOptionF[F, RedirectError, OriginalUrl](shortUrlDao.queryOriginalUrl(validShortUrl), OriginalUrlIsNotExist)
    } yield originalUrl
    errorOrOriginalUrl.value
  }

  def findOriginalUrl(shortUrl: ShortUrl): F[Either[OriginalUrlIsNotExist.type, OriginalUrl]] = {
    shortUrlDao.queryOriginalUrl(shortUrl).map {
      case Some(originalUrl) => originalUrl.asRight[OriginalUrlIsNotExist.type]
      case None => OriginalUrlIsNotExist.asLeft[OriginalUrl]
    }
  }

  /**
   * FIXME Unused!!!
   */
  private def validateShortUrl(shortUrl: ShortUrl): Either[RedirectError, ShortUrl] = {
    shortUrl.some
        .filter(url => ShortUrl.regularExpression.r.pattern.matcher(url.value).matches())
        .fold(
          Either.left[RedirectError, ShortUrl](ShortUrlIsInvalid(ShortUrl.regularExpression)))(
          _.asRight[RedirectError]
        )
  }

}

object UrlsHandler {

  def apply[F[_]: Sync](shortUrlDao: UrlsDao[F]): UrlsHandler[F] = new UrlsHandler[F](shortUrlDao)

}
