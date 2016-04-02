package model

import play.api.libs.json.Json
import scalikejdbc._

case class Channel(id: Int,
                   creatorId: Int,
                   name: String,
                   dateCreated: Long,
                   description: String)

case class ChannelSubscribeRequest(token: String)

case class ChannelCreateRequest(token: String,
                                name: String,
                                description: String)

object ChannelProtocol extends SQLSyntaxSupport[Channel] {
  implicit val cJson = Json.format[Channel]
  implicit val ccrJson = Json.format[ChannelCreateRequest]

  override val tableName = "channel"

  val c = ChannelProtocol.syntax("channel")

  def apply(rs: WrappedResultSet): Channel =
    Channel(
      rs int c.resultName.id,
      rs int c.resultName.creatorId,
      rs string c.resultName.name,
      rs long c.resultName.dateCreated,
      rs string c.resultName.description
    )
}
