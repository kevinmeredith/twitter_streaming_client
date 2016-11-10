package net

import cats.data.Xor
import net.model.Tweet
import org.http4s.client.oauth1

object Main {

	import net.service.StatusRepositoryImpl.stati

	// OAuth Tokens for accessing Twitter's sample streams API
	private val AccessToken       = "23021955-NeZKh2JAYW1Q5y0Ki9RQYtBtydNxN5ObHm7lNdIw2"
	private val AccessTokenSecret = "o6SZfroTRuR5VmDqYwYvH8N0ea2sD3DNn0TcgYqJuJolZ"

	private val TwitterStreamingSample = "https://stream.twitter.com/1.1/statuses/sample.json"

	private val ConsumerKey    = "AOvkb6d11mvmB7rI6zymrCQYZ"
	private val ConsumerSecret = "sLTDy0GR4Htqku0keM7znXgjXnMED9w1P1S3gbxLBkhk3JzILN"

	private val oauthConsumer = oauth1.Consumer(ConsumerKey, ConsumerSecret)

	def main(args: Array[String]): Unit = {
		val response = stati(oauthConsumer, AccessToken, AccessTokenSecret)(TwitterStreamingSample).take(1000).runLog.unsafePerformSync
		 println("response count: " + response.size)
		 response.foreach { x =>
			 x.as[Tweet] match {
				 case Xor.Right(success) => success.entities.media.foreach(println(_))
				 case Xor.Left(_)				 => ()
			 }
		 }
		 sys.exit(0)
	}

}
