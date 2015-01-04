package jetonmatik.server.actor

import java.time.ZoneOffset

import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jwt.SignedJWT
import jetonmatik.server.GeneratedKeys
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, FlatSpecLike}


class AccessTokenGeneratorSpec
  extends TestKit(ActorSystem("AccessTokenGeneratorSpec"))
  with ImplicitSender
  with FlatSpecLike
  with Matchers
  with MockitoSugar
  with BeforeAndAfterAll {

  import fakes.Basic._
  import fakes.OAuth._
  import fakes.Time._
  import fakes.Net._

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  trait TokenData {
    val clientId = fakeUuid.toString
    val scope: Set[String] = fakeScopeSet()

    val now = fakeInstant
    val validityDuration = fakeDuration

    val issueTime = now.atOffset(ZoneOffset.UTC)
    val expirationTime = issueTime.plus(validityDuration)
  }

  trait ActorUnderTest extends GeneratedKeys {
    val issuer = fakeHttpsUrl

    lazy val actor = TestActorRef(Props(new AccessTokenGenerator(rsaPrivateKey, issuer)))
  }

  "AccessTokenGenerator" should "generate valid token" in new ActorUnderTest with TokenData {

    import AccessTokenGenerator._

    actor ! GenerateAccessToken(
      clientId = clientId,
      scope = scope,
      issueTime = issueTime,
      expirationTime = expirationTime
    )

    expectMsgPF() {
      case AccessToken(accessToken) =>
        val verifier = new RSASSAVerifier(rsaPublicKey)
        val signedJwt = SignedJWT.parse(accessToken)

        val json = signedJwt.getPayload.toJSONObject

        val jwtClientId = json.get("clientId").asInstanceOf[String]
        val jwtScope = json.get("scope").asInstanceOf[String]
        val jwtIss = json.get("iss").asInstanceOf[String]
        val jwtIat = json.get("iat").asInstanceOf[Long]
        val jwtExp = json.get("exp").asInstanceOf[Long]

        jwtClientId should be (clientId)
        jwtScope should be (scope.mkString(" "))
        jwtIss should be (issuer)
        jwtIat should be (issueTime.toInstant.getEpochSecond)
        jwtExp should be (expirationTime.toInstant.getEpochSecond)

        signedJwt.verify(verifier) shouldBe true
    }
  }
}
