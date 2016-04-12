package model

import play.api.libs.json.Json

/**
  * @author jsflax on 4/5/16.
  */
case class Emoticon(id: Long,
                    shortcut: String,
                    `type`: String,
                    url: String)

object Emoticon {
  implicit val emJs = Json.format[Emoticon]
}
