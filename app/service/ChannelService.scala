package service

import model._
import scalikejdbc._

/**
  * Created by jasonflax on 2/18/16.
  */
object ChannelService {
  implicit val session = AutoSession

  private lazy val column = ChannelProtocol.column
  private lazy val c = ChannelProtocol.c

  sql"""
        CREATE TABLE IF NOT EXISTS channel (
          id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
          creator_id INT NOT NULL,
          name VARCHAR(255) NOT NULL,
          date_created BIGINT,
          description TEXT
        )
    """.execute().apply()

  sql"""
        CREATE TABLE IF NOT EXISTS channel_user_admins (
          id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
          channel_id INT,
          user_id INT
        )
    """.execute().apply()

  sql"""
        SHOW TRIGGERS LIKE 'channel'
    """.map(rs => rs).list.apply().isEmpty match {
    case true =>
      sql"""
       CREATE TRIGGER on_channel_created
       BEFORE INSERT ON channel
       FOR EACH ROW
       SET
        new.date_created = UNIX_TIMESTAMP(NOW())
      """.execute().apply()
    case _ =>
  }

  def create(channel: ChannelCreateRequest): ServiceResponse[Long] = {
    TokenService.findByToken(channel.token) match {
      case Some(user) =>
        ServiceResponse[Long](
          StatusCode.OK,
          withSQL {
            insert.into(ChannelProtocol).namedValues(
              column.name -> channel.name,
              column.creatorId -> user.id,
              column.description -> channel.description
            )
          }.updateAndReturnGeneratedKey().apply()
        )
      case None =>
        ServiceResponse[Long](
          StatusCode.Unauthorized,
          message = "user not found",
          data = null
        )
    }
  }

  def read(channelId: Long): ServiceResponse[Channel] = {
    val channelOpt = withSQL {
      select.from(ChannelProtocol as c).where.eq(c.id, channelId)
    }.map(rs => ChannelProtocol(rs)).single().apply()

    channelOpt match {
      case Some(channel) =>
        ServiceResponse(
          StatusCode.OK,
          channel
        )
      case None =>
        ServiceResponse[Channel](
          StatusCode.ResourceNotFound
        )
    }
  }

  def readAll(): ServiceResponse[List[Channel]] =
    ServiceResponse(
      StatusCode.OK,
      withSQL {
        select.from(ChannelProtocol as c)
      }.map(rs => ChannelProtocol(rs)).list().apply()
    )
}
