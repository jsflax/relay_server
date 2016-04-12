package models

import controllers.ChatActor
import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._

/**
  * Add your spec here.
  * You can mock out a whole application including requests, plugins etc.
  * For more information, consult the wiki.
  */
@RunWith(classOf[JUnitRunner])
class ChatActorSpec extends Specification {
  val chatStringParentheticalRemark =

    "Chat mention parsing" should {
      "return none" in {
        ChatActor.parseMentions("hi foo!")._2 must beEmpty
      }

      "return [@foo]" in {
        ChatActor.parseMentions("hi @foo!")._2 must
          beEqualTo(Seq("@foo"))
      }

      "return [@foo, @bar]" in {
        ChatActor.parseMentions("hi @foo. it's @bar")._2 must
          beEqualTo(Seq("@foo", "@bar"))
      }

      "return [@foo]" in {
        ChatActor.parseMentions("hi @foo, from foo@bar.com")._2 must
          beEqualTo(Seq("@foo"))
      }
    }

  "Chat emoticon parsing" should {
    "return empty" in {
      ChatActor.parseEmoticons("foo bar")._2 must beEmpty
    }
    "return [bar]" in {
      ChatActor.parseEmoticons("foo (bar)")._2 must beEqualTo(Seq("bar"))
    }
    "return [bar, baz]" in {
      ChatActor.parseEmoticons("foo (bar) (baz)")._2 must
        beEqualTo(Seq("bar", "baz"))
    }
    "return [qux]" in {
      ChatActor.parseEmoticons("foo (bar baz) (qux)")._2 must
        beEqualTo(Seq("qux"))
    }
  }

  "Chat string replacement" should {
    val parsableMessage = "hi @foo! it's @bar (baz). how are you? (qux)"
    """return "hi $m0! it's $m1 $e0. how are you? $e1" """ in {
      ChatActor.parseMentions(
        ChatActor.parseEmoticons(parsableMessage)._1
      )._1 must
        beEqualTo("hi $m0! it's $m1 $e0. how are you? $e1")
    }
  }

  "Chat link parsing" should {
    "return The New York Times - Breaking News, World News & Multimedia" in {
      ChatActor.parseLinks(
        "hi foo! check out http://www.nytimes.com/"
      )._2.head.title must beEqualTo(
        "The New York Times - Breaking News, World News & Multimedia"
      )
    }
  }
}
