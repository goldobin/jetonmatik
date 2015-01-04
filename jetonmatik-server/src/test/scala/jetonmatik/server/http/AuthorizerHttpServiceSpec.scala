package jetonmatik.server.http

import akka.actor.{Actor, ActorRef, Props}
import akka.testkit.TestProbe
import jetonmatik.server.actor.Authenticator.{Authentication, Authenticate}
import jetonmatik.server.actor.Authorizer.{Token, GenerateToken}
import jetonmatik.server.actor.{Authenticator, Authorizer}
import jetonmatik.util.Bytes
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
  with HttpService {

  import fakes.Basic._
  import fakes.User._
  import fakes.Text._
  import fakes.OAuth._

  def actorRefFactory = system
  override val realm: String = "Test Backend"

  trait AuthData {
    val clientId = fakeUuid.toString
    val clientSecret = fakePassword

    private val credentialsBase64 = Bytes.toBase64String(
      (clientId + ":" + clientSecret).getBytes("UTF-8"))

    val credentials = BasicHttpCredentials(clientId, clientSecret)
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

  trait RouteUnderTest {
    val authenticator = system.actorOf(Props(new Actor {
      import Authenticator._

      override def receive: Receive = {
        case Authenticate(_, _) => sender() ! Authentication(authenticated = true)
      }
    }))

    val authorizer = system.actorOf(Props(new Actor {
      import Authorizer._

      override def receive: Receive = {
        case GenerateToken(_, scope) => sender() ! Token(
          fakeAccessToken,
          scope,
          fakeAccessTokenTtl.getSeconds)
      }
    }))

    lazy val route = authorizerRoute(authenticator, authorizer)
  }

  "AuthorizerHttpService's public-key route" should "produce plain text public key" in {

    val publicKeyText = fakeHexString(2048)

    val authenticator = TestProbe().ref
    val authorizer = system.actorOf(Props(new Actor {
      import Authorizer._

      override def receive: Receive = {
        case RetrieveFormattedPublicKey => sender() ! FormattedPublicKey(publicKeyText)
      }
    }))

    val route = authorizerRoute(authenticator, authorizer)

    Get("/public-key") ~> route ~> check {
      status shouldBe OK
      responseAs[String] should be (publicKeyText)
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
      addHeader(Authorization(credentials)) ~>
      route ~> check {
      status shouldBe OK
    }
  }

  it should "reject if not authenticated" in new RouteUnderTest with TokenData with AuthData {
    override val authenticator: ActorRef = system.actorOf(Props(new Actor {
      import Authenticator._

      override def receive: Receive = {
        case Authenticate(_, _) => sender() ! Authentication(authenticated = false)
      }
    }))

    val tokenRequestData = FormData(
      Map(
        "grant_type" -> validGrantType
      ))

    Post("/token", tokenRequestData) ~>
      addHeader(Authorization(credentials)) ~>
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
        addHeader(Authorization(credentials)) ~>
        route ~> check {
        rejection shouldBe a [ValidationRejection]
      }
    }


  ignore should "accept GET and pass client credentials to underlying actor" in
    new RouteUnderTestWithProbes with TokenData with AuthData {

      Get(s"/token?grant_type=$validGrantType") ~>
        addHeader(Authorization(credentials)) ~>
        route

      import Authenticator._
      import Authorizer._

      authenticatorProbe.expectMsg(Authenticate(clientId, clientSecret))
      authenticatorProbe.reply(Authentication(authenticated = true))

      authorizerProbe.expectMsgType[GenerateToken]
      authorizerProbe.reply(Token(token, scope2, expiresIn))
    }

  it should "accept POST and pass client credentials to underlying actor" in
    new RouteUnderTestWithProbes with TokenData with AuthData {

      Post(
        "/token",
        FormData(Map(
          "grant_type" -> validGrantType
        ))) ~>
        addHeader(Authorization(credentials)) ~>
        route

      authenticatorProbe.expectMsg(Authenticate(clientId, clientSecret))
      authenticatorProbe.reply(Authentication(authenticated = true))

      authorizerProbe.expectMsgType[GenerateToken]
      authorizerProbe.reply(Token(token, scope2, expiresIn))
    }

  ignore should "accept GET and pass scope to underlying actor" in
    new RouteUnderTestWithProbes with TokenData with AuthData {

      Get(s"/token?grant_type=$validGrantType&scope=${scopeString.mkString("%20")}") ~>
        addHeader(Authorization(credentials)) ~>
        route

      import Authenticator._
      import Authorizer._

      authenticatorProbe.expectMsgType[Authenticate]
      authenticatorProbe.reply(Authentication(authenticated = true))

      authorizerProbe.expectMsg(GenerateToken(clientId, scope))
      authorizerProbe.reply(Token(token, scope2, expiresIn))
    }

  it should "accept POST and pass scope to underlying actor" in
    new RouteUnderTestWithProbes with TokenData with AuthData {

      Post(
        "/token",
        FormData(Map(
          "grant_type" -> validGrantType,
          "scope" -> scopeString
        ))) ~>
        addHeader(Authorization(credentials)) ~>
        route

      import Authenticator._
      import Authorizer._

      authenticatorProbe.expectMsgType[Authenticate]
      authenticatorProbe.reply(Authentication(authenticated = true))

      authorizerProbe.expectMsg(GenerateToken(clientId, scope))
      authorizerProbe.reply(Token(token, scope2, expiresIn))
    }

  ignore should "accept GET and pass empty scope to underlying actor if scope is not specified" in
    new RouteUnderTestWithProbes with TokenData with AuthData {

      Get(s"/token?grant_type=client_credentials") ~>
        addHeader(Authorization(credentials)) ~>
        route

      import Authenticator._
      import Authorizer._

      authenticatorProbe.expectMsgType[Authenticate]
      authenticatorProbe.reply(Authentication(authenticated = true))

      authorizerProbe.expectMsg(GenerateToken(clientId, Set.empty))
      authorizerProbe.reply(Token(token, scope2, expiresIn))
    }

  it should "accept POST and pass empty scope to underlying actor if scope is not specified" in
    new RouteUnderTestWithProbes with TokenData with AuthData {

      Post(
        "/token",
        FormData(Map(
          "grant_type" -> validGrantType
        ))) ~>
        addHeader(Authorization(credentials)) ~>
        route

      import Authenticator._
      import Authorizer._

      authenticatorProbe.expectMsgType[Authenticate]
      authenticatorProbe.reply(Authentication(authenticated = true))

      authorizerProbe.expectMsg(GenerateToken(clientId, Set.empty))
      authorizerProbe.reply(Token(token, scope2, expiresIn))
    }
}
