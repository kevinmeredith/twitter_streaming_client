package net.web

import org.http4s._
import org.http4s.dsl._

object TweetService {

  def personService = HttpService {
    case GET -> Root / "tweet" => ???   
  }	
}

