package net

import java.io.File

import cats.data.Xor
import net.model.Tweet
import io.circe._
import net.service.EmojiService
import net.service.TweetService.{InternalMetrics, metrics, sink}
import scalaz.stream.async
import scalaz.concurrent.Task
import scalaz.stream.Process
import net.web.MetricsService.clientMetrics
import org.http4s.server.blaze.BlazeBuilder
import org.joda.time.DateTime

import scalaz.stream.async.mutable.Signal

object Main {

	import net.service.StatusRepositoryImpl.stati

	private val initialMetrics                          = InternalMetrics.empty(DateTime.now)
	private val currentMetrics: Signal[InternalMetrics] = async.signalOf(initialMetrics)

	def main(args: Array[String]): Unit = {
		val tasks = List(server, tweetStream)
		val run = Task.gatherUnordered(tasks).unsafePerformSync
		()
	}

	def server: Task[Unit] =
		BlazeBuilder
			.bindHttp(8080, "localhost")
			.mountService(clientMetrics(DateTime.now, currentMetrics), "/api")
			.start
	    .flatMap( _ => Task.now( () ) )

	private def tweetStream: Task[Unit] = for {
		file    <- Task { new File(this.getClass.getResource("/emoji.json").toURI) }
		emojis  <- EmojiService.read(file)
	  // credit to Paul Snively and https://www.chrisstucchio.com/blog/2014/scalaz_streaming_tutorial.html
		running <- sink(metrics(stati.flatMap { json => Process.eval( readJsonToTweet(json) ) },
			initialMetrics,
			emojis), currentMetrics).run
	} yield running

	private def readJsonToTweet(json: Json): Task[Option[Tweet]] = json.as[Tweet] match {
		case Xor.Right(tweet) => Task.now { Some(tweet) }
		case Xor.Left(_)			=> Task.now { None }
	}

}
