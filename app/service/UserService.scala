package service

import java.io.File

import model.{ServiceResponse, StatusCode, User, UserRequest}
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

  val column = User.column
  val u = User.u

  sql"""
      CREATE TABLE IF NOT EXISTS user (
        id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        name VARCHAR(30) NOT NULL,
        email VARCHAR(50) UNIQUE NOT NULL,
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
    new File("./public/images")
  ).filter(
    _.getName.startsWith("dart_monkey")
  ).map(name => s"img/$name")


  def create(user: UserRequest): ServiceResponse[Option[String]] = {
    withSQL {
      select.from(User as u).where.eq(u.id, user.email)
    }.map(rs => User(rs)).single().apply() match {
      case Some(_) =>
        ServiceResponse[Option[String]](
          StatusCode.Unauthorized,
          message = Messages("user.exists.error")
        )
      case _ =>
        ServiceResponse(
          StatusCode.OK,
          withSQL {
            insert
              .into(User)
              .namedValues(
                column.name -> user.name.orNull,
                column.email -> user.email,
                column.avatarUrl -> defaultAvatars(
                  new Random().nextInt(defaultAvatars.length)
                ),
                column.hash -> BCrypt.hashpw(
                  user.password, BCrypt.gensalt()
                )
              )
          }.updateAndReturnGeneratedKey().apply() match {
            case l if l > 0 => Some(TokenService.create(l))
            case _ => None
          }
        )
    }
  }

  def read(userId: Long): User = withSQL {
    select.from(User as u).where.eq(u.id, userId)
  }.map(rs => User(rs)).single().apply().orNull

  def readByToken(token: String): ServiceResponse[User] =
    TokenService.findByToken(token) match {
      case Some(value) =>
        ServiceResponse(
          StatusCode.OK,
          read(value.userId)
        )
      case None =>
        ServiceResponse[User](
          StatusCode.ResourceNotFound,
          message = Messages("user.does.not.exist.error")
        )
    }

  def readByEmailAndPassword(params: UserRequest): ServiceResponse[User] =
    withSQL {
      select
        .from(User as u)
        .where.eq(u.email, params.email)
    }.map(rs => User(rs)).single().apply() match {
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
          val updateImpl = update(User)
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
