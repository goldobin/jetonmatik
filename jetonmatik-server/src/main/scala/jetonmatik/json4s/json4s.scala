package jetonmatik.json4s

import java.util.UUID

import org.json4s.{DefaultFormats, Formats, CustomSerializer}
import org.json4s.JsonAST.{JNull, JString}
import spray.httpx.Json4sSupport

// TODO: This source should be removed as soon as UUID serializer become a part of
// TODO: release of json4s.
// TODO: https://github.com/json4s/json4s/blob/master/ext/src/main/scala/org/json4s/ext/JavaTypesSerializers.scala


object JavaTypesSerializers {
  val all = List(UUIDSerializer)
}

case object UUIDSerializer extends CustomSerializer[UUID]({
  format => ({
    case JString(s) => UUID.fromString(s)
    case JNull => null
  }, {
    case x: UUID => JString(x.toString)
  })
})

object Implicits extends Json4sSupport {
  implicit def json4sFormats: Formats = DefaultFormats ++ JavaTypesSerializers.all
}