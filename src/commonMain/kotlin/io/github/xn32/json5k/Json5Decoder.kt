package io.github.xn32.json5k

import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder

sealed interface Json5Decoder : Decoder, CompositeDecoder {

    val json5: Json5

    fun decodeJson5Element(): Json5Element

}