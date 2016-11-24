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

  /**
    * Given a Stream of Tweet's, mutate state to keep track of metrics, e.g.
    * total # of tweets, top hash tags, etc.
    */
  def processTweetStream(p: Process[Task, Tweet], emojis: List[Emoji]): Process[Task, Unit] =
    p.flatMap { tweet =>
      Process.eval {
        for {
          _ <- updateCount(tweet)
          _ <- updateHashTagCount(tweet)
          _ <- updateTweetCountPicture(tweet)
          _ <- updateTweetCountHavingUrl(tweet)
          _ <- updateTweetsWithEmoji(tweet, emojis)
        } yield ()
      }
    }

  private def updateCount(tweet: Tweet): Task[Unit] =
    Task { tweetCount.getAndIncrement(); () }

  private def updateTweetsWithEmoji(tweet: Tweet, emojis: List[Emoji]): Task[Unit] = {
      val foundEmojis = EmojiService.findAll(emojis, tweet.text)
      val updated     = foundEmojis.map(e => emojiTweets.computeIfAbsent(e, updateHelper).getAndIncrement())
      Task.now { updated }.map( _ => ())
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

//  private val tweetCount = new AtomicLong
//
//  // Hash Tag -> Count
//  private val hashtags: ConcurrentMap[String, AtomicLong] = new ConcurrentHashMap[String, AtomicLong]()
//
//  // URL Domain -> Count
//  private val domains: ConcurrentMap[String, AtomicLong] = new ConcurrentHashMap[String, AtomicLong]()
//
//  // Tweet counts having a URL
//  private val tweetsHavingUrl = new AtomicLong
//
//  // Tweet counts having a Twitter `media` Picture or Instagram URL
//  // According to http://www.windowscentral.com/how-automatically-post-instagram-photos-twitter,
//  // "While tweeting links to Instagram photos is still possible, you can no longer view the photos on Twitter."
//  private val tweetsHavingTwitterOrInstagramPicture = new AtomicLong
//
//  // Tweet Counts of any Tweet having at least a single Emoji.
//  private val emojiTweets: ConcurrentMap[Emoji, AtomicLong] = new ConcurrentHashMap[Emoji, AtomicLong]()


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
  }

  import collection.JavaConverters._
  import scala.collection.SortedMap

  // Retrieve total number of tweets received so far
  def totalTweets: Long =
    tweetCount.get

  // Return top 5 elements by Long count, i.e. number of occurrences, in Map[A, Long].
  private def top5[A](map: Map[A, AtomicLong]): List[(A, Long)] = {
    val reversed: List[(Long, A)] = map.map { case (a, count) => (count.get, a) }.toList
    val sorted                    = SortedMap[Long, A]( reversed : _* )(Ordering.ordered[Long].reverse)
    sorted.take(5).toList.collect {
      case (count, ht) => (ht, count)
    }
  }

  def top5HashTags: List[(String, Long)] = top5[String](hashtags.asScala.toMap)

  def top5Emojis: List[(Emoji, Long)]    = top5[Emoji](emojiTweets.asScala.toMap)

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

  def tweetsHavingEmojis: BigDecimal =
    emojiTweets.asScala.toMap.map {
      case (_, v) => BigDecimal.valueOf( v.get )
    }.sum

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
