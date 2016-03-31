package model

import data.{User, UserCreateRequest, UserLoginRequest}
import play.api.libs.json.Json
import scalikejdbc._
import scalikejdbc.WrappedResultSet


object UserProtocol extends SQLSyntaxSupport[User] {
  implicit val uProtocol = Json.format[User]
  implicit val userCreateRequestProtocol = Json.format[UserCreateRequest]
  implicit val userLoginRequestProtocol = Json.format[UserLoginRequest]

  override val tableName = "user"
  val u = UserProtocol.syntax("user")

  def apply(rs: WrappedResultSet): User = new User(
    rs.long(u.resultName.id),
    rs.string(u.resultName.name),
    rs.string(u.resultName.email),
    rs.string(u.resultName.avatarUrl),
    rs.string(u.resultName.hash)
  )
}
