package model

import play.api.libs.json.Json
import scalikejdbc._
import scalikejdbc.WrappedResultSet

/**
  * Datum for users that must create accounts
  * to access the API
  *
  * @param id unique ID of user
  * @param name username
  * @param email email user signs up with
  * @param hash password hash
  *
  * @author jsflax on 3/30/16.
  */
case class User(id: Long,
                name: String,
                email: String,
                avatarUrl: String,
                hash: String)

object User extends SQLSyntaxSupport[User] {
  override val tableName = "user"
  val u = User.syntax("user")

  def apply(rs: WrappedResultSet): User = new User(
    rs.long(u.resultName.id),
    rs.string(u.resultName.name),
    rs.string(u.resultName.email),
    rs.string(u.resultName.avatarUrl),
    rs.string(u.resultName.hash)
  )
}

object UserProtocol {
  implicit val uProtocol = Json.format[User]
}

case class UserRequest(email: String, password: String, name: Option[String])

object UserRequestProtocol {
  implicit val userRequestProtocol = Json.format[UserRequest]
}
