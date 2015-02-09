package jetonmatik.actor.storage

import akka.actor.{Actor, ActorLogging, Props}
import jetonmatik.model.Client

object MemoryClientStorage {
  def props(predefinedClients: Set[Client]) = Props(new MemoryClientStorage(predefinedClients))
}

class MemoryClientStorage(clients: Set[Client]) extends Actor with ActorLogging {

  import jetonmatik.actor.storage.ClientStorage._

  val predefinedClientsMap = (clients map { c => c.id -> c}).toMap

  override def receive: Receive = {
    case LoadClient(clientId) => sender() ! ClientLoaded(predefinedClientsMap.get(clientId))
    case SaveClient(_) =>
      log.warning("Save client operation is not supported")
      sender() ! StorageFailure
  }
}