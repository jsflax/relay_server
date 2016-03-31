package model

import play.api.libs.json.Json
import scalikejdbc._
import data.Channel

object ChannelProtocol extends SQLSyntaxSupport[Channel] {
  implicit val uProtocol = Json.format[Channel]

  override val tableName = "channel"
  val c = ChannelProtocol.syntax("channel")

  def apply(rs: WrappedResultSet): Channel = new Channel(
    rs.long(c.resultName.id),
    rs.long(c.resultName.creatorId),
    rs.string(c.resultName.name),
    rs.long(c.resultName.dateCreated),
    rs.string(c.resultName.description)
  )
}
