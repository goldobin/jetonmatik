package jetonmatik.server.actor

import akka.actor.{Props, ActorLogging, Actor}
import jetonmatik.server.model.Client


object ClientStorage {
  case class Read(clientId: String)
  case class ReadResult(clientOption: Option[Client])

  case class WriteClient(client: Client)
  case object WriteAck

  def props(predefinedClients: Set[Client]) = Props(new ClientStorage(predefinedClients))
}

class ClientStorage(clients: Set[Client]) extends Actor with ActorLogging {

  import ClientStorage._

  val predefinedClientsMap = (clients map { c => c.clientId -> c}).toMap

  override def receive: Receive = {
    case Read(clientId) => sender() ! ReadResult(predefinedClientsMap.get(clientId))
    case WriteClient =>
      log.warning("ClientStorage WriteClient message received. Operation is not supported at the moment")
  }
}



