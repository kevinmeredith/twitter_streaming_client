package net.model

import io.circe._
import io.circe.generic.semiauto._
import scalaz.concurrent.Task

// See https://github.com/iamcal/emoji-data#using-the-data
// for the spec/protocol that this [[net.model.Emoji]] follows.
case class Emoji(name: Option[String], unified: String) {
  val unicode: List[String] = unified.split("-").toList
}
object Emoji {
  implicit val emojiDecoder: Decoder[Emoji] = deriveDecoder[Emoji]

  def asIntNel(e: Emoji): Task[List[Int]] =
    e.unicode.foldLeft[Task[List[Int]]](Task.now(Nil)){ (acc, elem) =>
      for {
        a <- acc
        e <- unicodeToInt(elem)
      } yield e :: a
    }

  // Given a String, i.e. assumed to be a Hex without a `0x` prefix,
  // attempt to convert it to an Int value.
  private def unicodeToInt(s: String): Task[Int] = {
    val hexPrefix = "0x"
    Task { Integer.decode(s"$hexPrefix$s") }
  }

}
