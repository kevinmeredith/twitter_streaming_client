package net.web

import org.http4s._
import org.http4s.dsl._
import org.joda.time.DateTime

object MetricsService {

  def metrics(start: DateTime) = HttpService {
    case GET -> Root / "metrics" => Ok( "TODO" )
  }

}
