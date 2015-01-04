package jetonmatik.server.actor

import java.security.interfaces.RSAPrivateKey
import java.time.OffsetDateTime
import java.util.Date

import akka.actor.{Props, Actor}
import com.nimbusds.jose.{JWSAlgorithm, JWSHeader}
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.{SignedJWT, JWTClaimsSet}

object AccessTokenGenerator {
  case class GenerateAccessToken(
    clientId: String,
    scope: Set[String],
    issueTime: OffsetDateTime,
    expirationTime: OffsetDateTime) {

    require(clientId.nonEmpty)
    require(expirationTime.compareTo(issueTime) > 0)
  }

  case class AccessToken(accessToken: String) {
    require(accessToken.nonEmpty)
  }

  def props(
    privateKey: RSAPrivateKey,
    issuer: String) =
    Props(new AccessTokenGenerator(privateKey, issuer))
}

class AccessTokenGenerator(
  privateKey: RSAPrivateKey,
  issuer: String)
  extends Actor {

  import AccessTokenGenerator._

  override def receive: Receive = {
    case GenerateAccessToken(clientId, scope, issueTime, expirationTime) =>
      val signer = new RSASSASigner(privateKey)

      val claimsSet = new JWTClaimsSet()

      claimsSet.setIssuer(issuer)
      claimsSet.setIssueTime(Date.from(issueTime.toInstant))
      claimsSet.setExpirationTime(Date.from(expirationTime.toInstant))

      claimsSet.setCustomClaim("clientId", clientId)

      if (scope.nonEmpty) {
        claimsSet.setCustomClaim("scope", scope.mkString(" "))
      }

      val signedJwt = new SignedJWT(
        new JWSHeader(JWSAlgorithm.RS256),
        claimsSet)
      signedJwt.sign(signer)

      val accessToken = signedJwt.serialize()

      sender() ! AccessToken(accessToken)
  }
}
