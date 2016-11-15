package net.service

import org.scalatest._
import net.model.{Entities, Tweet}
import scalaz.concurrent.Task
import scalaz.stream.Process

class TweetServiceSpec extends FlatSpec with Matchers {

  val tweet1 = Tweet("empty", Entities(hashtags = Nil, urls = Nil, media = None))

  "Getting the tweet count of Stream[Task, Tweet] when taking 100 items" should "show a tweetCount of 100" in {
    val stream: Process[Task, Tweet] = Process.repeatEval(Task.now(tweet1)).take(100)
    val readStream: Unit             = TweetService.processTweetStream(stream, Nil).run.unsafePerformSync
    TweetService.totalTweets             should be (100)
    TweetService.percentageHavingUrl     should be (0)
    TweetService.percentageHavingPicture should be (0)
    TweetService.top5HashTags            should be (Nil)
  }

}
