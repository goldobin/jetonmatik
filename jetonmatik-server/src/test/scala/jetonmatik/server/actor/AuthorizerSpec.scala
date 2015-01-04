package jetonmatik.server.actor

import java.time.Instant

import akka.actor.Status.Failure
import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestProbe, TestActorRef, ImplicitSender, TestKit}
import akka.util.Timeout
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
      clientId = clientId,
      clientSecretHash = clientSecretHash,
      name = clientName,
      scope = clientScope,
      tokenTtl = clientTokenTtl
    )
  }

  trait ActorUnderTest extends GeneratedKeys {

    val clientStorageProbe = TestProbe()
    val accessTokenGeneratorProbe = TestProbe()

    val now = fakeInstant

    trait TestNowProvider extends NowProvider {
      override def instant(): Instant = now
    }


    lazy val actor = TestActorRef(Props(
      new Authorizer(
        clientStorageProbe.ref,
        accessTokenGeneratorProbe.ref)
        with TestNowProvider))
  }

  import Authorizer._
  import ClientStorage._
  import AccessTokenGenerator._

  "Authorizer ! GenerateToken" should "respond with token with intersected scope and token ttl" in new ActorUnderTest with ClientData {

    val scope = Random.shuffle(fakeScopeSet(12) ++ clientScope)
    val requestScope = scope.take(8).toSet

    val accessToken = Bytes.toBase64String(fakeByteArray()())

    actor ! GenerateToken(
      clientId = clientId,
      scope = requestScope
    )

    clientStorageProbe.expectMsg(Read(clientId))
    clientStorageProbe.reply(ReadResult(Some(client)))

    accessTokenGeneratorProbe.expectMsgPF() {
      case GenerateAccessToken(resultClientId, resultScope, resultIssueTime, resultExpirationTime) =>
        resultClientId should be (clientId)
        resultScope should be (requestScope intersect clientScope)

        resultIssueTime.toInstant should be (now)
        resultExpirationTime.toInstant should be (now.plus(clientTokenTtl))
    }
    accessTokenGeneratorProbe.reply(AccessToken(accessToken))

    expectMsg(Token(
      accessToken,
      requestScope intersect clientScope,
      clientTokenTtlSeconds))
  }

  it should "respond with failure if client not found in storage" in new ActorUnderTest with ClientData {
    actor ! GenerateToken(clientId, clientScope)

    clientStorageProbe.expectMsgType[Read]
    clientStorageProbe.reply(ReadResult(None))

    accessTokenGeneratorProbe.expectNoMsg()

    expectMsgType[Failure]
  }

  ignore should "respond with failure if client storage timed out to respond" in new ActorUnderTest with ClientData {
    actor ! GenerateToken(clientId, clientScope)

    clientStorageProbe.expectMsgType[Read]
    accessTokenGeneratorProbe.expectNoMsg()

    expectMsgType[Failure]
  }

  ignore should "respond with failure if token generator timed out to respond" in new ActorUnderTest with ClientData {

    actor ! GenerateToken(clientId, clientScope)

    clientStorageProbe.expectMsgType[Read]
    clientStorageProbe.reply(ReadResult(Some(client)))

    accessTokenGeneratorProbe.expectMsgType[GenerateAccessToken]

    expectMsgType[Failure]
  }
}
