package io.github.xn32.json5k.deserialization

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder

internal interface CompoundDecoder : Decoder, CompositeDecoder {
    fun beginElement(
        descriptor: SerialDescriptor,
        index: Int
    )

    fun endElement(
        descriptor: SerialDescriptor,
        index: Int
    )

    override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean {
        return decodeElement(descriptor, index) {
            decodeBoolean()
        }
    }

    override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte {
        return decodeElement(descriptor, index) {
            decodeByte()
        }
    }

    override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short {
        return decodeElement(descriptor, index) {
            decodeShort()
        }
    }

    override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int {
        return decodeElement(descriptor, index) {
            decodeInt()
        }
    }

    override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long {
        return decodeElement(descriptor, index) {
            decodeLong()
        }
    }

    override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float {
        return decodeElement(descriptor, index) {
            decodeFloat()
        }
    }

    override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double {
        return decodeElement(descriptor, index) {
            decodeDouble()
        }
    }

    override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char {
        return decodeElement(descriptor, index) {
            decodeChar()
        }
    }

    override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String {
        return decodeElement(descriptor, index) {
            decodeString()
        }
    }

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?
    ): T {
        return decodeElement(descriptor, index) {
            decodeSerializableValue(deserializer)
        }
    }
}

internal inline fun <T> CompoundDecoder.decodeElement(
    descriptor: SerialDescriptor,
    index: Int,
    decode: () -> T
): T {
    beginElement(descriptor, index)
    val value = decode()
    endElement(descriptor, index)
    return value
}