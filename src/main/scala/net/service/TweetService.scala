package net.service

import org.joda.time.{DateTime, Duration}
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}
import net.model.{ Emoji, Tweet }
import scalaz.concurrent.Task
import scalaz.stream.Process

object TweetService {

  // Total tweets
  private val tweetCount = new AtomicLong

  // Hash Tag -> Count
  private val hashtags: ConcurrentMap[String, AtomicLong] = new ConcurrentHashMap[String, AtomicLong]()

  // URL Domain -> Count
  private val domains: ConcurrentMap[String, AtomicLong] = new ConcurrentHashMap[String, AtomicLong]()

  // Tweet counts having a URL
  private val tweetsHavingUrl = new AtomicLong

  // Tweet counts having a Twitter `media` Picture or Instagram URL
  // According to http://www.windowscentral.com/how-automatically-post-instagram-photos-twitter,
  // "While tweeting links to Instagram photos is still possible, you can no longer view the photos on Twitter."
  private val tweetsHavingTwitterOrInstagramPicture = new AtomicLong

  // Tweet Counts of any Tweet having at least a single Emoji.
  private val emojiTweets: ConcurrentMap[Emoji, AtomicLong] = new ConcurrentHashMap[Emoji, AtomicLong]()

  // Count of how many tweets have at least 1 emoji
  private val tweetsWithEmojiCount = new AtomicLong


  //def scan[B](b: B)(f: (B,O) => B): Process[F,B] =

  def f(tweets: Process[Task, Tweet], start: DateTime): Process[Task, Metrics] = {
    val accumulator = Metrics(0, 0, 0, 0, 0, Nil, Nil, 0, 0, Nil)

    tweets.scan(accumulator) { (acc, elem) =>
      val now = DateTime.now
      val newTweetCount = acc.tweetCount + 1
      acc.copy(
        tweetCount             = newTweetCount,
        averageTweetsPerHour   = averageTweetsPerHour(start, now, newTweetCount),
        averageTweetsPerMin    = averageTweetsPerMinute(start, now, newTweetCount),
        averageTweetsPerSec    = averageTweetsPerSecond(start, now, newTweetCount),
        percentTweetsWithEmoji =

      )
    }
  }


  case class Metrics(tweetCount: Long,
                     averageTweetsPerHour: BigDecimal,
                     averageTweetsPerMin: BigDecimal,
                     averageTweetsPerSec: BigDecimal,
                     percentTweetsWithEmoji: BigDecimal,
                     topEmojis: List[(Emoji, Long)],
                     topHashTags: List[(String, Long)],
                     percentOfTweetsWithUrl: BigDecimal,
                     percentOfTweetContainingTwitterOrInstagramPic: BigDecimal,
                     topUrlDomains: List[(String, Long)]
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

  def metrics(start: DateTime): Metrics =
    Metrics(
      tweetCount                                    = totalTweets,
      averageTweetsPerHour                          = averageTweetPerHour(start),
      averageTweetsPerMin                           = averageTweetPerMinute(start),
      averageTweetsPerSec                           = averageTweetPerSecond(start),
      percentTweetsWithEmoji                        = percentageTweetsHavingEmojis,
      topEmojis                                     = top5Emojis,
      topHashTags                                   = top5HashTags,
      percentOfTweetsWithUrl                        = percentageHavingUrl,
      percentOfTweetContainingTwitterOrInstagramPic = percentageHavingPicture,
      topUrlDomains                                 = top5Domains
    )

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

  def averageTweetsPerHour(start: DateTime, now: DateTime, tweetCount: Long): BigDecimal = {
    val duration       = new Duration(start, now)
    val hoursElapsed = duration.getStandardHours
    if (hoursElapsed == 0)
      0
    else
      BigDecimal.valueOf(tweetCount) / BigDecimal.valueOf(hoursElapsed)
  }

  def percentageHavingUrl: BigDecimal =
    percentage ( tweetsHavingUrl.get )

  def percentageHavingPicture: BigDecimal =
      percentage ( tweetsHavingTwitterOrInstagramPicture.get )

  private def percentage(count: Long, totalTweetCount: Long): BigDecimal = {
    val decimalPercent = BigDecimal.valueOf( count ) / BigDecimal.valueOf ( totalTweetCount )
    decimalPercent * BigDecimal.valueOf( 100 )
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
