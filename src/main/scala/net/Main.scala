package net

import java.io.File

import cats.data.Xor
import net.model.Tweet
import io.circe._
import net.service.EmojiService
import net.service.TweetService.{InternalMetrics, f}

import scalaz.concurrent.Task
import scalaz.stream.Process
import org.http4s.server.{Server, ServerApp}
import net.web.MetricsService.metrics
import org.http4s.server.blaze.BlazeBuilder
import org.joda.time.DateTime

object Main extends ServerApp {

	import net.service.StatusRepositoryImpl.stati

	override def server(args: List[String]): Task[Server] = {
		for {
      s <- {
        BlazeBuilder
          .bindHttp(8080, "localhost")
          .mountService(metrics(DateTime.now), "/api")
          .start
      }
			_ <- tweetStream
		} yield s
	}

	private val emptyMetrics: DateTime => InternalMetrics =
		InternalMetrics(0, 0, Map.empty, 0, Map.empty, 0, Map.empty, 0, _)

	private def tweetStream: Task[Unit] = for {
		file   <- Task { new File(this.getClass.getResource("/emoji.json").toURI) }
		emojis <- EmojiService.read(file)
		_      <- f(stati.flatMap { json => Process.eval( readJsonToTweet(json) ) }, emptyMetrics(DateTime.now), emojis).runLog
	} yield ()

	private def readJsonToTweet(json: Json): Task[Option[Tweet]] = json.as[Tweet] match {
		case Xor.Right(tweet) => Task.now { Some(tweet) }
		case Xor.Left(_)			=> Task.now { None }
	}

}
