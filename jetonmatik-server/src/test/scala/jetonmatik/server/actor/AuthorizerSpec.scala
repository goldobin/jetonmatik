package jetonmatik.server.actor

import java.time.Instant

import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestProbe, TestActorRef, ImplicitSender, TestKit}
import jetonmatik.server.GeneratedKeys
import jetonmatik.server.model.Client
import jetonmatik.util.{Bytes, PasswordHash}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, FlatSpecLike}

import scala.concurrent.duration._

import scala.util.Random

class AuthorizerSpec
  extends TestKit(ActorSystem("AuthorizerSpec"))
  with ImplicitSender
  with FlatSpecLike
  with Matchers
  with MockitoSugar
  with BeforeAndAfterAll {


  import fakes.Basic._
  import fakes.User._
  import fakes.Time._
  import fakes.OAuth._

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  trait ClientData {
    val clientId = fakeUuid.toString

    val clientSecret = fakePassword
    val clientSecretHash = PasswordHash.createHash(clientSecret)

    val clientName = fakeFullName
    val clientTokenTtl = fakeAccessTokenTtl
    val clientTokenTtlSeconds = clientTokenTtl.getSeconds
    val clientScope = fakeScopeSet()

    val client = Client(
      id = clientId,
      secretHash = clientSecretHash,
      name = clientName,
      scope = clientScope,
      tokenTtl = clientTokenTtl
    )
  }

  trait ActorUnderTest extends GeneratedKeys {

    val accessTokenGeneratorProbe = TestProbe()
    val now = fakeInstant

    trait TestNowProvider extends NowProvider {
      override def instant(): Instant = now
    }

    val timeout = 1.second

    lazy val actor = TestActorRef(Props(
      new Authorizer with AuthorizationWorkerPropsProvider {
        lazy val authorizationWorkerProps: Props =
          Props(new AuthorizationWorker(accessTokenGeneratorProbe.ref, timeout) with TestNowProvider)
      }
    ))
  }

  import Authorizer._
  import ClientStorage._
  import AccessTokenGenerator._

  "Authorizer ! GenerateToken" should "respond with token with intersected scope and token ttl" in new ActorUnderTest with ClientData {

    val scope = Random.shuffle(fakeScopeSet(12) ++ clientScope)
    val requestScope = scope.take(8).toSet

    val accessToken = Bytes.toBase64String(fakeByteArray(3)())

    actor ! Authorize(
      client = client.copy(scope = clientScope),
      scope = requestScope
    )

    accessTokenGeneratorProbe.expectMsgPF() {
      case Generate(resultClientId, resultScope, resultIssueTime, resultExpirationTime) =>
        resultClientId should be (clientId)
        resultScope should be (requestScope intersect clientScope)

        resultIssueTime.toInstant should be (now)
        resultExpirationTime.toInstant should be (now.plus(clientTokenTtl))
    }
    accessTokenGeneratorProbe.reply(Generated(accessToken))

    expectMsg(Authorized(
      accessToken,
      requestScope intersect clientScope,
      clientTokenTtlSeconds))
  }


  it should "respond with failure if token generator timed out to respond" in new ActorUnderTest with ClientData {

    override val timeout = 1.milli

    actor ! Authorize(client, clientScope)

    accessTokenGeneratorProbe.expectMsgType[Generate]

    expectMsg(FailedToAuthorize)
  }
}
