package net.model

import io.circe.parser._
import org.scalatest._
import cats.data.Xor

class TweetSpec extends FlatSpec with Matchers {

  "A real Tweet" should "be decoded into a [[net.model.Tweet]]" in {

    val file         = this.getClass().getResource("/Tweet.json")
    val tweetContent = scala.io.Source.fromURL(file, "UTF-8").mkString
    val tweet        = parse(tweetContent).getOrElse(throw new RuntimeException("Invalid JSON"))
    println("Tweet:" + tweet)
    val obj          = tweet.as[Tweet]
    val expected     = Tweet(
      text     = "blah blah blah tweet",
      entities = Entities(
        hashtags = List( HashTag( "bippy" ) ),
        urls     = List( EntityUrl( "http://foo.bar.com" ) )
      )
    )
    obj should be (Xor.right(expected))
  }
}
