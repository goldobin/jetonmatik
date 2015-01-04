package jetonmatik.server.actor

import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{OneForOneStrategy, Actor, ActorRefFactory, Props}
import akka.routing.RoundRobinPool
import jetonmatik.server.http.AuthorizerHttpService
import jetonmatik.server.ServerSettings
import jetonmatik.server.service.{PemFormattedPublicKeyProvider, YamlClientsProvider}
import jetonmatik.util.KeyStoreFactory

object RouteListener {
  def props(settings: ServerSettings) = Props(new RouteListener(settings))
}

class RouteListener(settings: ServerSettings)
  extends Actor
  with AuthorizerHttpService
  with PemFormattedPublicKeyProvider {

  override def actorRefFactory: ActorRefFactory = context
  override val realm: String = settings.realm

  val signatureKeyPairAlias = settings.signatureKeyPairAlias
  val signaturePrivateKeyPassword = settings.keyStore.keys.get(signatureKeyPairAlias) match {
    case Some(password) => password
    case None => throw new IllegalStateException(s"There is no password for key '$signatureKeyPairAlias'")
  }

  val keyStore = KeyStoreFactory(settings.keyStore.path)
    .createKeyStore(settings.keyStore.password)

  override val publicKeyAlgorithm = "RSA"
  override val publicKey = keyStore
    .obtainPublicKey(signatureKeyPairAlias)
    .asInstanceOf[RSAPublicKey]

  val privateKey = keyStore
    .obtainKey(signatureKeyPairAlias, signaturePrivateKeyPassword)
    .asInstanceOf[RSAPrivateKey]

  val clientStorage = settings.clientsFile match {
    case Some(path) =>
      context.actorOf(
        ClientStorage.props(YamlClientsProvider(path).obtainClients()),
        "client-storage")
    case None =>
      throw new AssertionError("Clients file should be defined")
  }

  val accessTokenGenerator = context.actorOf(
    AccessTokenGenerator
      .props(
        privateKey,
        issuer = settings.externalUrl)
      .withRouter(RoundRobinPool(
        nrOfInstances = settings.tokenGeneratorPoolSize,
        supervisorStrategy = OneForOneStrategy() {
          case _ =>  Restart
        })),
    "access-token-generator")

  val authorizer = context.actorOf(
    Authorizer.props(clientStorage, accessTokenGenerator),
    "authorizer")

  val authenticator = context.actorOf(
    Authenticator.props(clientStorage),
    "authenticator")

  override def receive: Receive = runRoute(authorizerRoute(authenticator, authorizer))
}
