package ru.finochenko.short.url.model

import cats.implicits._
import cats.effect.Sync
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.{Codec, Validator}

import scala.util.Random

case class ShortUrl(value: String) extends AnyVal

object ShortUrl {

  val regularExpression: String = "[A-Za-z0-9_]{10}"

  implicit val shorUrlCodec: Codec[ShortUrl, TextPlain, String] = Codec.stringPlainCodecUtf8
      .map(s => new ShortUrl(s))(myId => myId.value)
      .validate(Validator.pattern(regularExpression).contramap(_.value))

  def generate[F[_]: Sync]: F[ShortUrl] = {
    val chars = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') :+ '_'
    generateBySymbol(chars.toVector, 10, "").map(ShortUrl(_))
  }

  private def generateBySymbol[F[_]: Sync](chars: Vector[Char], n: Int, shortUrl: String): F[String] = {
    if (n <= 0) {
      shortUrl.pure[F]
    } else {
      Sync[F].delay {
        val randomIndex = Random.nextInt(chars.length)
        shortUrl + chars(randomIndex)
      }.flatMap(s => generateBySymbol(chars, n - 1, s))
    }
  }
}