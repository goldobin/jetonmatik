package jetonmatik.actor

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import fakes.{Basic, User}
import jetonmatik.model.Client
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.util.Random

class ClientStorageSpec
  extends TestKit(ActorSystem("ClientStorageSpec"))
  with ImplicitSender
  with FlatSpecLike
  with Matchers
  with BeforeAndAfterAll {

  import fakes.Basic._
  import fakes.User._

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  trait Clients {
    val clients = List.fill(10) {
      new Client(
        id = fakeUuid.toString,
        secretHash = fakeHexString(16),
        name = fakeFullName
      )
    }.toSet
  }

  trait ActorUnderTest {
    lazy val preProvisionedClients: Set[Client] = Set.empty

    lazy val actor = TestActorRef(Props(new ClientStorage(preProvisionedClients)))
  }

  import jetonmatik.actor.ClientStorage._

  "ClientStorage ! Read" should "respond with Some if pre provisioned clientId specified" in new ActorUnderTest with Clients {
    override lazy val preProvisionedClients = clients

    for (client <- Random.shuffle(clients)) {
      actor ! LoadClient(client.id)
    }

    val expectedMessages = for (client <- clients) yield ClientLoaded(Some(client))

    expectMsgAllOf(expectedMessages.toArray:_*)
  }

  it should "respond with None if no clients pre provisioned" in new ActorUnderTest {

    actor ! LoadClient(fakeUuid.toString)

    expectMsg(ClientLoaded(None))
  }

  it should "respond with None if wrong clientId specified" in new ActorUnderTest with Clients {
    override lazy val preProvisionedClients = clients

    actor ! LoadClient(fakeUuid.toString)

    expectMsg(ClientLoaded(None))
  }
}
