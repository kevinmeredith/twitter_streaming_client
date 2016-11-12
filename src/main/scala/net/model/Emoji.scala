package net.model

import io.circe._
import io.circe.generic.semiauto._
import cats.data.NonEmptyList

// See https://github.com/iamcal/emoji-data#using-the-data
// for the spec/protocol that this [[net.model.Emoji]] follows.
case class Emoji(name: String, unicode: NonEmptyList[String])
object Emoji {
  implicit val emojiDecoder: Decoder[Emoji] = deriveDecoder[Emoji]
}
