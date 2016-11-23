package net.model

import io.circe._
import cats.data.{NonEmptyList, Xor}

// See https://github.com/iamcal/emoji-data#using-the-data
// for the spec/protocol that this [[net.model.Emoji]] follows.
sealed abstract case class Emoji(name: Option[String], codePoints: NonEmptyList[Int]) {
  val codePointsSize: Int = 1 + codePoints.tail.size
  val list: List[Int]     = codePoints.head :: codePoints.tail
}
object Emoji {

  import cats.implicits._

  def fromString(name: Option[String], input: String): Option[Emoji] =
    input.split("-").toList match {
      case uni @ _ :: _  => convertToCodePoints(uni).map { str => new Emoji(name, str) {} }
      case _             => None
  }

  // "The Unicode codepoint, as 4-5 hex digits" (https://github.com/iamcal/emoji-data)
  /**
    * Given a List of possible Emoji's Code Points, return an optional String
    * representation of the emoji code points.
    *
    * Uses https://docs.oracle.com/javase/8/docs/api/java/lang/String.html#String-int:A-int-int-.
    */
   def convertToCodePoints(hex: List[String]): Option[NonEmptyList[Int]] = {
     val result = hex.foldRight[Option[List[Int]]](Some(Nil)) { (elem, acc) =>
       for {
         a  <- acc
         _  <- if (elem.length == 4 || elem.length == 5) Some(elem) else None
         cp <- stringToCodePointValue(elem)
       } yield cp :: a
     }
     result.flatMap {
       case Nil          => None
       case head :: tail => Some( NonEmptyList(head, tail) )
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
