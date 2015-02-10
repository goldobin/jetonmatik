package jetonmatik.actor.storage

import akka.actor.SupervisorStrategy.{Escalate, Stop}
import akka.actor._

import scala.util.control.NonFatal

trait RelationalStorageWorkerProvider {
  def relationalStorageWorkerProps: Props
}

class RelationalStorage extends Actor with ActorLogging {
  this: RelationalStorageWorkerProvider =>

  import Storage._

  var senderMap: Map[ActorRef, ActorRef] = Map.empty

  def receive: Receive = {
    case Terminated(worker) =>
      context.unwatch(worker)
      senderMap -= worker

    case msg =>
      val worker = context.actorOf(relationalStorageWorkerProps)
      senderMap += worker -> sender()
      context.watch(worker)
      worker forward msg
  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case NonFatal(e) =>
      log.error(e, "Failed to perform DB operation")

      val worker = sender()

      for (originalSender <- senderMap.get(worker)) {
        originalSender ! OperationFailed
      }

      Stop

    case _ => Escalate
  }
}
