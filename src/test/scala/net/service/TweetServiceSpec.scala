package net.service

import net.model.Emoji._
import org.scalatest._
import net.model._
import net.service.TweetService.InternalMetrics
import org.joda.time.DateTime
import scalaz.concurrent.Task
import scalaz.stream.{Process, async}

class TweetServiceSpec extends FlatSpec with Matchers  {

  val tweet1 = Tweet("empty", Entities(hashtags = Nil, urls = Nil, media = None))
  val count = 100L

  s"Getting tweet metrics of Stream[Task, Tweet] when taking $count items" should s"show a tweetCount of $count" in {
    val start                                = DateTime.now
    val stream: Process[Task, Option[Tweet]] = Process.repeatEval(Task.now(Some(tweet1))).take(100)
    val zero                                 = InternalMetrics.empty(start)
    val runningMetrics                       = async.signalOf(zero)
    TweetService.f(stream, zero, Nil).map(async.mutable.Signal.Set.apply).to(runningMetrics.sink).run.unsafePerformSync
    runningMetrics.get.unsafePerformSync should be ( zero.copy(tweetCount = count) )
  }

  val hashTag1 = "foo"
  val hashTag2 = "bar"

  val tweetWith2HashTags = Tweet("empty",
    Entities( hashtags = List( HashTag(hashTag2), HashTag(hashTag2), HashTag(hashTag1) ), urls = Nil, media = None)
  )

  val runs = 555L

  s"Getting tweet metrics of Stream[Task, Tweet] when taking $runs items" should s"show a tweetCount of $runs and hash tags" in {
    val start                                = DateTime.now
    val zero                                 = InternalMetrics.empty(start)
    val runningMetrics                       = async.signalOf(zero)
    val stream: Process[Task, Option[Tweet]] = Process.repeatEval(Task.now(Some(tweetWith2HashTags))).take(runs.toInt)
    TweetService.f(stream, zero, Nil).map(async.mutable.Signal.Set.apply).to(runningMetrics.sink).run.unsafePerformSync
    runningMetrics.get.unsafePerformSync should be (
      zero.copy(tweetCount = runs, tweetsHavingHashTag =  runs, tweetedHashTags = Map( (hashTag1, 1*runs), (hashTag2, 2*runs) ))
    )
  }

  import net.service.EmojiServiceSpec.codePointsToString

  val runs2 = 9999L

  val emojiSingleCodePoint     = Emoji.fromString(Some("COPYRIGHT SIGN"), "00A9").get
  val emojiTwoCodePoints       = Emoji.fromString(Some("REGIONAL INDICATOR SYMBOL LETTERS ZM"), "1F1FF-1F1F2").get
  val emojiSingleCodePointText = "\u00A9"
  val emojiTwoCodePointsText   = convertToCodePoints(List("1F1FF", "1F1F2")).map(codePointsToString).get

  val emojis = List(emojiSingleCodePoint, emojiTwoCodePoints)

  val tweetText2Emojis = s"$emojiTwoCodePointsText blah blah $emojiSingleCodePointText $emojiTwoCodePointsText"

  val tweetWith2Emojis = Tweet(tweetText2Emojis, Entities( hashtags = Nil , urls = Nil, media = None) )

  s"Getting tweet metrics of Stream[Task, Tweet] when taking $runs2 items" should s"show a tweetCount of $runs2 and emojis" in {
    val start                                = DateTime.now
    val zero                                 = InternalMetrics.empty(start)
    val runningMetrics                       = async.signalOf(zero)
    val stream: Process[Task, Option[Tweet]] = Process.repeatEval(Task.now(Some(tweetWith2Emojis))).take(runs2.toInt)
    TweetService.f(stream, zero, emojis).map(async.mutable.Signal.Set.apply).to(runningMetrics.sink).run.unsafePerformSync
    runningMetrics.get.unsafePerformSync should be (
      zero.copy(tweetCount = runs2, tweetsHavingEmojis = runs2, tweetedEmojis = Map((emojiSingleCodePoint, runs2), (emojiTwoCodePoints, runs2*2)))
    )
  }

  import TweetService.InstagramPhotoPrefix

  val tweetWithInstagramText = s"$InstagramPhotoPrefix/myvacation"

  val Yahoo  = "www.yahoo.com"
  val Google = "www.google.com"

  val tweetWithInstagramAndTwitterPic = Tweet(tweetWithInstagramText,
    Entities( hashtags = Nil , urls = List( EntityUrl(Yahoo), EntityUrl(Google) ), media = Some( List(Media("a.png"), Media("b.png"))))
  )

  val runs3 = 42L

  s"Getting tweet metrics of Stream[Task, Tweet] when taking $runs3 items" should s"show a tweetCount of $runs3, no emojis or hash tags, but present twitter and instagram pics present" in {
    val start                                = DateTime.now
    val zero                                 = InternalMetrics.empty(start)
    val runningMetrics                       = async.signalOf(zero)
    val stream: Process[Task, Option[Tweet]] = Process.repeatEval(Task.now(Some(tweetWithInstagramAndTwitterPic))).take(runs3.toInt)
    TweetService.f(stream, zero, Nil).map(async.mutable.Signal.Set.apply).to(runningMetrics.sink).run.unsafePerformSync
    runningMetrics.get.unsafePerformSync should be (
      zero.copy(
        tweetCount                        = runs3,
        tweetsHavingUrl                   = runs3,
        tweetsHavingTwitterOrInstagramPic = runs3,
        tweetedUrls = Map( (Yahoo, runs3), (Google, runs3))
      )
    )
  }

}
