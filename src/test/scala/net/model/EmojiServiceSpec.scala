package net.model

import java.io.File

import net.service.EmojiService
import org.scalatest.FlatSpec

class EmojiServiceSpec extends FlatSpec {

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

  "Reading the emoji.json, i.e. the full Emoji file" should "decode successfully" in {
    val url      = this.getClass().getResource("/emoji.json")
    val file     = new File(url.toURI)
    val result   = EmojiService.read(file).unsafePerformSync
    assert(result.nonEmpty)
  }

  "Checking for the existence of an Emoji" should "succeed if a single unicode emoji present" in {
    val exclamation = Emoji.fromString(Some( "exclamation" ),  "0021" ).get
    val input       = "foo bar bippy - ends with exclamation point \u0021"
    assert( EmojiService.exists(exclamation, input) == Some(exclamation) )
  }

  import Emoji.codePointsToString

  "Checking for the existence of an Emoji" should "succeed if a size 2 unicode emoji present" in {
    val fake         = Emoji.fromString(Some( "fake" ),  "0021-0022" ).get
    val codePointStr = codePointsToString(List("0021", "0022")).get
    val input        = s"$codePointStr  blah blah blah lasdfasdf."
    assert( EmojiService.exists(fake, input) == Some(fake) )
  }

  "Checking for the existence of an Emoji" should "fail if there's no emoji present in the input String" in {
    val absent = Emoji.fromString(Some( "fake" ),  "0021-0022" ).get
    val input = "no unicode to see here..... blah blah blah"
    assert( EmojiService.exists(absent, input).isEmpty )
  }

  "Finding all emojis in an input String" should "find all input emojis" in {
    val exclamation    = Emoji.fromString(Some( "exclamation" ),  "0021" ).get
    val fake           = Emoji.fromString(Some( "fake" ),  "0021-0022" ).get
    val fakeUnicodeStr = codePointsToString(List("0021", "0022")).get
    val input = s"hello \u0021 world $fakeUnicodeStr"
    assert(EmojiService.findAll(List(exclamation, fake), input) == List(exclamation, fake))
  }

  "Finding all emojis in an input String" should "find all input emojis with one of size 2, length 5" in {
    val exclamation    = Emoji.fromString(Some( "exclamation" ),  "0021" ).get
    val fake           = Emoji.fromString(Some( "fake" ),  "0021-0022" ).get
    val fakeUnicodeStr = codePointsToString(List("0021", "0022")).get
    val input = s"hello \u0021 world $fakeUnicodeStr"
    assert(EmojiService.findAll(List(exclamation, fake), input) == List(exclamation, fake))
  }

  "Finding all emojis in an input String" should "find all but 1 input emojis" in {
    val exclamation    = Emoji.fromString(Some( "exclamation" ),  "0021" ).get
    val fake           = Emoji.fromString(Some( "fake" ),  "0021-0022" ).get
    val input = s"hello \u0021 world how's it going"
    assert(EmojiService.findAll(List(exclamation, fake), input) == List(exclamation))
  }

  "Finding all emojis in an input String" should "find the 'fake' emoji" in {
    val fake      = Emoji.fromString(Some( "fake" ),  "0021-0022" ).get
    val emojiText = codePointsToString(List("0021", "0022")).get
    val input = s"hello \u0021 world $emojiText"
    assert(EmojiService.findAll(List(fake), input) == List(fake))
  }

  "Finding all emojis in an input String" should "find two instances of the same emoji if it shows up twice" in {
    val fake      = Emoji.fromString(Some( "fake2" ),  "0021-0023" ).get
    val input = "hello \u0021\u0023 world \u0021\u0023"
    assert(EmojiService.findAll(List(fake), input) == List(fake, fake))
  }

  "Finding all emojis in an input String" should "find an emoji having 2 5-length hex codes" in {
    val emoji      = Emoji.fromString(Some( "REGIONAL INDICATOR SYMBOL LETTERS ZM" ), "1F1FF-1F1F2" ).get
    val unicodeStr = codePointsToString(List("1F1FF", "1F1F2")).get
    val input = s"hi world it's me, $unicodeStr"
    assert(EmojiService.findAll(List(emoji), input) == List(emoji))
  }

}
