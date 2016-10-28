package net.service

import twitter4j._

object TweetService {
	
	private val listener = new StatusListener() {
		override def onStatus(s: Status): Unit = 
			println(s.getUser.getName + " : " + s.getText)
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
}