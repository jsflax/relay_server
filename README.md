# relay_server
Hipchat-inspired relay chat backend.

## Message parsing
Raw messages are received from the client application and parsed based on the
content.

There are three special content types.

### Mentions

A way to mention a user. Always starts with an '@' and ends when hitting a 
non-word character.

```scala
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

```

### Emoticons

Custom emoticons which are alphanumeric strings, no longer than 15 
characters, contained in parenthesis.

```scala
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
```

### Links

Any URLs contained in the message, along with the page's title.

```scala
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
```

### All together now

Content is then generated using the following method.

```scala
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
```

It is rendered as such.

```json
{
	"channelId":1,
	"userId":4,
	"rawContent":"hi @tim (android)",
	"mentions":["tim"],
	"emoticons":["android"],
	"links":[],
	"strippedContent":"hi $m0 $e0",
	"username":"zod",
	"avatarUrl":"/assets/avatars/penguin.png",
	"time":1461818833138
}
```
