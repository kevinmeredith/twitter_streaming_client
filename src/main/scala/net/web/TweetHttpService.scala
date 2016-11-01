package net.web

import org.http4s._
import org.http4s.dsl._
import net.service.TweetService.tweetCount

object TweetHttpService {

  def summary = HttpService {
    case GET -> Root / "count" => Ok(tweetCount.get.toString)
  }	

}