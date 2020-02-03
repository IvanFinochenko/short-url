package ru.finochenko.short.url.model

import ru.finochenko.short.url.model.Urls.ShortUrl

final case class ResponseShortUrl(shortUrl: String)

object ResponseShortUrl {

  def apply(server: ServerConfig, shortUrl: ShortUrl): ResponseShortUrl = {
    val url = s"http://${server.host}:${server.port}/${shortUrl.value}"
    ResponseShortUrl(url)
  }

}
