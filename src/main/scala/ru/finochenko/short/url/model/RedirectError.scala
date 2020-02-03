package ru.finochenko.short.url.model

sealed trait RedirectError

/**
 * FIXME !!! Идея кажется не реализованной до конца. Методы, использующие данную ошибку не используются.
 * FIXME !!! Вместо этого логика завязана на `ParseResult`
 */
final case class ShortUrlIsInvalid(regex: String) extends RedirectError

case object OriginalUrlIsNotExist extends RedirectError