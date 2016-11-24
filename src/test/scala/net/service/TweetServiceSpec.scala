package net.service

import org.scalatest._
import net.model.{Entities, HashTag, Tweet}
import scalaz.concurrent.Task
import scalaz.stream.Process

class TweetServiceSpec extends FlatSpec with Matchers with BeforeAndAfterEach {

  val tweet1 = Tweet("empty", Entities(hashtags = Nil, urls = Nil, media = None))

  "Getting the tweet count of Stream[Task, Tweet] when taking 100 items" should "show a tweetCount of 100" in {
    val stream: Process[Task, Tweet] = Process.repeatEval(Task.now(tweet1)).take(100)
    val readStream: Unit             = TweetService.processTweetStream(stream, Nil).run.unsafePerformSync
    TweetService.totalTweets             should be (100)
    TweetService.percentageHavingUrl     should be (0)
    TweetService.percentageHavingPicture should be (0)
    TweetService.top5HashTags            should be (Nil)
    TweetService.top5Emojis              should be (Nil)
  }

  val hashTag1 = "foo"
  val hashTag2 = "bar"

  val tweetWith2HashTags = Tweet("empty",
    Entities( hashtags = List( HashTag(hashTag2), HashTag(hashTag2), HashTag(hashTag1) ), urls = Nil, media = None)
  )

  val runs = 555

  s"Getting the tweet count of Stream[Task, Tweet] when taking $runs items" should s"show a tweetCount of $runs and hash tags" in {
    val stream: Process[Task, Tweet] = Process.repeatEval(Task.now(tweetWith2HashTags)).take(runs)
    val readStream: Unit             = TweetService.processTweetStream(stream, Nil).run.unsafePerformSync
    TweetService.totalTweets             should be (runs)
    TweetService.percentageHavingUrl     should be (0)
    TweetService.percentageHavingPicture should be (0)
    TweetService.top5HashTags            should be ( List( (hashTag2, 2*runs), (hashTag1, 1*runs))) // recall that tweetWith2HashTags repeats 555 times.
    TweetService.top5Emojis              should be (Nil)
  }

  override def beforeEach(): Unit = {
    TweetService.clearMetrics()
  }

}
