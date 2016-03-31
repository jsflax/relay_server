package model

import play.api.libs.json.Json
import scalikejdbc._

/**
  * Message channel that allows the exchanging of message between
  * multiple users.
  *
  * @param id unique ID of channel
  * @param name name of channel
  * @param dateCreated date channel was created (millis since epoch)
  * @param description channel description
  */
case class Channel(id: Long,
                   creatorId: Long,
                   name: String,
                   dateCreated: Long,
                   description: String)

object Channel extends SQLSyntaxSupport[Channel] {
  override val tableName = "channel"
  val c = Channel.syntax("channel")

  def apply(rs: WrappedResultSet): Channel = new Channel(
    rs.long(c.resultName.id),
    rs.long(c.resultName.creatorId),
    rs.string(c.resultName.name),
    rs.long(c.resultName.dateCreated),
    rs.string(c.resultName.description)
  )
}

object ChannelProtocol {
  implicit val uProtocol = Json.format[Channel]
}
