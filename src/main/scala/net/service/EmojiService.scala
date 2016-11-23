package net.service

import java.io.File
import io.circe.parser._
import cats.data.Xor
import io.circe.Json
import net.model.Emoji
import scalaz.concurrent.Task

object EmojiService {

  /**
    * Given an [[net.model.Emoji]] and a String, return the input `emoji` for the number
    * of occurrences that it shows up in the `input` String.
    * @param emoji Single Emoji
    * @param input String input
    * @return [[List[net.model.Emoji]] for each separate emoji occurrence in the `input`.
    */
  def findAllEmojiInstances(emoji: Emoji, input: String): List[Emoji] =
    input.codePoints.toArray.toList.sliding(emoji.codePointsSize).toList.flatMap { window =>
      if(window == emoji.list) List(emoji) else Nil
    }

  /**
    * Given a String, find all present Emojis, including duplicates.
    * @param emojis Non-empty list of emojis
    * @param string String input
    * @return All Emojis found in the String.
    */
  def findAll(emojis: List[Emoji], string: String): List[Emoji] =
    emojis.map(findAllEmojiInstances(_, string)).flatten
  
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
