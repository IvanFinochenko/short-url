package ru.finochenko.short.url.model

/**
 * Прослеживается некая непоследовательность в объявлении `case class`-ов: где-то с `final`, где-то без.
 * Имеет смысл придерживаться единго стиля, и объявлять их как `final`.
 */
case class RequestOriginalUrl(originalUrl: String)
