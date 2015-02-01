package jetonmatik.actor

import java.time.Instant

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import fakes.{Basic, OAuth, Time, User}
import jetonmatik.model.Client
import jetonmatik.provider.NowProvider
import jetonmatik.util.{GeneratedKeys, Bytes, PasswordHash}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._
import scala.util.Random

class AuthorizerSpec
  extends TestKit(ActorSystem("AuthorizerSpec"))
  with ImplicitSender
  with FlatSpecLike
  with Matchers
  with BeforeAndAfterAll {


  import fakes.Basic._
  import fakes.OAuth._
  import fakes.Time._
  import fakes.User._

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

  import jetonmatik.actor.AccessTokenGenerator._
  import jetonmatik.actor.Authorizer._

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
