package net

import java.io.File
import cats.data.Xor
import scalaz._
import net.model.Tweet
import io.circe._
import net.service.EmojiService
import net.service.TweetService.processTweetStream
import scalaz.concurrent.Task
import scalaz.stream.Process

object Main {

	import net.service.StatusRepositoryImpl.stati

	private def readJsonToTweet(json: Json): Task[Tweet] = json.as[Tweet] match {
		case Xor.Right(tweet) => Task.now { tweet }
		case Xor.Left(e)			=> Task.fail(e)
	}

	def main(args: Array[String]): Unit =
		mainHelper(Array.empty).unsafePerformAsyncInterruptibly {
			case \/-(success) => ???
			case -\/(e)       => ???
		}

	def mainHelper(args: Array[String]): Task[Unit] = for {
		file   <- Task { new File(this.getClass().getResource("/emoji.json").toURI) }
		emojis <- EmojiService.read(file)
		_      <- processTweetStream(stati.flatMap { json => Process.eval( readJsonToTweet(json) ) }, emojis).runLog
	} yield ()

//		stati.map(processTweetStream).runLog.unsafePerformAsyncInterruptibly {
//			case \/-(_) => println("read tweet")
//			case -\/(t) => throw new RuntimeException(s"Failure - going down due to $t.")
//		}

}
