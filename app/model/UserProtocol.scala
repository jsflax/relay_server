package model

import scalikejdbc._
import scalikejdbc.WrappedResultSet

import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Datum for users that must create accounts
  * to access the API
  *
  * @param id        unique ID of user
  * @param name      username
  * @param email     email user signs up with
  * @param avatarUrl url of avatar
  * @param token     password hash
  *
  * @author jsflax on 3/30/16.
  */
case class User(id: Long,
                name: String,
                email: String,
                avatarUrl: String,
                hash: String,
                token: String)

case class UserCreateRequest(email: String,
                             password: String,
                             name: String)

case class UserLoginRequest(email: String,
                            password: String)

object UserProtocol extends SQLSyntaxSupport[User] {
  implicit val uProtocolWrites = (
    (JsPath \ 'id).write[Long] and
    (JsPath \ 'name).write[String] and
    (JsPath \ 'email).write[String] and
    (JsPath \ 'avatarUrl).write[String] and
    (JsPath \ 'token).write[String]
  ) (unlift { (user: User) =>
    Some(user.id, user.name, user.email, user.avatarUrl, user.token)
  })

  implicit val userCreateRequestProtocol = Json.format[UserCreateRequest]
  implicit val userLoginRequestProtocol = Json.format[UserLoginRequest]

  override val tableName = "user"
  val u = UserProtocol.syntax("user")

  def apply(rs: WrappedResultSet): User = User(
    rs.long(u.resultName.id),
    rs.string(u.resultName.name),
    rs.string(u.resultName.email),
    rs.string(u.resultName.avatarUrl),
    rs.string(u.resultName.hash),
    rs.string(Token.t.resultName.value)
  )
}
