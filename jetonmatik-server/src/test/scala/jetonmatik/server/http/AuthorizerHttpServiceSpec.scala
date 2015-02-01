package jetonmatik.server.http

import akka.actor.{Actor, ActorRef, Props}
import akka.testkit.TestProbe
import fakes.{OAuth, Text, User, Basic}
import jetonmatik.actor.{Authorizer, Authenticator}
import jetonmatik.model.{ClientCredentials, Client}
import jetonmatik.provider.FormattedPublicKeyProvider
import Authenticator.{Authenticated, Authenticate}
import Authorizer.{Authorized, Authorize}
import jetonmatik.util.{PasswordHash, Bytes}
import org.scalatest.{Matchers, FlatSpec}
import spray.http.{FormData, BasicHttpCredentials}
import spray.http.HttpHeaders.Authorization
import spray.routing.{AuthenticationFailedRejection, ValidationRejection, HttpService}
import spray.testkit.ScalatestRouteTest
import spray.http.StatusCodes._


class AuthorizerHttpServiceSpec
  extends FlatSpec
  with Matchers
  with ScalatestRouteTest
  with AuthorizerHttpService
  with HttpService
  with FormattedPublicKeyProvider {

  import Basic._
  import User._
  import Text._
  import OAuth._

  override val formattedPublicKey: String = "Some formatted Public Key"

  def actorRefFactory = system
  override val realm: String = fakeHexString(2048)

  trait AuthData {
    val clientId = fakeUuid.toString
    val clientSecret = fakePassword
    
    val clientCredentials = ClientCredentials(clientId, clientSecret)

    private val httpCredentialsBase64Encoded = Bytes.toBase64String(
      (clientId + ":" + clientSecret).getBytes("UTF-8"))
    val httpCredentials = BasicHttpCredentials(clientId, clientSecret)
  }

  trait ClientData extends AuthData {
    lazy val client = Client(
      id = clientId,
      secretHash = PasswordHash.createHash(clientSecret)
    )
  }

  trait TokenData {
    val invalidGrantType = fakeWord + "_"
    val validGrantType = "client_credentials"

    val scope = fakeScopeSet()
    val scopeString = scope.mkString(" ")

    val scope2 = fakeScopeSet()
    val scopeString2 = scope.mkString(" ")

    val token = fakeAccessToken
    val expiresIn = fakeAccessTokenTtl.getSeconds
  }

  trait RouteUnderTestWithProbes {
    val authenticatorProbe = TestProbe()
    val authorizerProbe = TestProbe()

    lazy val route = authorizerRoute(authenticatorProbe.ref, authorizerProbe.ref)
  }

  trait RouteUnderTest extends ClientData {
    val authenticator = system.actorOf(Props(new Actor {
      import Authenticator._

      override def receive: Receive = {
        case Authenticate(_) => sender() ! Authenticated(Some(client))
      }
    }))

    val authorizer = system.actorOf(Props(new Actor {
      import Authorizer._

      override def receive: Receive = {
        case Authorize(_, scope) => sender() ! Authorized(
          fakeAccessToken,
          scope,
          fakeAccessTokenTtl.getSeconds)
      }
    }))

    lazy val route = authorizerRoute(authenticator, authorizer)
  }

  "AuthorizerHttpService's public-key route" should "produce plain text public key" in {

    val authenticator = TestProbe().ref
    val authorizer = TestProbe().ref

    val route = authorizerRoute(authenticator, authorizer)

    Get("/public-key") ~> route ~> check {
      status shouldBe OK
      responseAs[String] should be (formattedPublicKey)
    }
  }

  "AuthorizerHttpService's token route" should "produce token" in
    new RouteUnderTest with AuthData with TokenData {

    val tokenRequestData = FormData(
      Map(
        "grant_type" -> validGrantType,
        "scope" -> scopeString
      ))

    Post("/token", tokenRequestData) ~>
      addHeader(Authorization(httpCredentials)) ~>
      route ~> check {
      status shouldBe OK
    }
  }

  it should "reject if not authenticated" in new RouteUnderTest with TokenData with AuthData {
    override val authenticator: ActorRef = system.actorOf(Props(new Actor {
      import Authenticator._

      override def receive: Receive = {
        case Authenticate(_) => sender() ! Authenticated(None)
      }
    }))

    val tokenRequestData = FormData(
      Map(
        "grant_type" -> validGrantType
      ))

    Post("/token", tokenRequestData) ~>
      addHeader(Authorization(httpCredentials)) ~>
      route ~> check {
      rejection shouldBe an [AuthenticationFailedRejection]
    }
  }

  it should "reject if grant_type is not `client_credentials`" in
    new RouteUnderTest with TokenData with AuthData {
      val tokenRequestData = FormData(
        Map(
          "grant_type" -> invalidGrantType
        ))

      Post("/token", tokenRequestData) ~>
        addHeader(Authorization(httpCredentials)) ~>
        route ~> check {
        rejection shouldBe a [ValidationRejection]
      }
    }


  ignore should "accept GET and pass client credentials to underlying actors" in
    new RouteUnderTestWithProbes with TokenData with ClientData {

      Get(s"/token?grant_type=$validGrantType") ~>
        addHeader(Authorization(httpCredentials)) ~>
        route

      import Authenticator._
      import Authorizer._

      authenticatorProbe.expectMsg(Authenticate(clientCredentials))
      authenticatorProbe.reply(Authenticated(Some(client)))

      authorizerProbe.expectMsg(Authorize(client, Set.empty))
      authorizerProbe.reply(Authorized(token, scope2, expiresIn))
    }

  it should "accept POST and pass client credentials to underlying actor" in
    new RouteUnderTestWithProbes with TokenData with ClientData {

      Post(
        "/token",
        FormData(Map(
          "grant_type" -> validGrantType
        ))) ~>
        addHeader(Authorization(httpCredentials)) ~>
        route

      authenticatorProbe.expectMsg(Authenticate(clientCredentials))
      authenticatorProbe.reply(Authenticated(Some(client)))

      authorizerProbe.expectMsg(Authorize(client, Set.empty))
      authorizerProbe.reply(Authorized(token, scope2, expiresIn))
    }

  ignore should "accept GET and pass scope to underlying actor" in
    new RouteUnderTestWithProbes with TokenData with ClientData {

      Get(s"/token?grant_type=$validGrantType&scope=${scopeString.mkString("%20")}") ~>
        addHeader(Authorization(httpCredentials)) ~>
        route

      import Authenticator._
      import Authorizer._

      authenticatorProbe.expectMsg(Authenticate(clientCredentials))
      authenticatorProbe.reply(Authenticated(Some(client)))

      authorizerProbe.expectMsg(Authorize(client, scope))
      authorizerProbe.reply(Authorized(token, scope2, expiresIn))
    }

  it should "accept POST and pass scope to underlying actor" in
    new RouteUnderTestWithProbes with TokenData with ClientData {

      Post(
        "/token",
        FormData(Map(
          "grant_type" -> validGrantType,
          "scope" -> scopeString
        ))) ~>
        addHeader(Authorization(httpCredentials)) ~>
        route

      import Authenticator._
      import Authorizer._

      authenticatorProbe.expectMsg(Authenticate(clientCredentials))
      authenticatorProbe.reply(Authenticated(Some(client)))

      authorizerProbe.expectMsg(Authorize(client, scope))
      authorizerProbe.reply(Authorized(token, scope2, expiresIn))
    }

  ignore should "accept GET and pass empty scope to underlying actor if scope is not specified" in
    new RouteUnderTestWithProbes with TokenData with ClientData {

      Get(s"/token?grant_type=client_credentials") ~>
        addHeader(Authorization(httpCredentials)) ~>
        route

      import Authenticator._
      import Authorizer._

      authenticatorProbe.expectMsgType[Authenticate]
      authenticatorProbe.reply(Authenticated(Some(client)))

      authorizerProbe.expectMsg(Authorize(client, Set.empty))
      authorizerProbe.reply(Authorized(token, scope2, expiresIn))
    }

  it should "accept POST and pass empty scope to underlying actor if scope is not specified" in
    new RouteUnderTestWithProbes with TokenData with ClientData {

      Post(
        "/token",
        FormData(Map(
          "grant_type" -> validGrantType
        ))) ~>
        addHeader(Authorization(httpCredentials)) ~>
        route

      import Authenticator._
      import Authorizer._

      authenticatorProbe.expectMsgType[Authenticate]
      authenticatorProbe.reply(Authenticated(Some(client)))

      authorizerProbe.expectMsg(Authorize(client, Set.empty))
      authorizerProbe.reply(Authorized(token, scope2, expiresIn))
    }
}
