package jetonmatik.actor

import akka.actor.{ActorSystem, Props}
import akka.testkit._
import fakes.{Basic, User}
import jetonmatik.model.{Client, ClientCredentials}
import jetonmatik.util.PasswordHash
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._

class AuthenticatorSpec
  extends TestKit(ActorSystem("AuthenticatorSpec"))
  with ImplicitSender
  with FlatSpecLike
  with Matchers
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
    val timeout = 1.second

    lazy val actor = TestActorRef(Props(
      new Authenticator with AuthenticationWorkerProvider {
        lazy val authenticationWorkerProps: Props =
          Props(new AuthenticationWorker(clientStorageProbe.ref, 1.second))
      }
    ))
  }

  import jetonmatik.actor.Authenticator._
  import jetonmatik.actor.ClientStorage._

  "Authenticator" should "authenticate existing client if secret is correct" in new ActorUnderTest with ClientData {

    val clientOption = Some(Client(
      id = clientId,
      secretHash = clientSecretHash
    ))

    actor ! Authenticate(ClientCredentials(clientId, clientSecret))

    clientStorageProbe.expectMsg(LoadClient(clientId))
    clientStorageProbe.reply(ClientLoaded(clientOption))

    expectMsg(Authenticated(clientOption))
  }

  it should "not authenticate existing client if secret is incorrect" in new ActorUnderTest with ClientData {

    actor ! Authenticate(ClientCredentials(clientId, clientSecret))

    clientStorageProbe.expectMsg(LoadClient(clientId))
    clientStorageProbe.reply(ClientLoaded(Some(Client(
      id = clientId,
      secretHash = "some incorrect secret hash"
    ))))

    expectMsg(Authenticated(None))
  }

  it should "not authenticate not existing client" in new ActorUnderTest with ClientData {
    actor ! Authenticate(ClientCredentials(clientId, clientSecret))

    clientStorageProbe.expectMsg(LoadClient(clientId))
    clientStorageProbe.reply(ClientLoaded(None))

    expectMsg(Authenticated(None))
  }

  it should "not authenticate if storage timed out to respond" in new ActorUnderTest with ClientData {
    override val timeout: FiniteDuration = 1.milli

    actor ! Authenticate(ClientCredentials(clientId, clientSecret))

    clientStorageProbe.expectMsg(LoadClient(clientId))

    expectMsg(FailedToAuthenticate)
  }

}
