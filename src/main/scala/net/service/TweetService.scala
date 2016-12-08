package net.service

import org.joda.time.{DateTime, Duration}
import net.model.{Emoji, Tweet}

import scalaz.concurrent.Task
import scalaz.stream.Process

object TweetService {
  //def scan[B](b: B)(f: (B,O) => B): Process[F,B] =
  def f(tweets: Process[Task, Option[Tweet]],
        accumulator: InternalMetrics,
        eligibleEmojisInTweet: List[Emoji]): Process[Task, InternalMetrics] = {
    tweets.scan(accumulator) { (acc, tw) =>

      tw match {
        case None        => acc // if there's no Tweet, i.e. it's another message type, then return the input metrics
        case Some(tweet) =>
          val updatedTweetCount             = acc.tweetCount + 1
          val emojis                        = EmojiService.findAll(eligibleEmojisInTweet, tweet.text)
          val runningTweetsWithEmojisCount  = emojis match {
            case Nil    => acc.tweetsHavingEmojis
            case _ :: _ => acc.tweetsHavingEmojis + 1
          }
          val emojiMap                  = emojis.foldLeft(acc.tweetedEmojis)(addToMap)
          val runningTweetsWithUrlCount = tweet.urls match {
            case _ :: _ => acc.tweetsHavingUrl + 1
            case Nil    => acc.tweetsHavingUrl
          }
          val urlsMap = tweet.urls.foldLeft(acc.tweetedUrls)(addToMap)
          val hashTagCount = tweet.hashTags match {
            case Nil    => acc.tweetsHavingHashTag
            case _ :: _ => acc.tweetsHavingHashTag + 1
          }
          val hashTagsMap = tweet.hashTags.foldLeft(acc.tweetedHashTags)(addToMap)

          val instagramOrTwitterPic = findTwitterOrInstagramPic(tweet) match {
            case Some(_) => acc.tweetsHavingTwitterOrInstagramPic + 1
            case None    => acc.tweetsHavingTwitterOrInstagramPic
          }

          acc.copy(
            tweetCount                        = updatedTweetCount,
            tweetsHavingEmojis                = runningTweetsWithEmojisCount,
            tweetedEmojis                     = emojiMap,
            tweetsHavingHashTag               = hashTagCount,
            tweetedHashTags                   = hashTagsMap,
            tweetsHavingUrl                   = runningTweetsWithUrlCount,
            tweetedUrls                       = urlsMap,
            tweetsHavingTwitterOrInstagramPic = instagramOrTwitterPic
          )
      }
    }
  }

  //
  private def addToMap[A](m: Map[A, Long], elem: A): Map[A, Long] =
    m.get(elem) match {
      case Some(v) => m.updated(elem, v + 1)
      case None    => m + ((elem, 1L))
    }

  object InternalMetrics {
    def empty(start: DateTime) = InternalMetrics(0, 0, Map.empty, 0, Map.empty, 0, Map.empty, 0, start)
  }

  // Internal, i.e. system metrics, that become collected
  // in order to have the info to produce [[ClientMetrics]]
  case class InternalMetrics(tweetCount: Long,
                             tweetsHavingEmojis: Long,
                             tweetedEmojis: Map[Emoji, Long],
                             tweetsHavingHashTag: Long,
                             tweetedHashTags: Map[String, Long],
                             tweetsHavingUrl: Long,
                             tweetedUrls: Map[String, Long],
                             tweetsHavingTwitterOrInstagramPic: Long,
                             streamStart: DateTime)

  object ClientMetrics {
    def fromInternalMetrics(m: InternalMetrics) = {
      val now = DateTime.now
      ClientMetrics(
        tweetCount                   = m.tweetCount,
        avgTweetsPerHr               = averageTweetsPerHour(m.streamStart, now, m.tweetCount),
        avgTweetsPerMin              = averageTweetsPerMinute(m.streamStart, now, m.tweetCount),
        avgTweetsPerSec              = averageTweetsPerSecond(m.streamStart, now, m.tweetCount),
        percentTweetsWithEmoji       = percentage(m.tweetsHavingEmojis, m.tweetCount),
        top5Emojis                   = top5(m.tweetedEmojis),
        top5HashTags                 = top5(m.tweetedHashTags),
        percentTweetsWithUrl         = percentage(m.tweetsHavingUrl, m.tweetCount),
        percentWithInstaOrTwitterPic = percentage(m.tweetsHavingTwitterOrInstagramPic, m.tweetCount),
        top5Urls                     = top5(m.tweetedUrls)
      )
    }
  }

  // Metrics that the client cares about
  case class ClientMetrics(tweetCount: BigDecimal,
                           avgTweetsPerHr: BigDecimal,
                           avgTweetsPerMin: BigDecimal,
                           avgTweetsPerSec: BigDecimal,
                           percentTweetsWithEmoji: BigDecimal,
                           top5Emojis: List[(Emoji, Long)],
                           top5HashTags: List[(String, Long)],
                           percentTweetsWithUrl: BigDecimal,
                           percentWithInstaOrTwitterPic: BigDecimal,
                           top5Urls: List[(String, Long)]) {
    override def toString: String =
      s"""
         |tweetCount:           $tweetCount                   \n
         |averageTweetsPerHour: $avgTweetsPerHr               \n
         |averageTweetsPerMin:  $avgTweetsPerMin              \n
         |averageTweetsPerSec : $avgTweetsPerSec              \n
         |% with emoji(s)       $percentTweetsWithEmoji       \n
         |top 5 emojis          $top5Emojis                   \n
         |top 5 hash tags       $top5HashTags                 \n
         |% with url(s)         $percentTweetsWithUrl         \n
         |% with twitter/insta  $percentWithInstaOrTwitterPic \n
         |top 5 urls            $top5Urls
       """.stripMargin
  }

  // Return top 5 elements by Long count, i.e. number of occurrences, in Map[A, Long].
  private def top5[A](map: Map[A, Long]): List[(A, Long)] = {
    val orderedByCountDescending: List[(A, Long)] = map.toList.map { case (k, v) => (k, v) }.sortBy(_._2)(Ordering.ordered[Long].reverse)
    orderedByCountDescending.take(5)
  }

  def averageTweetsPerSecond(start: DateTime, now: DateTime, tweetCount: Long): BigDecimal = {
    val duration       = new Duration(start, now)
    val secondsElapsed = duration.getStandardSeconds
    if (secondsElapsed == 0)
      0
    else
      BigDecimal.valueOf(tweetCount) / BigDecimal.valueOf(secondsElapsed)
  }

  def averageTweetsPerMinute(start: DateTime, now: DateTime, tweetCount: Long): BigDecimal = {
    val duration       = new Duration(start, now)
    val minutesElapsed = duration.getStandardMinutes
    if (minutesElapsed == 0)
      0
    else
      BigDecimal.valueOf(tweetCount) / BigDecimal.valueOf(minutesElapsed)
  }

  def averageTweetsPerHour(start: DateTime, now: DateTime, tweetCount: BigDecimal): BigDecimal = {
    val duration       = new Duration(start, now)
    val hoursElapsed = duration.getStandardHours
    if (hoursElapsed == 0)
      0
    else
      tweetCount / BigDecimal.valueOf(hoursElapsed)
  }

  private def percentage(count: Long, totalTweetCount: Long): BigDecimal = {
    if(totalTweetCount == 0) 0
    else {
      val decimalPercent = BigDecimal.valueOf( count ) / BigDecimal.valueOf ( totalTweetCount )
      decimalPercent * BigDecimal.valueOf( 100 )
    }
  }

  // Assumption - I saw that a handful of Instagram photos' URLs
  // starting with the following prefix
  val InstagramPhotoPrefix = "WWW.INSTAGRAM.COM/P"

  private def findTwitterOrInstagramPic(tweet: Tweet): Option[String] =
    findTwitterPic(tweet).orElse(findInstagramUrl(tweet))

  private def findTwitterPic(tweet: Tweet): Option[String] =
    tweet.twitterPics match {
      case h :: t => Some(h)
      case Nil    => None
    }

  private def findInstagramUrl(tweet: Tweet): Option[String] =
    tweet.urls.find(_.toUpperCase.startsWith(InstagramPhotoPrefix))

}
