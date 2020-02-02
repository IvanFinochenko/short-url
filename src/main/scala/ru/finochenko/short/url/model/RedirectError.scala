package ru.finochenko.short.url.model

sealed trait RedirectError

final case class ShortUrlIsInvalid(regex: String) extends RedirectError

case object OriginalUrlIsNotExist extends RedirectError