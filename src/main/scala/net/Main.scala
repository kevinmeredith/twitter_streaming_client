package net

import org.http4s._
import org.http4s.client.blaze._
import org.http4s.client.oauth1
import scalaz.concurrent.Task
import scalaz.stream.Process
import jawnstreamz._
import io.circe.Json
import java.time.{LocalDateTime, Duration}

object Main {

	private val AccessToken       = "23021955-NeZKh2JAYW1Q5y0Ki9RQYtBtydNxN5ObHm7lNdIw2"
	private val AccessTokenSecret = "o6SZfroTRuR5VmDqYwYvH8N0ea2sD3DNn0TcgYqJuJolZ"

	private val TwitterStreamingSample = "https://stream.twitter.com/1.1/statuses/sample.json"

	private val ConsumerKey    = "AOvkb6d11mvmB7rI6zymrCQYZ"
	private val ConsumerSecret = "sLTDy0GR4Htqku0keM7znXgjXnMED9w1P1S3gbxLBkhk3JzILN"

	// Credit to Paul Snively for the following code.
	// http://scastie.org/23401

	implicit val f = io.circe.jawn.CirceSupportParser.facade
	val consumer = oauth1.Consumer(ConsumerKey, ConsumerSecret)

	val client = PooledHttp1Client()

	def stati(tokenAccess: String, tokenSecret: String)(uri: String): Process[Task, Json] = {
		val token = oauth1.Token(tokenAccess, tokenSecret)
		for {
			uri  <- Process.eval(Task.fromDisjunction(Uri.fromString(uri)))
			twapi  = Request(uri = uri)
			signed = oauth1.signRequest(twapi, consumer, callback = None, verifier = None, token = Some(token))
			req  <- Process.eval(signed)
			res  <- client.streaming(req)(resp => resp.body.parseJsonStream)
		} yield res
	}

	def main(args: Array[String]): Unit = {
	   val count = stati(AccessToken, AccessTokenSecret)(TwitterStreamingSample).take(10).runLog.run
	   println(count)
	}

}
