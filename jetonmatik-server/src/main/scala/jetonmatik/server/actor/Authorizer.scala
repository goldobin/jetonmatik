package jetonmatik.server.actor

import java.io.StringWriter
import java.security.{PublicKey, Key}
import java.time.{Duration, ZoneOffset}

import akka.actor.{Props, ActorRef, Actor, ActorLogging}
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout
import org.bouncycastle.util.io.pem.{PemObject, PemWriter}

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._

object Authorizer {
  case class GenerateToken(
    clientId: String,
    scope: Set[String]) {
    require(clientId.nonEmpty)
  }

  case class Token(
    accessToken: String,
    scope: Set[String],
    expiresIn: Long
  ) {
    require(accessToken.nonEmpty)
    require(expiresIn > 0)
  }

  case object RetrieveFormattedPublicKey

  case class FormattedPublicKey(key: String) {
    require(key.nonEmpty)
  }

  def props(
    publicKey: PublicKey,
    clientStorage: ActorRef,
    accessTokenGenerator: ActorRef) = Props(
    new Authorizer(publicKey, clientStorage, accessTokenGenerator)
      with LocalNowProvider)
}

class Authorizer(
  publicKey: PublicKey,
  clientStorage: ActorRef,
  accessTokenGenerator: ActorRef)
  extends Actor
  with ActorLogging {

  self: NowProvider =>

  import jetonmatik.server.actor.Authorizer._

  val keyPairAlgorithm = "RSA"
  val pemPublicKeyHeader = "RSA PUBLIC KEY"

  val formattedPublicKey =
    formatKey(
      pemPublicKeyHeader,
      publicKey)

  private def formatKey(header: String, key: Key): String = {
    val writer = new StringWriter()
    val pemWriter = new PemWriter(writer)
    pemWriter.writeObject(new PemObject(pemPublicKeyHeader, key.getEncoded))
    pemWriter.flush()
    pemWriter.close()

    writer.toString
  }

  override def receive: Receive = {
    case RetrieveFormattedPublicKey =>
      sender() ! FormattedPublicKey(formattedPublicKey)

    case GenerateToken(clientId, scope) =>

      val originalSender = sender()

      val accessTokenRequestFuture = {

        implicit val timeout = Timeout(10.seconds)

        for {
          ClientStorage.ReadResult(Some(client)) <- clientStorage ? ClientStorage.Read(clientId)
        }
        yield {
          val resultScope = scope.intersect(client.scope)
          val issueTime = instant().atOffset(ZoneOffset.UTC)
          val expirationTime = issueTime.plus(client.tokenTtl)

          AccessTokenGenerator.GenerateAccessToken(
            clientId,
            resultScope,
            issueTime,
            expirationTime)
        }
      }

      val accessTokenFuture = {

        implicit val timeout = Timeout(5.seconds)

        for {
          accessTokenRequest <- accessTokenRequestFuture
          AccessTokenGenerator.AccessToken(accessToken) <- accessTokenGenerator ? accessTokenRequest
        } yield {
          val validityDuration = Duration.between(
            accessTokenRequest.issueTime,
            accessTokenRequest.expirationTime)

          assume(!validityDuration.isNegative)

          Token(
            accessToken = accessToken,
            accessTokenRequest.scope,
            expiresIn = validityDuration.getSeconds
          )
        }
      }

      accessTokenFuture pipeTo originalSender
  }
}
