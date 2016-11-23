package net.model

import org.scalatest.{FlatSpec, Matchers}

class EmojiSpec extends FlatSpec with Matchers {

  val noName = None
  val name   = Some("some emoji")

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


