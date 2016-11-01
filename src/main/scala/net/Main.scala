package net

import org.http4s.server.{Server, ServerApp}
import org.http4s.server.blaze.BlazeBuilder
import scalaz.concurrent.Task
import net.service.TweetService
import net.web.TweetHttpService

object Main extends ServerApp {

  override def server(args: List[String]): Task[Server] = for {
  	_      <- Task { TweetService.sample }
  	server <- (
		BlazeBuilder
      		.bindHttp(8080, "localhost")
      		.mountService(TweetHttpService.summary, "/api")
      		.start
  		)
  } yield server
}
