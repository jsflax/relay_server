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

case class UserCreateRequest(email: String,
                             password: String,
                             name: String)

case class UserLoginRequest(email: String,
                            password: String)

object UserProtocol extends SQLSyntaxSupport[User] {
  implicit val uProtocol = Json.format[User]
  implicit val userCreateRequestProtocol = Json.format[UserCreateRequest]
  implicit val userLoginRequestProtocol = Json.format[UserLoginRequest]

  override val tableName = "user"
  val u = UserProtocol.syntax("user")

  def apply(rs: WrappedResultSet): User = User(
    rs.long(u.resultName.id),
    rs.string(u.resultName.name),
    rs.string(u.resultName.email),
    rs.string(u.resultName.avatarUrl),
    rs.string(u.resultName.hash)
  )
}
