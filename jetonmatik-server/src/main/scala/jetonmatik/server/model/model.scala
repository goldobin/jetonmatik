package jetonmatik.server.model

import java.time.Duration

case class Client (
  clientId: String,
  clientSecretHash: String,
  name: String = "",
  scope: Set[String] = Set.empty,
  tokenTtl: Duration = Duration.ofSeconds(60)
) {
  require(clientId.nonEmpty)
  require(clientSecretHash.nonEmpty)
  require(!tokenTtl.isNegative || !tokenTtl.isZero)
}

