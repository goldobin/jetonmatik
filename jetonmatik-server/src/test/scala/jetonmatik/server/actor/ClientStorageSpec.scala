package jetonmatik.server.actor
import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import jetonmatik.server.model.Client
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, FlatSpecLike}

import scala.util.Random

class ClientStorageSpec
  extends TestKit(ActorSystem("ClientStorageSpec"))
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

  trait Clients {
    val clients = List.fill(10) {
      new Client(
        clientId = fakeUuid.toString,
        clientSecretHash = fakeHexString(16),
        name = fakeFullName
      )
    }.toSet
  }

  trait ActorUnderTest {
    lazy val preProvisionedClients: Set[Client] = Set.empty

    lazy val actor = TestActorRef(Props(new ClientStorage(preProvisionedClients)))
  }

  import ClientStorage._

  "ClientStorage ! Read" should "respond with Some if pre provisioned clientId specified" in new ActorUnderTest with Clients {
    override lazy val preProvisionedClients = clients

    for (client <- Random.shuffle(clients)) {
      actor ! Read(client.clientId)
    }

    val expectedMessages = for (client <- clients) yield ReadResult(Some(client))

    expectMsgAllOf(expectedMessages.toArray:_*)
  }

  it should "respond with None if no clients pre provisioned" in new ActorUnderTest {

    actor ! Read(fakeUuid.toString)

    expectMsg(ReadResult(None))
  }

  it should "respond with None if wrong clientId specified" in new ActorUnderTest with Clients {
    override lazy val preProvisionedClients = clients

    actor ! Read(fakeUuid.toString)

    expectMsg(ReadResult(None))
  }
}
