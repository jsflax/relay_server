package model

import play.api.libs.json.Json
import scalikejdbc._

/**
  * Expiring token granted to a user to allow temporary access to the API.
  * Will be refreshed upon use. Tokens only expire after not being used
  * for 1 hour
  *
  * @param id auto-incrementing token key
  * @param value actual token value
  * @param dateCreated date created (millis since epoch)
  * @param expires timestamp that token expires
  * @param userId unique user ID that this token is associated with
  * @author jsflax on 3/30/16.
  */
case class Token(id: Long,
                 value: String,
                 dateCreated: Long,
                 expires: Long,
                 userId: Long) {
  def isExpired = System.currentTimeMillis > expires
}

case class SimpleUuid(uuid: String)

object Token extends SQLSyntaxSupport[Token] {
  implicit val tsProtocol = Json.format[SimpleUuid]

  override val tableName = "token"

  lazy val t = Token.syntax("token")

  def apply(rs: WrappedResultSet): Token = new Token(
    rs.long(t.resultName.id),
    rs.string(t.resultName.value),
    rs.long(t.resultName.dateCreated),
    rs.long(t.resultName.expires),
    rs.long(t.resultName.userId)
  )
}
