package net.service

import twitter4j._
//import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicLong

object TweetService {

	val tweetCount = new AtomicLong()

	private val listener = new StatusListener() {
		override def onStatus(s: Status): Unit = 
			incrementTweetCount()
		override def onDeletionNotice(n: StatusDeletionNotice): Unit = ()
		override def onTrackLimitationNotice(numberOfLimitedStatuses: Int): Unit = ()
		override def onException(e: Exception): Unit = 
			println(e.getStackTrace)
		override def onScrubGeo(userId: Long, upToStatusId: Long): Unit = ()
		override def onStallWarning(w: StallWarning): Unit = ()
	}

	// Making lazy since I'm not sure what would happen if `stream.sample` were called twice.
	// Nor, I think, would it make sense to call it twice.
	lazy val sample: Unit = {
		val stream: TwitterStream = (new TwitterStreamFactory).getInstance
		stream.addListener(listener)
		stream.sample()		
	}

	private def incrementTweetCount(): Unit = {
		tweetCount.getAndIncrement()
		()
	}

}