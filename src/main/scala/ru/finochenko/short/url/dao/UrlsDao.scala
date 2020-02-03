package ru.finochenko.short.url.dao

import cats.effect.Bracket
import doobie.implicits._
import doobie.util.Read
import doobie.util.transactor.Transactor
import ru.finochenko.short.url.model.Urls.{OriginalUrl, ShortUrl}

trait UrlsDao[F[_]] {

  def queryShortUrl(originalUrl: OriginalUrl): F[Option[ShortUrl]]

  def queryOriginalUrl(shortUrl: ShortUrl): F[Option[OriginalUrl]]

  def insertUrls(shortUrl: ShortUrl, originalUrl: OriginalUrl): F[Int]

}

class UrlsDaoImpl[F[_]](transactor: Transactor[F])(implicit bracket:Bracket[F, Throwable]) extends UrlsDao[F] {
  implicit val originalUrlRead: Read[OriginalUrl] = Read[String].map(OriginalUrl(_))
  implicit val shortUrlRead: Read[ShortUrl] = Read[String].map(ShortUrl(_))
  override def queryShortUrl(originalUrl: OriginalUrl): F[Option[ShortUrl]] = {
    sql"SELECT short_url FROM short_original_urls WHERE original_url = ${originalUrl.value}"
        .query[ShortUrl]
        .option
        .transact(transactor)
  }

  override def queryOriginalUrl(shortUrl: ShortUrl): F[Option[OriginalUrl]] = {
    sql"SELECT original_url FROM short_original_urls WHERE short_url = ${shortUrl.value}"
        .query[OriginalUrl]
        .option
        .transact(transactor)
  }

  override def insertUrls(shortUrl: ShortUrl, originalUrl: OriginalUrl): F[Int] = {
    sql"INSERT INTO short_original_urls(short_url, original_url) VALUES(${shortUrl.value}, ${originalUrl.value})"
        .update
        .run
        .transact(transactor)
  }

}

object UrlsDaoImpl {

  def apply[F[_]](transactor: Transactor[F])(implicit bracket:Bracket[F, Throwable]): UrlsDao[F] = {
    new UrlsDaoImpl[F](transactor)
  }

}
