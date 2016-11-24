package net.model

import java.io.File

import cats.data.NonEmptyList
import net.service.EmojiService
import org.scalatest.FlatSpec

object EmojiServiceSpec {
  // Given a [[cats.data.NonEmptyList]] of Int, where each
  // element represents a Code Point, return its String representation.
  def codePointsToString(codePoints: NonEmptyList[Int]): String = {
    val cps = codePoints.head :: codePoints.tail
    new String(cps.toArray, 0, cps.length)
  }
}

class EmojiServiceSpec extends FlatSpec {

  import EmojiServiceSpec.codePointsToString

  "Reading the emoji-shortened.json file" should "produce a valid List of [[net.model.Emoji]] of length 4's." in {
    val url      = this.getClass().getResource("/emoji-shortened.json")
    val file     = new File(url.toURI)
    val result   = EmojiService.read(file).unsafePerformSync
    val expected1 = Emoji.fromString(Some( "COPYRIGHT SIGN" ),  "00A9" ).get
    val expected2 = Emoji.fromString(Some( "REGIONAL INDICATOR SYMBOL LETTERS ZM" ), "1F1FF-1F1F2" ).get
    assert(result.size == 2)
    assert(result.contains(expected1))
    assert(result.contains(expected2))
  }

  "Reading the emoji.json, i.e. the full Emoji file" should "decode > 100 emojis successfully" in {
    val url      = this.getClass().getResource("/emoji.json")
    val file     = new File(url.toURI)
    val result   = EmojiService.read(file).unsafePerformSync
    assert(result.size > 100)
  }

  "Checking for the existence of an Emoji" should "succeed if a single unicode emoji present" in {
    val exclamation = Emoji.fromString(Some( "exclamation" ),  "0021" ).get
    val input       = "foo bar bippy - ends with exclamation point \u0021"
    assert( EmojiService.findAllEmojiInstances(exclamation, input) == List(exclamation) )
  }

  import Emoji.convertToCodePoints

  "Checking for the existence of an Emoji" should "succeed if a size 2 unicode emoji present" in {
    val fake       = Emoji.fromString(Some( "fake" ),  "0021-0022" ).get
    val codePoints = convertToCodePoints(List("0021", "0022")).map(codePointsToString).get
    val input      = s"$codePoints  blah blah blah lasdfasdf."
    assert( EmojiService.findAllEmojiInstances(fake, input) == List(fake) )
  }

  "Checking for the existence of an Emoji" should "fail if there's no emoji present in the input String" in {
    val absent = Emoji.fromString(Some( "fake" ),  "0021-0022" ).get
    val input = "no unicode to see here..... blah blah blah"
    assert( EmojiService.findAllEmojiInstances(absent, input).isEmpty )
  }

  "Finding all emojis in an input String" should "find all input emojis" in {
    val exclamation             = Emoji.fromString(Some( "exclamation" ),  "0021" ).get
    val emoji                   = Emoji.fromString(Some( "fake" ),  "0044-0045" ).get
    val emojiCodePointsAsString = convertToCodePoints(List("0044", "0045")).map(codePointsToString).get
    val input = s"hello \u0021 world $emojiCodePointsAsString"
    assert(EmojiService.findAll(List(exclamation, emoji), input) == List(exclamation, emoji))
  }

  "Finding all emojis in an input String" should "find all input emojis with one of size 2, length 5" in {
    val exclamation          = Emoji.fromString(Some( "exclamation" ),  "0021" ).get
    val emoji                = Emoji.fromString(Some( "fake" ),  "aaabc-aaabd" ).get
    val emojiCodePointString = convertToCodePoints(List("aaabc", "aaabd")).map(codePointsToString).get
    val input = s"hello \u0021 world $emojiCodePointString"
    assert(EmojiService.findAll(List(exclamation, emoji), input) == List(exclamation, emoji))
  }

  "Finding all emojis in an input String" should "find all but 1 input emojis" in {
    val exclamation    = Emoji.fromString(Some( "exclamation" ),  "0021" ).get
    val fake           = Emoji.fromString(Some( "fake" ),  "0021-0022" ).get
    val input = s"hello \u0021 world how's it going"
    assert(EmojiService.findAll(List(exclamation, fake), input) == List(exclamation))
  }

  "Finding all emojis in an input String" should "find the 'fake' emoji" in {
    val fake         = Emoji.fromString(Some( "fake" ),  "0021-0022" ).get
    val codePointStr = convertToCodePoints(List("0021", "0022")).map(codePointsToString).get
    val input = s"hello \u0021 world $codePointStr"
    assert(EmojiService.findAll(List(fake), input) == List(fake))
  }

  "Finding all emojis in an input String" should "find two instances of the same emoji if it shows up twice" in {
    val fake      = Emoji.fromString(Some( "fake2" ),  "0021-0023" ).get
    val input = "hello \u0021\u0023 world \u0021\u0023"
    assert(EmojiService.findAll(List(fake), input) == List(fake, fake))
  }

  "Finding all emojis in an input String" should "find no instances since the code points were reversed" in {
    val fake      = Emoji.fromString(Some( "fake2" ),  "0021-0023" ).get
    val input = "hello world \u0023\u0021"
    assert(EmojiService.findAll(List(fake), input) == Nil)
  }

  "Finding all emojis in an input String" should "find no instances since the code points were separated by a space" in {
    val fake      = Emoji.fromString(Some( "fake2" ),  "0021-0023" ).get
    val input = "hello world \u0023 \u0021"
    assert(EmojiService.findAll(List(fake), input) == Nil)
  }

  "Finding all emojis in an input String" should "find an emoji having 2 5-length hex codes" in {
    val emoji              = Emoji.fromString(Some( "REGIONAL INDICATOR SYMBOL LETTERS ZM" ), "1F1FF-1F1F2" ).get
    val codePointStr       = convertToCodePoints(List("1F1FF", "1F1F2")).map(codePointsToString).get
    val secondCodePointStr = convertToCodePoints(List("1F1F2")).map(codePointsToString).get
    val input = s"$codePointStr, hi world $secondCodePointStr it's me, $codePointStr"
    assert(EmojiService.findAll(List(emoji), input) == List(emoji, emoji))
  }

}
