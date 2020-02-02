package ru.finochenko.short.url.model

import monix.eval.Coeval
import org.scalatest.{FlatSpec, Matchers}

class ShortUrlSpec extends FlatSpec with Matchers {

  "ShortUrl.generate" should "return short url mathc regex" in {
    val shortUrl = ShortUrl.generate[Coeval]
    val actualShortUrl = shortUrl.value().value
    actualShortUrl should fullyMatch regex ShortUrl.regularExpression
  }

}
