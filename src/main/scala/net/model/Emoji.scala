package net.model

import io.circe._
import cats.data.Xor

// See https://github.com/iamcal/emoji-data#using-the-data
// for the spec/protocol that this [[net.model.Emoji]] follows.
sealed abstract case class Emoji(name: Option[String], unicode: String)
object Emoji {

  import cats.implicits._

  def fromString(name: Option[String], input: String): Option[Emoji] = {
    def strToUnicode(xs: List[String]): Option[String] = {
      val result = xs.map(unicode)
      result.traverse(identity).map(_.mkString)
    }

    input.split("-").toList match {
      case uni@ _ :: _  => strToUnicode(uni).map { unicodes =>  new Emoji(name, unicodes) {}}
      case _            => None
    }
  }

  import scala.util.Try

  // "The Unicode codepoint, as 4-5 hex digits" (https://github.com/iamcal/emoji-data)
  private def unicode(x: String): Option[String] = {
    if(x.length == 4 || x.length == 5) {
      Try { Integer.parseInt(x, 16) }.map(_.toChar.toString).toOption
    }
    else {
      None
    }
  }


  implicit val emojiDecoder = new Decoder[Emoji] {
    override def apply(hCursor: HCursor): Decoder.Result[Emoji] = {
      val result = for {
        name    <- hCursor.downField("name").as[Option[String]]
        unicode <- hCursor.downField("unified").as[String]
      } yield (name, unicode)
      result.flatMap {
        case (name, unified) => Emoji.fromString(name, unified) match {
          case Some(emoji) => Xor.right(emoji)
          case None        => Xor.left(DecodingFailure("Invalid 'unified' field", Nil))
        }
      }
    }

  }

}
