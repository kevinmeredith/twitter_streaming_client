package net.model

import io.circe._, io.circe.generic.semiauto._

case class HashTag(text: String)
object HashTag {
  implicit val hashTagDecoder: Decoder[HashTag] = deriveDecoder[HashTag]
}

case class EntityUrl(url: String)
object EntityUrl {
  implicit val entityUrlDecoder: Decoder[EntityUrl] = deriveDecoder[EntityUrl]
}

case class Media(display_url: String)
object Media {
  implicit val mediaDecoder: Decoder[Media] = deriveDecoder[Media]
}

case class Entities(hashtags: List[HashTag], urls: List[EntityUrl], media: Option[List[Media]])
object Entities {
  implicit val entitiesDecoder: Decoder[Entities] = deriveDecoder[Entities]
}

// See https://dev.twitter.com/overview/api/tweets
// for the Tweet's JSON spec/protocol.
case class Tweet(text: String, entities: Entities ) {
  val hashTags: List[String]    = entities.hashtags.map(_.text)
  val urls:     List[String]    = entities.urls.map(_.url)
  val twitterPics: List[String] = entities.media.map(_.map(_.display_url)).getOrElse(Nil)
}
object Tweet {
  implicit val tweetDecoder: Decoder[Tweet] = deriveDecoder[Tweet]
}
