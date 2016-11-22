package net.model

import org.scalatest.{FlatSpec, Matchers}

class EmojiSpec extends FlatSpec with Matchers {
  
  import Emoji.codePointsToString

  val noName = None
  val name = Some("some emoji")

  "Constructing an Emoji" should "succeed if the input is a single unicode of 4-5 characters in length and HEX" in {
    assert( Emoji.fromString(noName, "0021").map(_.value)  == codePointsToString(List("0021")))
    assert( Emoji.fromString(noName, "0023A").map(_.value) == codePointsToString(List("0023A")))
    assert( Emoji.fromString(name,   "AAAA").map(_.value)  == codePointsToString(List("AAAA")))
    assert( Emoji.fromString(name,   "BBBBB").map(_.value) == codePointsToString(List("BBBBB")))
  }

  "Constructing an Emoji" should "succeed if the input consists of two unicode's of 4-5 characters in length and HEX" in {
    assert( Emoji.fromString(noName, "0021-0022").map(_.value)   == codePointsToString(List("0021", "0022")))
    assert( Emoji.fromString(noName, "abcde-abcdf").map(_.value) == codePointsToString(List("abcDe", "abcdf")))
    assert( Emoji.fromString(noName, "00001-2222").map(_.value)  == codePointsToString(List("00001", "2222")))
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


