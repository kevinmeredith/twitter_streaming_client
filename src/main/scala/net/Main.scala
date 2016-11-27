package net

import java.io.File
import cats.data.Xor
import net.model.Tweet
import io.circe._
import net.service.EmojiService
import net.service.TweetService.processTweetStream
import scalaz.concurrent.Task
import scalaz.stream.Process
import org.http4s.server.{Server, ServerApp}

object Main extends ServerApp {

	import net.service.StatusRepositoryImpl.stati

	override def server(args: List[String]): Task[Server] = {
		for {
			_ <- tweetStream
			s <- server
		} yield s
	}

	private def server: Task[Server] = ???

	private def tweetStream: Task[Unit] = for {
		file   <- Task { new File(this.getClass.getResource("/emoji.json").toURI) }
		emojis <- EmojiService.read(file)
		_      <- processTweetStream(stati.flatMap { json => Process.eval( readJsonToTweet(json) ) }, emojis).runLog
	} yield ()

	private def readJsonToTweet(json: Json): Task[Option[Tweet]] = json.as[Tweet] match {
		case Xor.Right(tweet) => Task.now { Some(tweet) }
		case Xor.Left(_)			=> Task.now { None }
	}

}
