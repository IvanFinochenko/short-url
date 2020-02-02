package ru.finochenko.short.url

import io.circe.generic.auto._
import org.http4s.ParseFailure
import ru.finochenko.short.url.model.ShortUrl.shorUrlCodec
import ru.finochenko.short.url.model.{RequestOriginalUrl, ResponseShortUrl, ShortUrl}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._

object Endpoints {

  val redirect: Endpoint[ShortUrl, String, Seq[(String, String)], Nothing] = endpoint
      .get
      .description("Redirect to the original url by the short url")
      .in(path[ShortUrl]("shortUrl")(shorUrlCodec).description(s"Short url should match ${ShortUrl.regularExpression}"))
      .errorOut(stringBody)
      .out(statusCode(StatusCode.TemporaryRedirect))
      .out(headers)

  val getShortUrl: Endpoint[RequestOriginalUrl, ParseFailure, ResponseShortUrl, Nothing] = endpoint
      .post
      .description("Get a short url or generate new short url if it doesn't exist")
      .in(jsonBody[RequestOriginalUrl])
      .errorOut(jsonBody[ParseFailure])
      .out(jsonBody[ResponseShortUrl])

}
