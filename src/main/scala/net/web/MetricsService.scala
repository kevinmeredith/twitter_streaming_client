package net.web

import org.http4s._
import net.service.TweetService.{ metrics => tweetMetrics }
import org.http4s.dsl._
import org.joda.time.DateTime

object MetricsService {

  def metrics(start: DateTime) = HttpService {
    case GET -> Root / "metrics" => Ok( tweetMetrics(start).toString )
  }

}
