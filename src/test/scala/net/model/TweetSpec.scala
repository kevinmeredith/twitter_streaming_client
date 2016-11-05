package net.model

import io.circe.parser._
import org.scalatest._
import cats.data.Xor

class TweetSpec extends FlatSpec with Matchers {

  "A Hash Tag" should "be decoded into a [[net.model.HashTag]]" in {
    val json = parse("""{ "text" : "bippy" } """).getOrElse(throw new RuntimeException("Invalid JSON"))
    json.as[HashTag] should be (Xor.right(HashTag("bippy")))
  }

  "An array of HashTag's" should "be decoded into a List[[net.model.HashTag]]" in {
    val json = parse("""[ { "text" : "bippy" } ]""").getOrElse(throw new RuntimeException("Invalid JSON"))
    json.as[List[HashTag]] should be (Xor.right(List(HashTag("bippy"))))
  }

  "An Entity" should "be decoded into a [[net.model.Entity]]" in {
    val json = parse("""{ "hashtags" : [ { "text" : "bippy" } ] }""").getOrElse(throw new RuntimeException("Invalid JSON"))
    json.as[Entities] should be (Xor.right(Entities(List(HashTag("bippy")))))
  }

  "A real Tweet" should "be decoded into a [[net.model.Tweet]]" in {

    val file         = this.getClass().getResource("/Tweet.json")
    val tweetContent = scala.io.Source.fromURL(file, "UTF-8").mkString
    val tweet        = parse(tweetContent).getOrElse(throw new RuntimeException("Invalid JSON"))
    val obj          = tweet.as[Tweet]
    val expected     = Tweet(
      text     = "blah blah blah tweet",
      entities = Entities(List(HashTag("bippy")))
    )
    obj should be (Xor.right(expected))
  }

}
