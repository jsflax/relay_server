package controllers

import java.util.concurrent.{Executors, TimeUnit}
import javax.inject.{Inject, Singleton}

import akka.actor.{Actor, ActorRef, Props}
import model._
import play.api.Logger
import play.api.libs.json.{JsArray, JsNumber, JsObject, Json}
import play.api.mvc.{Action, WebSocket}
import play.api.libs.streams._
import model.MessageProtocol._

import scala.collection.mutable.ListBuffer
import akka.actor._
import akka.stream.Materializer
import akka.util.Timeout
import play.api.libs.ws.{WS, WSClient}

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.language.postfixOps
import scala.concurrent.duration._

/**
  * @author jsflax on 3/31/16.
  */
object ChatActor {
  val actorMap = mutable.Map[String, ActorRef]()
  val channelActorMap = mutable.Map[Long, ListBuffer[ActorRef]]()

  def generateMessageResponse(messageRequest: MessageRequest):
  MessageResponse = {

    val (rawStrippedMentions, mentions) =
      parseMentions(messageRequest.rawContent)

    val (rawStrippedMentionsAndEmoticons, emoticons) =
      parseEmoticons(rawStrippedMentions)

    val (rawStrippedFinal, links) =
      parseLinks(rawStrippedMentionsAndEmoticons)

    MessageResponse(
      messageRequest.channelId,
      messageRequest.userId,
      messageRequest.rawContent,
      mentions,
      emoticons,
      links,
      rawStrippedFinal,
      messageRequest.username,
      messageRequest.avatarUrl,
      messageRequest.time
    )
  }

  def parseMentions(rawContent: String): (String, Seq[String]) = {
    // (?<![^\W\d_]) Assert that there is no letter before the current position
    // @ Match at symbol
    // \\w+ match til non-word
    val mentionRegex = "(?<![^\\W\\d_])@\\w+".r

    var idx = -1
    val buffer = ListBuffer[String]()

    (mentionRegex.replaceAllIn(rawContent, r => {
      buffer += r.matched
      idx += 1
      s"\\$$m$idx"
    }), buffer)
  }

  def parseEmoticons(rawContent: String): (String, Seq[String]) = {
    // Match all text between two parentheses that are not broken by non-words
    val emoticonRegex = "\\(([\\w+\\)]*)\\)".r

    var idx = -1
    val buffer = ListBuffer[String]()

    (emoticonRegex.replaceAllIn(rawContent, r => {
      buffer += r.group(1)
      idx += 1
      s"\\$$e$idx"
    }), buffer)
  }

  def parseHtmlTitle(sUrl: String): String = {
    val titleRegex = "<title>(.*?)</title>".r

    titleRegex.findFirstMatchIn(
      scala.io.Source.fromURL(sUrl).mkString
    ).get.group(1)
  }

  def parseLinks(rawContent: String): (String, Seq[Link]) = {
    val linkRegex = "(https?:\\/\\/(?:www\\.|(?!www))[^\\s\\.]+\\.[^\\s]{2,}|www\\.[^\\s]+\\.[^\\s]{2,})".r

    var idx = -1
    val buffer = ListBuffer[Link]()

    (linkRegex.replaceAllIn(rawContent, r => {
      buffer += Link(r.matched, parseHtmlTitle(r.matched))
      idx += 1
      s"\\$$l$idx"
    }), buffer)
  }
}

class ChatActor(out: ActorRef) extends Actor {

  def receive = {
    case msg: MessageResponse =>
      Logger.warn("passing response")
      out ! Json.toJson(msg).toString()
    case msg: String =>
      Logger.error("channel map: " + ChatActor.channelActorMap)

      Logger.error(self.path.toString)

      val json = Json.parse(msg)

      Logger.info("msg 1: " + json.toString())

      val req = json.as[MessageRequest]

      ChatActor.channelActorMap(req.channelId).foreach(
        _ ! Json.toJson(ChatActor.generateMessageResponse(req)).toString()
      )
  }
}

case class Create(p: Props, uuid: String)

@Singleton
class MessageController @Inject()
(implicit system: ActorSystem, materializer: Materializer, ws: WSClient)
  extends BaseController {

  var emotes: Seq[Emoticon] = Seq()

  materializer.schedulePeriodically(0.millis, 7.days, new Runnable {
    override def run(): Unit = {
      ws.url(
        "https://api.hipchat.com/v2/emoticon?" +
          "start-index=0&max-results=1000" +
          "&auth_token=UBcOSFWxlCceJXzRBqsi87AK8XLoLPcGiMa4TPCr"
      ).get().map { response =>
        emotes = Json.parse(response.body)
          .as[JsObject].value("items").as[JsArray].value.map { emoticon =>
          emoticon.as[Emoticon]
        }
      }
    }
  })

  implicit val executionContext = new ExecutionContext {
    val threadPool = Executors.newFixedThreadPool(1000)

    def execute(runnable: Runnable) {
      threadPool.submit(runnable)
    }

    def reportFailure(t: Throwable) {}
  }

  implicit val timeout = Timeout(5000, TimeUnit.MILLISECONDS)

  def subscribe(channelId: Long, uuid: String) = {
    val userResult = ChatActor.actorMap.get(uuid)
    userResult match {
      case Some(ref) =>
        Logger.info(s"successfully subscribed $uuid")
        ChatActor.channelActorMap.foreach { p =>
          p._2.find(_.equals(ref)) match {
            case Some(oldRef) => p._2 -= oldRef
            case _ =>
          }
        }

        ChatActor.channelActorMap.getOrElseUpdate(
          channelId, ListBuffer()
        ) += ref
        renderServiceResponse[String](
          ServiceResponse(StatusCode.OK)
        )
      case None =>
        Ok(
          Json.obj(
            "status" -> StatusCode.ImproperParameters,
            "message" -> "user not connected"
          )
        )
    }
  }

  def emoticons = Action {
      Ok(
        JsObject(Seq(
          "status" -> JsNumber(200),
          "data" -> Json.toJson(emotes)
        ))
      )
  }

  /**
    * Core call for connecting to a socket.
    *
    * @return
    */
  def chat(uuid: String) =
    WebSocket.accept[String, String] { request =>
      Logger.debug(s"Accepted! $uuid")

      ActorFlow.actorRef(out => {
            ChatActor.actorMap(uuid) = out
            Props(new ChatActor(out))
      })
    }
}
