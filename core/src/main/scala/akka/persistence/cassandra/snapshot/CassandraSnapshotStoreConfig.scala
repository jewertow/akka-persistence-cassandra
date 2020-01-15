/*
 * Copyright (C) 2016-2017 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.persistence.cassandra.snapshot

import scala.collection.immutable

import com.typesafe.config.Config
import akka.persistence.cassandra.CassandraPluginConfig
import akka.actor.ActorSystem
import akka.persistence.cassandra.CassandraPluginConfig.getReplicationStrategy
import akka.persistence.cassandra.compaction.CassandraCompactionStrategy
import akka.persistence.cassandra.getListFromConfig

class CassandraSnapshotStoreConfig(system: ActorSystem, config: Config) extends CassandraPluginConfig(system, config) {
  val writeProfile: String = config.getString("write-profile")
  val readProfile: String = config.getString("read-profile")

  CassandraPluginConfig.checkProfile(system, readProfile)
  CassandraPluginConfig.checkProfile(system, writeProfile)

  val table: String = config.getString("table")

  val tableCompactionStrategy: CassandraCompactionStrategy =
    CassandraCompactionStrategy(config.getConfig("table-compaction-strategy"))

  val replicationStrategy: String = getReplicationStrategy(
    config.getString("replication-strategy"),
    config.getInt("replication-factor"),
    getListFromConfig(config, "data-center-replication-factors"))

  val gcGraceSeconds: Long = config.getLong("gc-grace-seconds")

  val maxLoadAttempts = config.getInt("max-load-attempts")
  val cassandra2xCompat = config.getBoolean("cassandra-2x-compat")

  /**
   * The Cassandra Statement[_]that can be used to create the configured keyspace.
   *
   * This can be queried in for example a startup script without accessing the actual
   * Cassandra plugin actor.
   *
   * {{{
   * new CassandraSnapshotStoreConfig(actorSystem, actorSystem.settings.config.getConfig("cassandra-journal.snapshot")).createKeyspaceStatement
   * }}}
   *
   * @see [[CassandraSnapshotStoreConfig#createTablesStatements]]
   */
  def createKeyspaceStatement: String =
    statements.createKeyspace

  /**
   * Scala API: The Cassandra statements that can be used to create the configured tables.
   *
   * This can be queried in for example a startup script without accessing the actual
   * Cassandra plugin actor.
   *
   * {{{
   * new CassandraSnapshotStoreConfig(actorSystem, actorSystem.settings.config.getConfig("cassandra-snapshot-store")).createTablesStatements
   * }}}
   * *
   * * @see [[CassandraSnapshotStoreConfig#createKeyspaceStatement]]
   */
  def createTablesStatements: immutable.Seq[String] =
    statements.createTable :: Nil

  /**
   * Java API: The Cassandra statements that can be used to create the configured tables.
   *
   * This can be queried in for example a startup script without accessing the actual
   * Cassandra plugin actor.
   *
   * {{{
   * new CassandraSnapshotStoreConfig(actorSystem, actorSystem.settings().config().getConfig("cassandra-snapshot-store")).getCreateTablesStatements();
   * }}}
   * *
   * * @see [[CassandraSnapshotStoreConfig#createKeyspaceStatement]]
   */
  def getCreateTablesStatements: java.util.List[String] = {
    import scala.collection.JavaConverters._
    createTablesStatements.asJava
  }

  private def statements: CassandraStatements =
    new CassandraStatements {
      override def snapshotConfig: CassandraSnapshotStoreConfig =
        CassandraSnapshotStoreConfig.this
    }
}
