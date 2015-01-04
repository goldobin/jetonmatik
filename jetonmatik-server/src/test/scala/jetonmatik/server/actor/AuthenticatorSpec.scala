package jetonmatik.server.actor

import akka.actor.{Props, ActorSystem}
import akka.testkit._
import jetonmatik.server.model.Client
import jetonmatik.util.PasswordHash
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, FlatSpecLike}

class AuthenticatorSpec
  extends TestKit(ActorSystem("AuthenticatorSpec"))
  with ImplicitSender
  with FlatSpecLike
  with Matchers
  with MockitoSugar
  with BeforeAndAfterAll {


  import fakes.Basic._
  import fakes.User._

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  trait ClientData {
    val clientId = fakeUuid.toString
    val clientSecret = fakePassword

    val clientSecretHash = PasswordHash.createHash(clientSecret)
  }

  trait ActorUnderTest {
    val clientStorageProbe = TestProbe()

    lazy val actor = TestActorRef(
      Props(new Authenticator(clientStorageProbe.ref)))
  }

  import Authenticator._
  import ClientStorage._

  "Authenticator" should "authenticate existing client if secret is correct" in new ActorUnderTest with ClientData {

    actor ! Authenticate(clientId, clientSecret)

    clientStorageProbe.expectMsg(Read(clientId))
    clientStorageProbe.reply(ReadResult(Some(Client(
      clientId = clientId,
      clientSecretHash = clientSecretHash
    ))))

    expectMsg(Authentication(authenticated = true))
  }

  it should "not authenticate existing client if secret is incorrect" in new ActorUnderTest with ClientData {
    actor ! Authenticate(clientId, clientSecret)

    clientStorageProbe.expectMsg(Read(clientId))
    clientStorageProbe.reply(ReadResult(Some(Client(
      clientId = clientId,
      clientSecretHash = fakePassword
    ))))

    expectMsg(Authentication(authenticated = false))
  }

  it should "not authenticate not existing client" in new ActorUnderTest with ClientData {
    actor ! Authenticate(clientId, clientSecret)

    clientStorageProbe.expectMsg(Read(clientId))
    clientStorageProbe.reply(ReadResult(None))

    expectMsg(Authentication(authenticated = false))
  }

  ignore should "not authenticate if storage timed out to respond" in new ActorUnderTest with ClientData {

    actor ! Authenticate(clientId, clientSecret)

    clientStorageProbe.expectMsg(Read(clientId))

    expectMsg(Authentication(authenticated = false))
  }

}
