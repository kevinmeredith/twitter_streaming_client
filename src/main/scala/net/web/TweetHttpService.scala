package net.web

import org.http4s._
import org.http4s.dsl._

object TweetHttpService {

  def personService = HttpService {
    case GET -> Root / "tweet" => ???   
  }	

}

