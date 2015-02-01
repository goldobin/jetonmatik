package jetonmatik.actor

import akka.actor._
import jetonmatik.model.{Client, ClientCredentials}
import jetonmatik.util.PasswordHash

import scala.concurrent.duration._
import scala.util.control.NonFatal

object Authenticator {
  case class Authenticate(credentials: ClientCredentials)
  case class Authenticated(clientOption: Option[Client])
  case object FailedToAuthenticate

  def props(clientStorage: ActorRef): Props = Props(new Authenticator with AuthenticationWorkerProvider {
      lazy val authenticationWorkerProps = AuthenticationWorker.props(clientStorage)
    }
  )
}

class Authenticator extends Actor with ActorLogging {
  this: AuthenticationWorkerProvider =>

  import jetonmatik.actor.Authenticator._

  override def receive: Receive = {
    case msg: Authenticate =>
      val retriever = context.actorOf(authenticationWorkerProps)
      retriever forward msg
  }
}

trait AuthenticationWorkerProvider {
  def authenticationWorkerProps: Props
}

protected object AuthenticationWorker {
  val LoadTimeout: FiniteDuration = 1.minute

  protected case object ClientRetrievalTimedOut

  def props(clientStorage: ActorRef) = Props(new AuthenticationWorker(clientStorage, LoadTimeout))
}

case class AuthenticationWorker(
  clientStorage: ActorRef,
  timeout: FiniteDuration)
  extends Actor
  with ActorLogging {

  import jetonmatik.actor.AuthenticationWorker._
  import jetonmatik.actor.Authenticator._
  import jetonmatik.actor.ClientStorage._

  override def receive: Receive = {
    case Authenticate(credentials) => context.become(loadAndValidateClient(credentials, sender()))
  }

  private def loadAndValidateClient(credentials: ClientCredentials, originalSender: ActorRef): Receive = {
    clientStorage ! LoadClient(credentials.id)

    import context.dispatcher
    val timeoutCancelable = context.system.scheduler.scheduleOnce(timeout, self, ClientRetrievalTimedOut)

    {
      case ClientLoaded(clientOption) =>
        timeoutCancelable.cancel()

        val validatedClientOption = clientOption filter { client =>
          try {
            PasswordHash.validatePassword(credentials.secret, client.secretHash)
          } catch {
            case NonFatal(e) =>
              log.warning(s"Can't validate client secret hash for Client(id=${client.id})")
              false
          }
        }

        originalSender ! Authenticator.Authenticated(validatedClientOption)

        log.debug(s"The client ${credentials.id} successfully authenticated")
        stop()

      case FailedToLoadClient =>
        timeoutCancelable.cancel()

        originalSender ! Authenticator.FailedToAuthenticate

        log.error(s"The client ${credentials.id} is failed to be loaded due to storage error")
        stop()

      case ClientRetrievalTimedOut =>
        originalSender ! Authenticator.FailedToAuthenticate

        log.error(s"The client ${credentials.id} is failed to be loaded within $timeout")
        stop()
    }
  }

  private def stop() = context.stop(self)
}