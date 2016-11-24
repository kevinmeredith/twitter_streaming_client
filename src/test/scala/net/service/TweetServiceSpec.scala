package net.service

import net.model.Emoji._
import org.scalatest._
import net.model._

import scalaz.concurrent.Task
import scalaz.stream.Process

class TweetServiceSpec extends FlatSpec with Matchers with BeforeAndAfterEach {

  val tweet1 = Tweet("empty", Entities(hashtags = Nil, urls = Nil, media = None))
  
  "Getting tweet metrics of Stream[Task, Tweet] when taking 100 items" should "show a tweetCount of 100" in {
    val stream: Process[Task, Tweet] = Process.repeatEval(Task.now(tweet1)).take(100)
    val readStream: Unit             = TweetService.processTweetStream(stream, Nil).run.unsafePerformSync
    TweetService.totalTweets                  should be (100)
    TweetService.percentageHavingUrl          should be (0)
    TweetService.percentageHavingPicture      should be (0)
    TweetService.percentageTweetsHavingEmojis should be (0)
    TweetService.top5HashTags                 should be (Nil)
    TweetService.top5Emojis                   should be (Nil)

  }

  val hashTag1 = "foo"
  val hashTag2 = "bar"

  val tweetWith2HashTags = Tweet("empty",
    Entities( hashtags = List( HashTag(hashTag2), HashTag(hashTag2), HashTag(hashTag1) ), urls = Nil, media = None)
  )

  val runs = 555

  s"Getting tweet metrics of Stream[Task, Tweet] when taking $runs items" should s"show a tweetCount of $runs and hash tags" in {
    val stream: Process[Task, Tweet] = Process.repeatEval(Task.now(tweetWith2HashTags)).take(runs)
    val readStream: Unit             = TweetService.processTweetStream(stream, Nil).run.unsafePerformSync
    TweetService.totalTweets                  should be (runs)
    TweetService.percentageHavingUrl          should be (0)
    TweetService.percentageHavingPicture      should be (0)
    TweetService.percentageTweetsHavingEmojis should be (0)
    TweetService.top5HashTags                 should be ( List( (hashTag2, 2*runs), (hashTag1, 1*runs))) // recall that tweetWith2HashTags repeats 555 times.
    TweetService.top5Emojis                   should be (Nil)
  }

  import net.model.EmojiServiceSpec.codePointsToString

  val runs2 = 9999

  val emojiSingleCodePoint     = Emoji.fromString(Some("COPYRIGHT SIGN"), "00A9").get
  val emojiTwoCodePoints       = Emoji.fromString(Some("REGIONAL INDICATOR SYMBOL LETTERS ZM"), "1F1FF-1F1F2").get
  val emojiSingleCodePointText = "\u00A9"
  val emojiTwoCodePointsText   = convertToCodePoints(List("1F1FF", "1F1F2")).map(codePointsToString).get

  val tweetText2Emojis = s"$emojiTwoCodePointsText blah blah $emojiSingleCodePointText $emojiTwoCodePointsText"

  val tweetWith2Emojis = Tweet(tweetText2Emojis, Entities( hashtags = Nil , urls = Nil, media = None) )

  s"Getting tweet metrics of Stream[Task, Tweet] when taking $runs2 items" should s"show a tweetCount of $runs2 and emojis" in {
    val stream: Process[Task, Tweet] = Process.repeatEval(Task.now(tweetWith2Emojis)).take(runs2)
    val readStream: Unit             = TweetService.processTweetStream(stream, List(emojiSingleCodePoint, emojiTwoCodePoints)).run.unsafePerformSync
    TweetService.totalTweets                  should be (runs2)
    TweetService.percentageHavingUrl          should be (0)
    TweetService.percentageTweetsHavingEmojis should be (100)
    TweetService.percentageHavingPicture      should be (0)
    TweetService.top5HashTags                 should be (Nil)
    TweetService.top5Emojis                   should be (List( (emojiTwoCodePoints, runs2 * 2), (emojiSingleCodePoint, runs2) ) )
  }

  import TweetService.InstagramPhotoPrefix

  val tweetWithInstagramText = s"$InstagramPhotoPrefix/myvacation"

  val tweetWithInstagramAndTwitterPic = Tweet(tweetWithInstagramText,
    Entities( hashtags = Nil , urls = List( EntityUrl("www.yahoo.com"), EntityUrl("www.google.com") ), media = Some( List(Media("a.png"), Media("b.png"))))
  )

  val runs3 = 42
  
  s"Getting tweet metrics of Stream[Task, Tweet] when taking $runs3 items" should s"show a tweetCount of $runs3, no emojis or hash tags, but present twitter and instagram pics present" in {
    val stream: Process[Task, Tweet] = Process.repeatEval(Task.now(tweetWithInstagramAndTwitterPic)).take(runs3)
    val readStream: Unit             = TweetService.processTweetStream(stream, List(emojiSingleCodePoint, emojiTwoCodePoints)).run.unsafePerformSync
    TweetService.totalTweets                  should be (runs3)
    TweetService.percentageHavingUrl          should be (100)
    TweetService.percentageHavingPicture      should be (100)
    TweetService.percentageTweetsHavingEmojis should be (0)
    TweetService.top5HashTags                 should be (Nil)
    TweetService.top5Emojis                   should be (Nil)
  }
  
  // net.service.TweetService contains mutable state, i.e. for metrics.
  // Clear it before every test to avoid one test affecting the other's metrics.
  override def beforeEach(): Unit = {
    TweetService.clearMetrics()
  }

}
