package jetonmatik.relational

import java.time.{Duration, OffsetDateTime}

import jetonmatik.model.{Client, MetaInfo}

sealed trait StoredMetaInfo {
  val lastModified: OffsetDateTime
  val version: Long
}

case class StoredClient(
  id: String,
  secretHash: String,
  scope: String,
  tokenTtl: Long,
  lastModified: OffsetDateTime,
  version: Long) extends StoredMetaInfo {
  def toClient(): (Client, MetaInfo) = {
    (
      Client(
        id,
        secretHash,
        scope.split(" ").toSet,
        Duration.ofSeconds(tokenTtl)
      ),
      MetaInfo(
        lastModified,
        version
      ))
  }
}
