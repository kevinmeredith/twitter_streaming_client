package net.web

import net.service.TweetService.{ClientMetrics, InternalMetrics}
import org.http4s._
import org.http4s.dsl._
import org.joda.time.DateTime

import scalaz.stream.async.mutable.Signal

object MetricsService {

  def metrics(start: DateTime, metrics: Signal[InternalMetrics]) = HttpService {
    case GET -> Root / "metrics" => metrics.get.flatMap { m => Ok(ClientMetrics.fromInternalMetrics(m).toString) }
  }

}