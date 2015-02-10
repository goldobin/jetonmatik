package jetonmatik.actor.storage

import jetonmatik.model.Client

object ClientStorage {
  case class LoadClient(clientId: String)
  case class ClientLoaded(clientOption: Option[Client])

  case class SaveClient(client: Client)
  case object ClientSaved
}