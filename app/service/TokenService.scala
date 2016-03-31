package service

import java.math.BigInteger
import java.security.SecureRandom

import model.Token
import scalikejdbc._

import scala.util.Random

/**
  * Created by jasonflax on 2/18/16.
  */
object TokenService {
  implicit val session = AutoSession

  private lazy val random = new SecureRandom()
  private lazy val column = Token.column
  private lazy val t = Token.t
  private lazy val oneHourMillis = 3600000L

  val min = 200
  val max = 300

  private def nextSessionId =
    new BigInteger(Random.nextInt(max - min + 1) + min, random).toString(32)

  sql"""
        CREATE TABLE IF NOT EXISTS token (
          id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
          value VARCHAR(255) NOT NULL,
          date_created BIGINT,
          expires BIGINT,
          user_id INT
        )
    """.execute().apply()

  sql"""
        SHOW TRIGGERS LIKE 'token'
    """.map(rs => rs).list.apply().isEmpty match {
    case true =>
      sql"""
       CREATE TRIGGER on_token_created
       BEFORE INSERT ON token
       FOR EACH ROW
       SET
        new.date_created = UNIX_TIMESTAMP(NOW()),
        new.expires = UNIX_TIMESTAMP(NOW()) + 3600000;
      """.execute().apply()
    case _ =>
  }

  def create(userId: Long): String = {
    val token = nextSessionId
    withSQL {
      insert.into(Token).namedValues(
        column.value -> token,
        column.expires -> (System.currentTimeMillis() + oneHourMillis),
        column.userId -> userId
      )
    }.update().apply()
    token
  }

  def findById(tokenId: Long) = withSQL {
    select.from(Token as t).where.eq(t.id, tokenId)
  }.map(rs => Token(rs)).single().apply()

  def findByToken(tokenValue: String) = withSQL {
    select.from(Token as t).where.eq(t.value, tokenValue)
  }.map(rs => Token(rs)).single().apply()

  def findByUserId(userId: Long) = withSQL {
    select.from(Token as t).where.eq(t.userId, userId)
  }.map(rs => Token(rs)).single().apply()

  def updateExpiry(tokenId: Long) = withSQL {
    update.apply(Token).set(
      column.expires -> (System.currentTimeMillis() + oneHourMillis)
    )
  }.update().apply()
}
