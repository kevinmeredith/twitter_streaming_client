package net.service

import cats.data.NonEmptyList
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
  private val tweetsHavingEmoji = new AtomicLong

  /**
    * Given a Stream of Tweet's, mutate state to keep track of metrics, e.g.
    * total # of tweets, top hash tags, etc.
    */
  def readStream(p: Process[Task, Tweet], emojis: NonEmptyList[Emoji]): Process[Task, Unit] =
    p.map { tweet =>
      updateCount(tweet)
      updateHashTagCount(tweet)
      updateTweetCountPicture(tweet)
      updateTweetCountHavingUrl(tweet)
      updateTweetsWithEmoji(tweet, emojis)
    }

  private def updateCount(tweet: Tweet): Unit = {
    val result = tweetCount.getAndIncrement()
    () // ignoring result since this function is a side-effect
  }

  private def updateTweetsWithEmoji(tweet: Tweet, emojis: NonEmptyList[Emoji]): Unit = ???


  import java.util.function.{Function => jFunction}

  // credit: http://stackoverflow.com/a/26214475/409976
  private def updateHashTagCount(tweet: Tweet): Unit = {
    val update = new jFunction[String, AtomicLong] {
      override def apply(x: String) = new AtomicLong()
    }

    tweet.hashTags.foreach { ht =>
      hashtags.computeIfAbsent(ht, update).getAndIncrement(); ()
    }
  }

  private def updateTweetCountHavingUrl(tweet: Tweet): Unit =
    tweet.urls match {
      case _ :: _ => val ignoredResult = tweetsHavingUrl.getAndIncrement; ()
      case Nil    => () // no need to update count since there's no URLs
    }

  private def updateTweetCountPicture(tweet: Tweet): Unit =
    findTwitterPic(tweet).orElse(findInstagramUrl(tweet)) match {
      case Some(_) =>
        val result = tweetsHavingTwitterOrInstagramPicture.getAndIncrement()
        () // ignore since this method updates a counter
      case None    => ()
    }

  import collection.JavaConverters._
  import scala.collection.SortedMap

  // Retrieve total number of tweets received so far
  def totalTweets: Long =
    tweetCount.get

  // Return top-5 Hash Tags by count
  def top5HashTags: List[(String, Long)] = {
    val reversed: List[(Long, String)] = hashtags.asScala.toList.map { case (ht, count) => (count.get, ht) }
    val map                            = SortedMap[Long, String]( reversed : _*)(Ordering.ordered[Long].reverse)
    map.take(5).toList.collect {
      case (count, ht) => (ht, count)
    }
  }

  def averageTweetPerSecond(start: DateTime): BigDecimal = {
    val fromStart = new Duration(start, DateTime.now())
    BigDecimal.valueOf(tweetCount.get()) / BigDecimal.valueOf(fromStart.getStandardSeconds)
  }

  def averageTweetPerMinute(start: DateTime): BigDecimal = {
    val fromStart = new Duration(start, DateTime.now())
    BigDecimal.valueOf(tweetCount.get()) / BigDecimal.valueOf(fromStart.getStandardMinutes)
  }

  def averageTweetPerHour(start: DateTime): BigDecimal = {
    val fromStart = new Duration(start, DateTime.now())
    BigDecimal.valueOf(tweetCount.get()) / BigDecimal.valueOf(fromStart.getStandardHours)
  }

  def percentageHavingUrl: BigDecimal =
    percentage ( tweetsHavingUrl.get )

  def percentageHavingPicture: BigDecimal =
      percentage ( tweetsHavingTwitterOrInstagramPicture.get )

  private def percentage(count: Long): BigDecimal = {
    val decimalPercent = BigDecimal.valueOf( count ) / BigDecimal.valueOf ( tweetCount.get )
    decimalPercent * BigDecimal.valueOf( 100 )
  }

  private def findTwitterPic(tweet: Tweet): Option[String] =
    tweet.twitterPics match {
      case h :: t => Some(h)
      case Nil    => None
    }

  private def findInstagramUrl(tweet: Tweet): Option[String] = {
    // Assumption - I saw that a handful of Instagram photos' URLs
    // starts with the following prefix
    val InstagramPhotoPrefix = "WWW.INSTAGRAM.COM/P"

    tweet.urls.find(_.toUpperCase.startsWith(InstagramPhotoPrefix))
  }

}
