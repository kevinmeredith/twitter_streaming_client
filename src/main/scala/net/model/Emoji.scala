package net.model

import io.circe._
import cats.data.Xor

// See https://github.com/iamcal/emoji-data#using-the-data
// for the spec/protocol that this [[net.model.Emoji]] follows.
sealed abstract case class Emoji(name: Option[String], value: String)
object Emoji {

  import cats.implicits._

  def fromString(name: Option[String], input: String): Option[Emoji] =
    input.split("-").toList match {
      case uni @ _ :: _  => codePointsToString(uni).map { value =>  new Emoji(name, value) {}}
      case _             => None
  }

  // "The Unicode codepoint, as 4-5 hex digits" (https://github.com/iamcal/emoji-data)
  /**
    * Given a List of possible Emoji's Code Points, return an optional String
    * representation of the emoji code points.
    *
    * Uses https://docs.oracle.com/javase/8/docs/api/java/lang/String.html#String-int:A-int-int-.
    */
   def codePointsToString(x: List[String]): Option[String] = {
      if(x.length == 4 || x.length == 5) {
        val codePointsOrNothing = x.map(stringToCodePointValue).sequence
        codePointsOrNothing.map { cps =>
          new String(cps.toArray, 0, cps.length)
        }
      }
      else {
        None
      }
    }

  import scala.util.Try

  private def stringToCodePointValue(x: String): Option[Int] =
    Try { Integer.parseInt(x, 16) }.toOption

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
