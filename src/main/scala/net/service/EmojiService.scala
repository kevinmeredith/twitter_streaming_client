package net.service

import java.io.File
import io.circe.parser._
import cats.data.{NonEmptyList, Xor}
import io.circe.Json
import net.model.Emoji
import scalaz.concurrent.Task

object EmojiService {

  /**
    * Given a String, find the first Emoji.
    * @param string String input
    * @return First Emoji found or None if none is present
    */
  def exists(string: String): Option[Emoji] = ???

  /**
    * Given a String, find all Emojis.
    * @param string String input
    * @return All Emojis found in the String.
    */
  def findAll(string: String): List[Emoji] = ???


  def read(file: File): Task[NonEmptyList[Emoji]] = readFileToJson(file).flatMap { json =>
    json.as[NonEmptyList[Emoji]] match {
      case Xor.Right(emojis)  => Task.now( emojis )
      case Xor.Left(err) => Task.fail( new RuntimeException(err) )
    }
  }

  private def readFileToJson(file: File): Task[Json] =
    Task.now {
      // credit: http://stackoverflow.com/a/1284446/409976
      val source = scala.io.Source.fromFile (file)
      try source.mkString finally source.close ()
    }.flatMap { str =>
      parse(str) match {
        case Xor.Right(json) => Task.now(json)
        case Xor.Left(err)   => Task.fail(new RuntimeException(err))
      }
    }
}
