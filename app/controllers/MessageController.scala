package controllers

import java.net.URL

import akka.actor.{Actor, ActorRef}
import model.{Link, MessageRequest, MessageResponse}
import play.api.libs.json.JsValue

import scala.collection.mutable
import scala.collection.mutable.ListBuffer


/**
  * @author jsflax on 3/31/16.
  */
object ChatActor {
  def generateMessageResponse(messageRequest: MessageRequest):
    MessageResponse = {

    val (rawStrippedMentions, mentions) =
      parseMentions(messageRequest.rawContent)

    val (rawStrippedMentionsAndEmoticons, emoticons) =
      parseEmoticons(rawStrippedMentions)

    MessageResponse(
      messageRequest.channelId,
      messageRequest.rawContent,
      mentions,
      emoticons,
      Seq(),
      rawStrippedMentionsAndEmoticons,
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
    val emoticonRegex = "\\(([\\w+\\)]+)\\)".r

    var idx = -1
    val buffer = ListBuffer[String]()

    (emoticonRegex.replaceAllIn(rawContent, r => {
      buffer += r.matched
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

class ChatActor(out: ActorRef,
                dispatcher: (MessageResponse) => Unit) extends Actor {


  def receive = {
    case msg: JsValue =>

  }
}

class MessageController extends BaseController {
  val channelActorMap = mutable.Map[Long, ChatActor]()
}
