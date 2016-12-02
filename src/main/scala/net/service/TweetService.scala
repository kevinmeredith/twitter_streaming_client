package net.service

import org.joda.time.{DateTime, Duration}
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}

import net.model.{Emoji, HashTag, Tweet}

import scalaz.concurrent.Task
import scalaz.stream.Process

object TweetService {
  //def scan[B](b: B)(f: (B,O) => B): Process[F,B] =

  def f(tweets: Process[Task, Tweet], emojis: List[Emoji], start: DateTime): Process[Task, Metrics] = {
    val accumulator = Metrics(0, 0, 0, 0, 0, Nil, Nil, 0, 0, Nil)

    tweets.scan(accumulator) { (acc, tweet) =>

      val now                           = DateTime.now
      val tweetCount                    = acc.tweetCount + 1
      val singleTweetEmojis             = EmojiService.findAll(emojis, tweet.text)
      val runningTweetedEmojis          = singleTweetEmojis ++ acc.tweetedEmojis
      val runningTweetsWithEmojisCount  = singleTweetEmojis match {
        case Nil    => acc.tweetsHavingEmojis
        case _ :: _ => acc.tweetsHavingEmojis + 1
      }
      val runningTweetsWithUrlCount = tweet.urls map {
        case _ :: _ => acc.tweetsHavingUrl + 1
        case Nil    => acc.tweetsHavingUrl
      }
      val runningTweetedUrls = tweet.urls ++ acc.tweetedUrls
      val runningTweetsWithHashTagsCount = tweet.hashTags match {
        case Nil    => acc.tweetsHavingHashTag
        case _ :: _ => acc.tweetsHavingHashTag + 1
      }
      val runningTweetedHashTags = tweet.hashTags ++ acc.tweetedHashTags

      acc.copy(
        tweetCount             = tweetCount,
        averageTweetsPerHour   = averageTweetsPerHour(start, now, tweetCount),
        averageTweetsPerMin    = averageTweetsPerMinute(start, now, tweetCount),
        averageTweetsPerSec    = averageTweetsPerSecond(start, now, tweetCount),
        percentTweetsWithEmoji = percentage(tweetsHavingEmojis, tweetCount),
        tweetedEmojis          = runningTweetedEmojis,
        tweetsHavingEmojis     = runningTweetsWithEmojisCount,
        topEmojis              = ???,
        tweetedHashTags        = runningTweetedHashTags,
        topHashTags            = ???,
        tweetedUrls            = runningTweetsWithUrlCount,
        tweetsHavingUrl        = runningTweetedUrls,
        topUrlDomains          = ???
        percentOfTweetsWithUrl = percentage(runningTweetedUrls, tweetCount),
        twitterOrInstagramPic  = ???,
        tweetsHavingTwitterOrInstagramPic = ???,
        percentOfTweetContainingTwitterOrInstagramPic = percentage(???, tweetCount)
      )
    }
  }

  case class Metrics(tweetCount: BigDecimal,
                     averageTweetsPerHour: BigDecimal,
                     averageTweetsPerMin: BigDecimal,
                     averageTweetsPerSec: BigDecimal,
                     percentTweetsWithEmoji: BigDecimal,
                     tweetedEmojis: List[Emoji],
                     tweetsHavingEmojis: BigDecimal,
                     topEmojis: List[(Emoji, Long)],
                     tweetsHavingHashTag: BigDecimal,
                     tweetedHashTags: List[String],
                     topHashTags: List[(String, Long)],
                     tweetedUrls: List[String],
                     tweetsHavingUrl: BigDecimal,
                     topUrlDomains: List[(String, Long)]
                     percentOfTweetsWithUrl: BigDecimal,
                     twitterOrInstagramPic: List[String],
                     tweetsHavingTwitterOrInstagramPic: BigDecimal,
                     percentOfTweetContainingTwitterOrInstagramPic: BigDecimal
                    ) {
    override def toString: String =
      s"""
         |tweetCount:           ${this.tweetCount}                                    \n
         |averageTweetsPerHour: ${this.averageTweetsPerHour}                          \n
         |averageTweetsPerMin:  ${this.averageTweetsPerMin}                           \n
         |averageTweetsPerSec : ${this.averageTweetsPerSec}                           \n
         |% with emoji(s)       ${this.percentTweetsWithEmoji}                        \n
         |top 5 emojis          ${this.topEmojis}                                     \n
         |top 5 hash tags       ${this.topHashTags}                                   \n
         |% with url(s)         ${this.percentOfTweetsWithUrl}                        \n
         |% with twitter/insta  ${this.percentOfTweetContainingTwitterOrInstagramPic} \n
         |top 5 urls            ${this.topUrlDomains}
       """.stripMargin
  }

  /**
    * Given a Stream of Tweet's, mutate state to keep track of metrics, e.g.
    * total # of tweets, top hash tags, etc.
    */
  def processTweetStream(p: Process[Task, Option[Tweet]], emojis: List[Emoji]): Process[Task, Unit] =
    p.flatMap {
      case Some(tweet) =>
        Process.eval {
          for {
            _ <- updateCount(tweet)
            _ <- updateHashTagCount(tweet)
            _ <- updateEmojiMetricsPerTweet(tweet, emojis)
            _ <- updateTweetCountPicture(tweet)
            _ <- updateTweetCountHavingUrl(tweet)
            _ <- updateDomains(tweet)
          } yield ()
        }
      case None => Process.eval( Task.now( () ) )
    }

  private def updateDomains(tweet: Tweet): Task[Unit] =
    Task {
      tweet.urls.foreach { url =>
        domains.computeIfAbsent(url, updateHelper).getAndIncrement()
      }
    }

  private def updateCount(tweet: Tweet): Task[Unit] =
    Task { tweetCount.getAndIncrement(); () }

  private def updateEmojiMetricsPerTweet(tweet: Tweet, emojis: List[Emoji]): Task[Unit] = {
      val foundEmojis = EmojiService.findAll(emojis, tweet.text)
      Task {
        foundEmojis.foreach { e =>
          emojiTweets.computeIfAbsent(e, updateHelper).getAndIncrement()
        }
        if (foundEmojis.nonEmpty) {
          tweetsWithEmojiCount.getAndIncrement()
          ()
        }
        else {
          ()
        }
      }
    }

  // credit: http://stackoverflow.com/a/26214475/409976
  private def updateHashTagCount(tweet: Tweet): Task[Unit] =
    Task {
      tweet.hashTags.foreach { ht =>
        hashtags.computeIfAbsent(ht, updateHelper).getAndIncrement(); ()
      }
    }

  private def updateTweetCountHavingUrl(tweet: Tweet): Task[Unit] =
    tweet.urls match {
      case _ :: _ => Task { tweetsHavingUrl.getAndIncrement; () }
      case Nil    => Task.now( () )// no need to update count since there's no URLs
    }

  private def updateTweetCountPicture(tweet: Tweet): Task[Unit] =
    findTwitterPic(tweet).orElse(findInstagramUrl(tweet)) match {
      case Some(_) => Task.now {  tweetsHavingTwitterOrInstagramPicture.getAndIncrement(); () }
      case None    => Task.now( () )
    }

  // Clear all metrics, i.e. reset the state of each metric
  // Currently, this method is only used in testing. In the event
  // that it needs to be used in `main` code, then verify that
  // each of these reset methods runs concurrently without race conditions.
  private [service] def clearMetrics(): Unit = {
    tweetCount.set(0)
    hashtags.clear()
    domains.clear()
    tweetsHavingUrl.set(0)
    tweetsHavingTwitterOrInstagramPicture.set(0)
    emojiTweets.clear()
    tweetsWithEmojiCount.set(0)
  }

  import collection.JavaConverters._

  // Retrieve total number of tweets received so far
  def totalTweets: Long =
    tweetCount.get

  private def ordering[A] = new Ordering[(Long, A)] {
    override def compare(x: (Long, A), y: (Long, A)): Int =
      Ordering.ordered[Long].reverse.compare(x._1, y._1)
  }

  // Return top 5 elements by Long count, i.e. number of occurrences, in Map[A, Long].
  private def top5[A](map: Map[A, AtomicLong]): List[(A, Long)] = {
    val orderedByCountDescending: List[(A, Long)] = map.toList.map { case (k, v) => (k, v.get) }.sortBy(_._2)(Ordering.ordered[Long].reverse)
    orderedByCountDescending.take(5)
  }

  def top5HashTags: List[(String, Long)] = top5[String](hashtags.asScala.toMap)

  def top5Emojis: List[(Emoji, Long)]    = top5[Emoji](emojiTweets.asScala.toMap)

  def top5Domains: List[(String, Long)]  = top5[String](domains.asScala.toMap)

  def percentageTweetsHavingEmojis: BigDecimal = {
    val decimalPercent = tweetsHavingEmojis / BigDecimal.valueOf(tweetCount.get)
    decimalPercent * BigDecimal.valueOf( 100 )
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

  def percentageHavingUrl: BigDecimal =
    percentage ( tweetsHavingUrl.get )

  def percentageHavingPicture: BigDecimal =
      percentage ( tweetsHavingTwitterOrInstagramPicture.get )

  private def percentage(count: Long, totalTweetCount: Long): BigDecimal = {
    if(totalTweetCount == 0) 0
    else {
      val decimalPercent = BigDecimal.valueOf( count ) / BigDecimal.valueOf ( totalTweetCount )
      decimalPercent * BigDecimal.valueOf( 100 )
    }
  }

  def tweetsHavingEmojis: BigDecimal = BigDecimal.valueOf( tweetsWithEmojiCount.get() )

  private def findTwitterPic(tweet: Tweet): Option[String] =
    tweet.twitterPics match {
      case h :: t => Some(h)
      case Nil    => None
    }

  // Assumption - I saw that a handful of Instagram photos' URLs
  // starting with the following prefix
  val InstagramPhotoPrefix = "WWW.INSTAGRAM.COM/P"

  private def findInstagramUrl(tweet: Tweet): Option[String] =
    tweet.urls.find(_.toUpperCase.startsWith(InstagramPhotoPrefix))

  import java.util.function.{Function => jFunction}

  private def updateHelper[A] = new jFunction[A, AtomicLong] {
    override def apply(x: A) = new AtomicLong()
  }

}
