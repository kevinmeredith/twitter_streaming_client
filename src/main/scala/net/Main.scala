package net

import cats.data.Xor
import net.model.Tweet
import io.circe._

object Main {

	import net.service.StatusRepositoryImpl.stati

	def main(args: Array[String]): Unit = {
		val response: Vector[Json] = stati(42).take(80).runLog.unsafePerformSync
		 println("response count: " + response.size)
		 response.foreach { json => json.as[Tweet] match {
			 case Xor.Right(tweet) => println(tweet.twitterPics)
			 case Xor.Left(e)      => println(json)
		 	}
		 }
		 sys.exit(0)
	}

}
