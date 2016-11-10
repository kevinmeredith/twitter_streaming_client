package net.service

import io.circe.Json
import org.http4s.{Request, Uri}
import org.http4s.client.blaze.PooledHttp1Client
import org.http4s.client.oauth1
import scalaz.concurrent.Task
import scalaz.stream.Process
import jawnstreamz._

/**
  * The `StatusRepository` returns a stream of `Json` with `Task` effects.
  */
trait StatusRepository {
  def stati(consumer: oauth1.Consumer, tokenAccess: String, tokenSecret: String)(uri: String): Process[Task, Json]
}

object StatusRepositoryImpl extends StatusRepository {

  // Credit to Paul Snively for the following code.
  // http://scastie.org/23401

  implicit val f = io.circe.jawn.CirceSupportParser.facade

  val client = PooledHttp1Client()

  // stati is plural of status
  def stati(consumer: oauth1.Consumer, tokenAccess: String, tokenSecret: String)(uri: String): Process[Task, Json] = {
    val token = oauth1.Token(tokenAccess, tokenSecret)
    for {
      uri  <- Process.eval(Task.fromDisjunction(Uri.fromString(uri)))
      twapi  = Request(uri = uri)
      signed = oauth1.signRequest(twapi, consumer, callback = None, verifier = None, token = Some(token))
      req  <- Process.eval(signed)
      res  <- client.streaming(req)(resp => resp.body.parseJsonStream)
    } yield res
  }

}
