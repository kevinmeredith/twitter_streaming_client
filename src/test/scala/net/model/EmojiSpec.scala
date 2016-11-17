package net.model

import org.scalatest.{FlatSpec, Matchers}

object EmojiSpec {
  def readHex(hex: String): String =
    Integer.parseInt(hex, 16).toChar.toString

  def readHexes(hex1: String, hex2: String): String =
    readHex(hex1) + readHex(hex2)
}

class EmojiSpec extends FlatSpec with Matchers {

  import EmojiSpec._

  val noName = None
  val name = Some("some emoji")

  "Constructing an Emoji" should "succeed if the input is a single unicode of 4-5 characters in length and HEX" in {
    assert( Emoji.fromString(noName, "0021").map(_.unicode)  == Some(readHex("0021")))
    assert( Emoji.fromString(noName, "0023A").map(_.unicode) == Some(readHex("0023A")))
    assert( Emoji.fromString(name,   "AAAA").map(_.unicode)  == Some(readHex("AAAA")))
    assert( Emoji.fromString(name,   "BBBBB").map(_.unicode) == Some(readHex("BBBBB")))
  }

  "Constructing an Emoji" should "succeed if the input consists of two unicode's of 4-5 characters in length and HEX" in {
    assert( Emoji.fromString(noName, "0021-0022").map(_.unicode)  == Some(readHexes("0021", "0022")))
    assert( Emoji.fromString(noName, "abcde-abcdf").map(_.unicode)  == Some(readHexes("abcde", "abcdf")))
    assert( Emoji.fromString(noName, "00001-2222").map(_.unicode)  == Some(readHexes("00001", "2222")))
  }

  "Constructing an Emoji" should "fail if the input is HEX, but not 4-5 characters long." in {
    assert( Emoji.fromString(noName, "42").isEmpty )
    assert( Emoji.fromString(noName, "AAA").isEmpty )
    assert( Emoji.fromString(name, "AAABBBCCCDDD").isEmpty )
  }

  "Constructing an Emoji" should "fail if the input is not HEX." in {
    assert( Emoji.fromString(noName, "abcX").isEmpty )
    assert( Emoji.fromString(noName, "FIFO").isEmpty )
    assert( Emoji.fromString(name, "aaaZ").isEmpty )
  }

}


