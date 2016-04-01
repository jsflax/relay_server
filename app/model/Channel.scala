package model

import play.api.libs.json.Json
import scalikejdbc._

case class Channel(id: Int,
                   creatorId: Int,
                   name: String,
                   dateCreated: Long,
                   description: String)

case class ChannelCreateRequest(creatorId: Int,
                                name: String,
                                description: String)

object ChannelProtocol extends SQLSyntaxSupport[Channel] {
  implicit val cJson = Json.format[Channel]
  implicit val ccrJson = Json.format[ChannelCreateRequest]

  override val tableName = "channel"

  val c = ChannelProtocol.syntax("channel")

  def apply(rs: WrappedResultSet): Channel =
    Channel(
      rs int c.id,
      rs int c.creatorId,
      rs string c.name,
      rs long c.dateCreated,
      rs string c.description
    )
}
