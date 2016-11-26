package net

import java.io.File
import cats.data.Xor
import net.model.Tweet
import io.circe._
import net.service.EmojiService
import net.service.TweetService.processTweetStream
import org.joda.time.{DateTime, Period}
import net.service.TweetService.printMetrics
import scalaz.concurrent.Task
import scalaz.stream.Process

object Main {

	import net.service.StatusRepositoryImpl.stati

	private def printMetricsEvery2Minutes(start: DateTime, now: DateTime): Task[Unit] = {
		if (new Period(start, now).getMinutes % 1 == 0) {
				for {
					_ <- Task { printMetrics }
					_ <- printMetricsEvery2Minutes(start, DateTime.now)
				} yield ()
			}
		else {
			printMetricsEvery2Minutes(start, DateTime.now)
		}
	}

	private def readJsonToTweet(json: Json): Task[Option[Tweet]] = json.as[Tweet] match {
		case Xor.Right(tweet) => Task.now { Some(tweet) }
		case Xor.Left(_)			=> Task.now { None }
	}

	def main(args: Array[String]): Unit = {
		val result = for {
			_ <- mainHelper
			_ <- Task.now ( () ) //printMetricsEvery2Minutes(DateTime.now, DateTime.now)
		} yield ()
		result.unsafePerformSync
	}

	private def mainHelper: Task[Unit] = for {
		file   <- Task { new File(this.getClass.getResource("/emoji.json").toURI) }
		emojis <- EmojiService.read(file)
		_      <- processTweetStream(stati.flatMap { json => Process.eval( readJsonToTweet(json) ) }, emojis).runLog
	} yield ()

}
