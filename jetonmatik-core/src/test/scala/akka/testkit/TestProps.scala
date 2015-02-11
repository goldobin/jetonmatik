package akka.testkit

import akka.actor.{Actor, ActorRef, Props}

object TestProps {

  class ProbeProxy(ref: ActorRef) extends Actor {
    def receive: Receive = {
      case message => ref forward message
    }
  }

  def apply(probe: TestProbe) = Props(new ProbeProxy(probe.ref))
  def apply(ref: ActorRef) = Props(new ProbeProxy(ref))
}
