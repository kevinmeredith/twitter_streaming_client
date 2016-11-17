package net.service

import java.io.File
import io.circe.parser._
import cats.data.Xor
import io.circe.Json
import net.model.Emoji
import scalaz.concurrent.Task

object EmojiService {

  /**
    * Given a String, find the first Emoji.
    * @param emoji Single Emoji
    * @param input String input
    * @return First Emoji found or None if none is present
    */
  def exists(emoji: Emoji, input: String): Option[Emoji] =
    if(input.contains(emoji.unicode)) Some(emoji) else None

  /**
    * Given a String, find all Emojis.
    * @param emojis Non-empty list of emojis
    * @param string String input
    * @return All Emojis found in the String.
    */
  def findAll(emojis: List[Emoji], string: String): List[Emoji] =
    emojis.flatMap { e =>
      exists(e, string)
    }
  
  /**
    * Given a [[java.io.File]], return a [[scalaz.concurrent.Task]]-wrapped Non-empty list of emojis
    * that were extracted from the File.
    * @param file File from which to extract emojis
    * @return [[scalaz.concurrent.Task]]-wrapped Non-empty list of emojis
    */
  def read(file: File): Task[List[Emoji]] = readFileToJson(file).flatMap { json =>
    json.as[List[Emoji]] match {
      case Xor.Right(emojis) => Task.now( emojis )
      case Xor.Left(err)     => Task.fail( new RuntimeException(err) )
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
