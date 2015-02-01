package jetonmatik.actor

import java.time.{Duration, ZoneOffset}

import akka.actor._
import jetonmatik.model.Client
import jetonmatik.provider.{LocalNowProvider, NowProvider}

import scala.concurrent.duration.{FiniteDuration, _}

object Authorizer {
  case class Authorize(
    client: Client,
    scope: Set[String])

  case class Authorized(
    accessToken: String,
    scope: Set[String],
    expiresIn: Long
  ) {
    require(accessToken.nonEmpty)
    require(expiresIn > 0)
  }

  case object FailedToAuthorize

  def props(accessTokenGenerator: ActorRef) = Props(
    new Authorizer with AuthorizationWorkerPropsProvider {
      lazy val authorizationWorkerProps: Props = AuthorizationWorker.props(accessTokenGenerator)
    }
  )
}

class Authorizer
  extends Actor
  with ActorLogging {

  this: AuthorizationWorkerPropsProvider =>

  import jetonmatik.actor.Authorizer._

  override def receive: Receive = {
    case msg: Authorize =>
      val worker = context.actorOf(authorizationWorkerProps)
      worker forward msg
  }
}

trait AuthorizationWorkerPropsProvider {
  def authorizationWorkerProps: Props
}

object AuthorizationWorker {
  val GenerationTimeout: FiniteDuration = 1.minute

  case object TokenGenerationTimedOut

  def props(accessTokenGenerator: ActorRef) = Props(
    new AuthorizationWorker(accessTokenGenerator, GenerationTimeout) with LocalNowProvider
  )
}

class AuthorizationWorker(
  accessTokenGenerator: ActorRef,
  timeout: FiniteDuration)
  extends Actor
  with ActorLogging {

  this: NowProvider =>

  import jetonmatik.actor.AccessTokenGenerator._
  import jetonmatik.actor.AuthorizationWorker._
  import jetonmatik.actor.Authorizer._

  override def receive: Receive = {
    case Authorize(client, requestedScope) =>
      context.become(generateAccessToken(client, requestedScope, sender()))
  }

  def generateAccessToken(
    client: Client,
    requestedScope: Set[String],
    originalSender: ActorRef): Receive = {

    import context.dispatcher
    val timeoutCancelable = context.system.scheduler.scheduleOnce(timeout, self, TokenGenerationTimedOut)

    val resultScope = requestedScope.intersect(client.scope)
    val issueTime = instant().atOffset(ZoneOffset.UTC)
    val expirationTime = issueTime.plus(client.tokenTtl)

    accessTokenGenerator ! AccessTokenGenerator.Generate(
      client.id,
      resultScope,
      issueTime,
      expirationTime
    )

    {
      case Generated(accessToken) =>
        timeoutCancelable.cancel()

        val validityDuration = Duration.between(
          issueTime,
          expirationTime)

        assume(!validityDuration.isNegative)

        originalSender ! Authorized(
          accessToken = accessToken,
          resultScope,
          expiresIn = validityDuration.getSeconds
        )

        log.debug(s"Access token for client ${client.id} successfully generated with scope ${resultScope.mkString(",")}")
        stop()

      case TokenGenerationTimedOut =>
        originalSender ! FailedToAuthorize
        log.error(s"Access token for client ${client.id} is failed to be generated within $timeout")

        stop()
    }
  }

  def stop(): Unit = context.stop(self)
}

