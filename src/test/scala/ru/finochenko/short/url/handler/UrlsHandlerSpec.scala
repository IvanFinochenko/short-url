package ru.finochenko.short.url.handler

import cats.effect.concurrent.Ref
import cats.implicits._
import monix.eval.Coeval
import org.http4s.ParseFailure
import org.scalatest.{FlatSpec, Matchers}
import ru.finochenko.short.url.dao.UrlsDao
import ru.finochenko.short.url.model.{OriginalUrl, OriginalUrlIsNotExist, ShortUrl}

class UrlsHandlerSpec extends FlatSpec with Matchers {

  "findOriginalUrl" should "return Left(OriginalUrlIsNotExist) if can not find original url" in {
    val dao = new UrlsDaoTest(None, None)
    val handler = UrlsHandler[Coeval](dao)
    val actualResult = for {
      shortUrl <- ShortUrl.generate[Coeval]
      errorOrOriginalUrl <- handler.findOriginalUrl(shortUrl)
    } yield errorOrOriginalUrl
    val expectedError = OriginalUrlIsNotExist.asLeft[OriginalUrl]
    actualResult.value() should be(expectedError)
  }

  it should "return original url" in {
    val originalUrl = OriginalUrl("some-url")
    val dao = new UrlsDaoTest(None, originalUrl.some)
    val handler = UrlsHandler[Coeval](dao)
    val actualResult = for {
      shortUrl <- ShortUrl.generate[Coeval]
      errorOrOriginalUrl <- handler.findOriginalUrl(shortUrl)
    } yield errorOrOriginalUrl
    val expectedOriginalUrl = originalUrl.asRight[OriginalUrlIsNotExist.type]
    actualResult.value() should be(expectedOriginalUrl)
  }

  "findOrGenerateShortUrl" should "return Left(ParseFailure) if pass wrong original url" in {
    val originalUrl = OriginalUrl("some wrong url")
    val dao = new UrlsDaoTest(None, None)
    val handler = UrlsHandler[Coeval](dao)
    val actualResult = handler.findOrGenerateShortUrl(originalUrl)
    actualResult.value().isLeft should be(true)
  }

  it should "return old short url" in {
    val originalUrl = OriginalUrl("http://some-url.org")
    val resultAndShortUrl = for {
      shortUrl <- ShortUrl.generate[Coeval]
      dao = new UrlsDaoTest(shortUrl.some, None)
      handler = UrlsHandler[Coeval](dao)
      errorOrShortUrl <- handler.findOrGenerateShortUrl(originalUrl)
    } yield errorOrShortUrl -> shortUrl
    val (actualResult, shortUrl) = resultAndShortUrl.value()
    actualResult should be(shortUrl.asRight[ParseFailure])
  }

  it should "return new generated short url if short url in dao was not found" in {
    val originalUrl = OriginalUrl("http://some-url.org")
    val resultAndSavedUrls = for {
      savedUrls <- Ref.of[Coeval, Option[(ShortUrl, OriginalUrl)]](None)
      dao = new UrlsDaoTest(None, None, savedUrls.some)
      handler = UrlsHandler[Coeval](dao)
      result <- handler.findOrGenerateShortUrl(originalUrl)
    } yield result -> savedUrls
    val (generatedShortUrl, savedUrls) = resultAndSavedUrls.value()
    val urlsInStore = savedUrls.get.value()
    val originalUrlInStore = urlsInStore.map(_._2)
    val shortUrlInStore = urlsInStore.map(_._1)
    originalUrlInStore should be(originalUrl.some)
    generatedShortUrl.toOption should be (shortUrlInStore)
  }

  private class UrlsDaoTest(
      val shortUrl: Option[ShortUrl],
      val originalUrl: Option[OriginalUrl],
      val savedUrls: Option[Ref[Coeval, Option[(ShortUrl, OriginalUrl)]]] = None
  ) extends UrlsDao[Coeval] {

    override def queryShortUrl(originalUrl: OriginalUrl): Coeval[Option[ShortUrl]] = Coeval(shortUrl)

    override def queryOriginalUrl(shortUrl: ShortUrl): Coeval[Option[OriginalUrl]] = Coeval(originalUrl)

    override def insertUrls(shortUrl: ShortUrl, originalUrl: OriginalUrl): Coeval[Int] = {
      savedUrls
          .map(_.modify(_ => (shortUrl, originalUrl).some -> 1))
          .getOrElse(Coeval(0))
    }

  }

}
