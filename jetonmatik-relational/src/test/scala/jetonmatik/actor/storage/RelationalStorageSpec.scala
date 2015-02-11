package jetonmatik.actor.storage

import java.io.IOException
import java.sql.SQLException

import akka.actor.Actor.Receive
import akka.actor.{Status, Actor, Props, ActorSystem}
import akka.testkit._
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

class RelationalStorageSpec extends TestKit(ActorSystem("AuthorizerSpec"))
  with ImplicitSender
  with FlatSpecLike
  with Matchers
  with BeforeAndAfterAll {

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  trait ActorUnderTest {
    lazy val workerProbe = TestProbe()
    lazy val workerProps = TestProps(workerProbe)

    lazy val actor = TestActorRef(Props(
      new RelationalStorage with RelationalStorageWorkerProvider {
        lazy val relationalStorageWorkerProps: Props = workerProps
      }
    ))
  }

  "RelationalStorage" should "forward messages to worker" in new ActorUnderTest {
    actor ! "Request Message"

    workerProbe.expectMsg("Request Message")
    workerProbe.reply("Response Message")

    expectMsg("Response Message")
  }

  it should "respond with failure if Exception occurred in worker" in new ActorUnderTest {

    val e = new Exception()

    override lazy val workerProps = Props(new Actor {
      def receive: Receive = {
        case _ => throw e
      }
    })

    actor ! "Request Message"
    expectMsg(Storage.OperationFailed)
  }

}
