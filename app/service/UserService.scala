package service

import java.io.File

import model._
import play.api.i18n.Messages
import scalikejdbc._

import scala.annotation.tailrec
import util.BCrypt

import scala.util.Random
import model.UserProtocol._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

/**
  * @author jsflax on 3/30/16.
  */
object UserService {
  implicit val session = AutoSession

  val column = UserProtocol.column
  val u = UserProtocol.u
  val t = Token.t

  sql"""
      CREATE TABLE IF NOT EXISTS user (
        id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        name VARCHAR(50) UNIQUE NOT NULL,
        date_created BIGINT,
        avatar_url VARCHAR(255),
        hash VARCHAR(255) NOT NULL
      )
  """.execute().apply()

  sql"""
        SHOW TRIGGERS LIKE 'user'
    """.map(rs => rs).list.apply().isEmpty match {
    case true =>
      sql"""
     CREATE TRIGGER on_user_created
     BEFORE INSERT ON user
     FOR EACH ROW
     SET
      new.date_created = UNIX_TIMESTAMP(NOW());
  """.execute().apply()
    case _ =>
  }

  /**
    * Traverse all files in view directory using tail recursion.
    *
    * @param file top directory
    * @return list of all files
    */
  private def listFiles(file: File): List[File] = {
    @tailrec def listFiles(files: List[File], result: List[File]): List[File] =
      files match {
        case Nil => result
        case head :: tail if head.isDirectory =>
          listFiles(Option(head.listFiles).map(
            _.toList ::: tail
          ).getOrElse(tail), result)
        case head :: tail if head.isFile =>
          listFiles(tail, head :: result)
      }
    listFiles(List(file), Nil)
  }

  lazy val defaultAvatars = listFiles(
    new File("./public/images/avatars")
  ).map(name => s"/assets/avatars/${name.getName}")


  def create(user: UserCreateRequest): ServiceResponse[Option[User]] = {
    withSQL {
      select.from(UserProtocol as u).where.eq(u.name, user.name)
    }.map(rs => UserProtocol(rs)).single().apply() match {
      case Some(_) =>
        ServiceResponse[Option[User]](
          StatusCode.Unauthorized,
          message = Messages("user.exists.error")
        )
      case _ =>
        val avatarUrl = defaultAvatars(
          new Random().nextInt(defaultAvatars.length)
        )
        val pwHash = BCrypt.hashpw(
          user.password, BCrypt.gensalt()
        )
        ServiceResponse(
          StatusCode.OK,
          withSQL {
            insert
              .into(UserProtocol)
              .namedValues(
                column.name -> user.name,
                column.avatarUrl -> user.avatarUrl.getOrElse(avatarUrl),
                column.hash -> pwHash
              )
          }.updateAndReturnGeneratedKey().apply() match {
            case l if l > 0 =>
              Some(
                User(
                  l,
                  user.name,
                  user.avatarUrl.getOrElse(avatarUrl),
                  pwHash,
                  TokenService.create(l)
                )
              )
            case _ => None
          }
        )
    }
  }

  private def read(userId: Long): User = withSQL {
    select.from(UserProtocol as u)
      .join(Token as t)
      .where.eq(u.id, userId)
      .and.eq(t.userId, userId)
  }.map(rs => UserProtocol(rs)).single().apply().orNull

  def readByToken(token: String): ServiceResponse[User] =
    TokenService.findByToken(token) match {
      case Some(value) =>
        ServiceResponse(
          StatusCode.OK,
          value
        )
      case None =>
        ServiceResponse[User](
          StatusCode.ResourceNotFound,
          message = Messages("user.does.not.exist.error")
        )
    }

  def readByNameAndPassword(params: UserLoginRequest): ServiceResponse[User] =
    withSQL {
      select
        .from(UserProtocol as u)
        .join(Token as t)
        .where.eq(u.name, params.name)
        .and.eq(u.id, t.userId)
    }.map(rs => UserProtocol(rs)).single().apply() match {
      case Some(user) =>
        if (BCrypt.checkpw(params.password, user.hash)) {
          ServiceResponse(
            StatusCode.OK,
            user
          )
        } else {
          ServiceResponse[User](
            StatusCode.Unauthorized,
            message = Messages("user.invalid.password")
          )
        }
      case _ =>
        ServiceResponse[User](
          StatusCode.ResourceNotFound,
          message = Messages("user.does.not.exist.error")
        )
    }

  def modify(token: String, fields: (String, Any)*): Boolean = {
    val userResp = readByToken(token)
    userResp.statusCode match {
      case StatusCode.OK =>
        withSQL {
          val updateImpl = update(UserProtocol as u)
          fields.foldLeft(updateImpl) {
            (query, field) =>
              query.set(column.column(field._1) -> field._2)
          }.where.eq(u.id, userResp.data.id)
        }.update().apply()
        true
      case _ => false
    }
  }
}
