package jetonmatik.actor

import akka.actor.{Actor, ActorLogging, Props}
import jetonmatik.model.Client


object ClientStorage {
  case class LoadClient(clientId: String)
  case class ClientLoaded(clientOption: Option[Client])
  case object FailedToLoadClient

  case class SaveClient(client: Client)
  case object ClientSaved
  case object FailedToSaveClient

  def props(predefinedClients: Set[Client]) = Props(new ClientStorage(predefinedClients))
}

class ClientStorage(clients: Set[Client]) extends Actor with ActorLogging {

  import jetonmatik.actor.ClientStorage._

  val predefinedClientsMap = (clients map { c => c.id -> c}).toMap

  override def receive: Receive = {
    case LoadClient(clientId) => sender() ! ClientLoaded(predefinedClientsMap.get(clientId))
    case SaveClient(_) =>
      log.warning("ClientStorage WriteClient message received. Operation is not supported at the moment")
  }
}



