package model

import play.api.libs.json.{JsPath, Json, Writes}
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._

case class MessageRequest(channelId: Int,
                          rawContent: String,
                          time: Long,
                          userId: Int,
                          username: String,
                          avatarUrl: String)

case class Link(link: String, title: String)

/**
  * Message exchanged between client and server
  *
  * @param channelId id of channel that message is being sent to
  * @param content   content of message
  * @param username  name of user sending message
  * @param time      time message was sent (millis)
  * @param token     token of user sending message
  * @author jsflax on 3/31/16.
  */
case class MessageResponse(channelId: Int,
                           userId: Int,
                           rawContent: String,
                           mentions: Seq[String],
                           emoticons: Seq[String],
                           links: Seq[Link],
                           strippedContent: String,
                           username: String,
                           avatarUrl: String,
                           time: Long)

object MessageProtocol {
  implicit val lJson = Json.format[Link]
  implicit val mJson = Json.format[MessageRequest]
  implicit val mrJson = Json.format[MessageResponse]
}
