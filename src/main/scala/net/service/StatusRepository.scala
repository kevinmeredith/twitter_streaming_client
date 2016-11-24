package net.service

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

  /**
    * Return a Stream of [[net.model.Tweet]] with effect [[scalaz.concurrent.Task]].
    */
  def stati: Process[Task, io.circe.Json]
}

object StatusRepositoryImpl extends StatusRepository {

  // Credit to Paul Snively for the following code.
  // http://scastie.org/23401
  implicit val f = io.circe.jawn.CirceSupportParser.facade

  private val client = PooledHttp1Client()

  private val TwitterStreamingSample = "https://stream.twitter.com/1.1/statuses/sample.json"

  private val ConsumerKey    = "AOvkb6d11mvmB7rI6zymrCQYZ"
  private val ConsumerSecret = "sLTDy0GR4Htqku0keM7znXgjXnMED9w1P1S3gbxLBkhk3JzILN"

  private val oauthConsumer = oauth1.Consumer(ConsumerKey, ConsumerSecret)

  // OAuth Tokens for accessing Twitter's sample streams API
  private val AccessToken       = "23021955-NeZKh2JAYW1Q5y0Ki9RQYtBtydNxN5ObHm7lNdIw2"
  private val AccessTokenSecret = "o6SZfroTRuR5VmDqYwYvH8N0ea2sD3DNn0TcgYqJuJolZ"

  override def stati: Process[Task, io.circe.Json] = {
    val token = oauth1.Token(AccessToken, AccessTokenSecret)
    for {
      uri    <- Process.eval(Task.fromDisjunction(Uri.fromString(TwitterStreamingSample)))
      twapi  = Request(uri = uri)
      signed = oauth1.signRequest(twapi, oauthConsumer, callback = None, verifier = None, token = Some(token))
      req    <- Process.eval(signed)
      res    <- client.streaming(req)(resp => resp.body.parseJsonStream)
    } yield res
  }


}
