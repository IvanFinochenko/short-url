package ru.finochenko.short.url.model

import cats.effect.Sync
import io.estatico.newtype.macros.newtype
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.{Codec, Validator}

import scala.util.Random

object Urls {

  @newtype case class OriginalUrl(value: String)

  @newtype case class ShortUrl(value: String)

  object ShortUrl {

    val regularExpression: String = "[A-Za-z0-9_]{10}"

    private val shortUrlLength = 10
    private val shortUrlChars = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') :+ '_'

    implicit val shortUrlCodec: Codec[ShortUrl, TextPlain, String] = Codec.stringPlainCodecUtf8
        .map(s => ShortUrl(s))(myId => myId.value)
        .validate(Validator.pattern(regularExpression).contramap(_.value))

    def generate[F[_] : Sync]: F[ShortUrl] = {
      Sync[F].delay {
        val random = (1 to shortUrlLength).foldLeft("") { case (acc, _) =>
          val randomIndex = Random.nextInt(shortUrlChars.length)
          acc + shortUrlChars(randomIndex)
        }
        ShortUrl(random)
      }
    }

  }

}
