package jetonmatik.actor.storage

import akka.actor.Actor

abstract class RelationalStorageWorker extends Actor {

  def receiveStorageOperation: Receive

  def receive: Receive = receiveStorageOperation andThen { _ =>
    context.stop(self)
  }
}
