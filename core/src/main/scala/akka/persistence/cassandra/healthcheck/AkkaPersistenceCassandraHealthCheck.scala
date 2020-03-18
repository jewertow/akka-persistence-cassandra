/*
 * Copyright (C) 2016-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.persistence.cassandra.healthcheck

import akka.actor.ActorSystem
import akka.event.Logging
import akka.pattern.{ ask, AskTimeoutException }
import akka.persistence.Persistence
import akka.persistence.cassandra.journal.CassandraJournal.{ HealthCheckQuery, HealthCheckResponse }
import akka.util.Timeout

import scala.concurrent.{ ExecutionContextExecutor, Future }
import java.util.concurrent.TimeUnit.MILLISECONDS

import scala.util.control.NonFatal

class AkkaPersistenceCassandraHealthCheck(system: ActorSystem) extends (() => Future[Boolean]) {

  private[akka] val log = Logging.getLogger(system, getClass)

  private val healthCheckSettings = new HealthCheckSettings(system, system.settings.config)
  private val journalPluginId = s"${healthCheckSettings.pluginLocation}.journal"
  private val journalRef = Persistence(system).journalFor(journalPluginId)

  private implicit val ec: ExecutionContextExecutor = system.dispatchers.lookup(s"$journalPluginId.plugin-dispatcher")
  private implicit val timeout: Timeout = Timeout(healthCheckSettings.timeoutMs, MILLISECONDS)

  override def apply(): Future[Boolean] = {
    (journalRef ? HealthCheckQuery).mapTo[HealthCheckResponse].map(_.result).recoverWith {
      case _: AskTimeoutException =>
        log.warning("Failed to execute health check due to ask timeout")
        Future(false)
      case NonFatal(e) =>
        log.warning("Failed to execute health check due to: {}", e)
        Future(false)
    }
  }
}
