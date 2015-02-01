package jetonmatik.model

import java.time.Duration

case class Client (
  id: String,
  secretHash: String,
  name: String = "",
  scope: Set[String] = Set.empty,
  tokenTtl: Duration = Duration.ofSeconds(60)
) {
  require(id.nonEmpty)
  require(secretHash.nonEmpty)
  require(!tokenTtl.isNegative || !tokenTtl.isZero)
}

case class ClientCredentials (id: String, secret: String) {
  override def toString: String = s"${this.getClass.getName} {id=$id,secret:***}"
}

