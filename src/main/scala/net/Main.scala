package net

import java.io.File

import cats.data.Xor
import net.model.Tweet
import io.circe._
import net.service.EmojiService
import scalaz._
import net.service.TweetService.{InternalMetrics, f}
import scalaz.stream.async
import scalaz.concurrent.Task
import scalaz.stream.Process
import org.http4s.server.{Server, ServerApp}
import net.web.MetricsService.metrics
import org.http4s.server.blaze.BlazeBuilder
import org.joda.time.DateTime

import scalaz.stream.async.mutable.Signal

object Main extends ServerApp {

	import net.service.StatusRepositoryImpl.stati

	private val initialMetrics                          = InternalMetrics.empty(DateTime.now)
	private val currentMetrics: Signal[InternalMetrics] = async.signalOf(initialMetrics)

	override def server(args: List[String]): Task[Server] = {
		for {
      s <- {
        BlazeBuilder
          .bindHttp(8080, "localhost")
          .mountService(metrics(DateTime.now, currentMetrics), "/api")
          .start
      }
			_ <- Task.now( tweetStream.unsafePerformAsync {
				case \/-(_) => ()
				case -\/(e) => println("error: " + e.getStackTrace.mkString("\n"))
			} )
		} yield s
	}

	private def tweetStream: Task[Unit] = for {
		file    <- Task { new File(this.getClass.getResource("/emoji.json").toURI) }
		emojis  <- EmojiService.read(file)
	  // credit to Paul Snively and https://www.chrisstucchio.com/blog/2014/scalaz_streaming_tutorial.html
		running <- f(stati.flatMap { json => Process.eval( readJsonToTweet(json) ) },
			initialMetrics,
			emojis).map(async.mutable.Signal.Set.apply).to(currentMetrics.sink).run
	} yield running

	private def readJsonToTweet(json: Json): Task[Option[Tweet]] = json.as[Tweet] match {
		case Xor.Right(tweet) => Task.now { Some(tweet) }
		case Xor.Left(_)			=> Task.now { None }
	}

}
