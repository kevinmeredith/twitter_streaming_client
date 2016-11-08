package net.service

import org.joda.time.{Duration, DateTime}
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}
import java.util.function.{Function => jFunction}
import net.model.Tweet

object TweetService {

  // Total tweets
  private val tweetCount = new AtomicLong

  // Hash Tag -> Count
  private val hashtags = new ConcurrentHashMap[String, AtomicLong]()

  // URL Domain -> Count
  private val domains = new ConcurrentHashMap[String, AtomicLong]()

  // Tweet counts having a URL
  private val tweetsHavingUrl = new AtomicLong

  // Tweet counts having a URL of Twitter or Instagram
  private val tweetsHavingTwitterOrInstagramPicture = new AtomicLong

  def updateCount(tweet: Tweet): Unit =
    tweetCount.getAndIncrement(); ()

  // credit: http://stackoverflow.com/a/26214475/409976
  def updateHashCodeCount(tweet: Tweet): Unit = {
    val update = new jFunction[String, AtomicLong] {
      override def apply(x: String) = new AtomicLong()
    }

    tweet.hashTags.foreach { ht =>
      hashtags.computeIfAbsent(ht, update).getAndIncrement(); ()
    }
  }

  def averageTweetPerSecond(start: DateTime): Long = {
    val fromStart = new Duration(start, DateTime.now())
    tweetCount.get() / fromStart.getStandardSeconds
  }

  def averageTweetPerMinute(start: DateTime): Long = {
    val fromStart = new Duration(start, DateTime.now())
    tweetCount.get() / fromStart.getStandardMinutes
  }

  def averageTweetPerHour(start: DateTime): Long = {
    val fromStart = new Duration(start, DateTime.now())
    tweetCount.get() / fromStart.getStandardHours
  }

  def updateTweetCountHavingPicture(tweet: Tweet): Unit = {
    tweet.urls.foreach { url =>
      val upper = url.toUpperCase
      if ( validSite(url) && photo(url) ) {
        tweetsHavingTwitterOrInstagramPicture.incrementAndGet(); ()
      }
      else {
        ()
      }
    }

    def validSite(url: String): Boolean = {
      val upper = url.toUpperCase
      upper.contains("TWITTER.COM") || upper.contains("INSTAGRAM.COM")
    }

    def photo(url: String): Boolean = {
      val upper = url.toUpperCase
      upper.endsWith("PNG") || upper.endsWith("JPEG")
    }

  }
}
