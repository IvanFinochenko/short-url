package ru.finochenko.short.url.model

/**
 * Для достижения такого же эффекта можно было бы использовать либу: https://github.com/estatico/scala-newtype
 */
case class OriginalUrl(value: String) extends AnyVal
