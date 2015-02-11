package jetonmatik.model

import java.time.{OffsetDateTime, Duration}

case class Client (
  id: String,
  secretHash: String,
  scope: Set[String] = Set.empty,
  tokenTtl: Duration = Duration.ofSeconds(60)
) {
  require(id.nonEmpty)
  require(secretHash.nonEmpty)
}

case class ClientCredentials (id: String, secret: String) {
  override def toString: String = s"${this.getClass.getName} {id=$id,secret:***}"
}

case class MetaInfo (
  lastModified: OffsetDateTime,
  version: Long
)

