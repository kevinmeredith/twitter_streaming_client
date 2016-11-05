package net.model

import io.circe._, io.circe.generic.semiauto._

case class HashTag(text: String)
object HashTag {
  implicit val hashTagDecoder: Decoder[HashTag] = deriveDecoder[HashTag]
}

case class Entities(hashtags: List[HashTag])
object Entities {
  implicit val entitiesDecoder: Decoder[Entities] = deriveDecoder[Entities]
}

case class Tweet(text: String, entities: Entities ) {
  val hashTags: List[String] = entities.hashtags.map(_.text)
}
object Tweet {
  implicit val tweetDecoder: Decoder[Tweet] = deriveDecoder[Tweet]
}
