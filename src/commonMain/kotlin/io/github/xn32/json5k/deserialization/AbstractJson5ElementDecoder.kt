package io.github.xn32.json5k.deserialization

import io.github.xn32.json5k.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind.*
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.SerializersModule

internal sealed class AbstractJson5ElementDecoder(
    val json5: Json5,
) : Decoder {

    abstract val element: Json5Element

    override val serializersModule: SerializersModule
        get() = json5.serializersModule

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        return when (val kind = descriptor.kind) {
            CLASS, is PolymorphicKind, OBJECT -> {
                Json5ClassDecoder(
                    delegate = this,
                    json = element.asObject(),
                )
            }
            LIST -> {
                Json5ArrayDecoder(
                    delegate = this,
                    array = element.asArray()
                )
            }
            MAP -> {
                Json5MapDecoder(
                    delegate = this,
                    map = element.asObject()
                )
            }
            else -> throw UnsupportedOperationException("Unsupported serial kind: $kind")
        }
    }

    override fun decodeBoolean(): Boolean = element.asPrimitive().boolean

    override fun decodeByte(): Byte = element.asPrimitive().byte

    override fun decodeChar(): Char = element.asPrimitive().char

    override fun decodeDouble(): Double = element.asPrimitive().double

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int =
        enumDescriptor.getElementIndex(element.asPrimitive().content)

    override fun decodeFloat(): Float = element.asPrimitive().float

    override fun decodeInline(descriptor: SerialDescriptor): Decoder = this

    override fun decodeInt(): Int = element.asPrimitive().int

    override fun decodeLong(): Long = element.asPrimitive().long

    @ExperimentalSerializationApi
    override fun decodeNotNullMark(): Boolean = element != Json5Null

    @ExperimentalSerializationApi
    override fun decodeNull(): Nothing? = null

    override fun decodeShort(): Short = element.asPrimitive().short

    override fun decodeString(): String = element.asPrimitive().content
}

internal class Json5ElementDecoder(
    json5: Json5,
    override val element: Json5Element
) : AbstractJson5ElementDecoder(json5)

internal sealed class AbstractJson5CompositeDecoder(
    delegate: AbstractJson5ElementDecoder
) : AbstractJson5ElementDecoder(delegate.json5), CompoundDecoder {

    override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int): Decoder {
        return this.decodeElement(descriptor, index) { this }
    }

    @ExperimentalSerializationApi
    final override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        previousValue: T?
    ): T? {
        return decodeElement(descriptor, index) {
            if (element is Json5Null) {
                null
            } else {
                decodeSerializableValue(deserializer)
            }
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {}

}

internal class Json5ClassDecoder(
    delegate: AbstractJson5ElementDecoder,
    val json: Json5Object,
) : AbstractJson5CompositeDecoder(delegate) {

    private var allowImplicitNull: Boolean = false

    private var currentElementIndex: Int = 0

    override lateinit var element: Json5Element

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        while (currentElementIndex < descriptor.elementsCount) {
            val index = currentElementIndex
            val key = descriptor.getElementName(index)
            allowImplicitNull = false
            currentElementIndex++
            when {
                key in json -> {
                    element = json[key]!!
                    return index
                }
                allowImplicitNull(descriptor, index) -> {
                    element = Json5Null
                    return index
                }
            }
        }
        return DECODE_DONE
    }

    private fun allowImplicitNull(
        descriptor: SerialDescriptor,
        index: Int
    ): Boolean {
        val allow = !descriptor.isElementOptional(index) &&
                descriptor.getElementDescriptor(index).isNullable
        allowImplicitNull = allow
        return allow
    }

    override fun beginElement(descriptor: SerialDescriptor, index: Int) {}

    override fun endElement(descriptor: SerialDescriptor, index: Int) {}

}

internal class Json5MapDecoder(
    delegate: AbstractJson5ElementDecoder,
    val map: Json5Object,
) : AbstractJson5CompositeDecoder(delegate) {

    val iterator: Iterator<Json5Element> = iterator {
        for ((key, value) in map) {
            yield(Json5Primitive(key))
            yield(value)
        }
    }

    var currentElementIndex: Int = 0

    override lateinit var element: Json5Element

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
        return map.size
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return if (iterator.hasNext()) currentElementIndex
        else DECODE_DONE
    }

    override fun beginElement(descriptor: SerialDescriptor, index: Int) {
        element = iterator.next()
    }

    override fun endElement(descriptor: SerialDescriptor, index: Int) {
        currentElementIndex++
    }

    @ExperimentalSerializationApi
    override fun decodeSequentially(): Boolean = true
}

internal class Json5ArrayDecoder(
    delegate: AbstractJson5ElementDecoder,
    val array: Json5Array
) : AbstractJson5CompositeDecoder(delegate) {

    private var currentElementIndex: Int = 0

    override lateinit var element: Json5Element

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
        return array.size
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return if (currentElementIndex < array.size) currentElementIndex
        else DECODE_DONE
    }

    override fun beginElement(descriptor: SerialDescriptor, index: Int) {
        element = array[currentElementIndex]
    }

    override fun endElement(descriptor: SerialDescriptor, index: Int) {
        currentElementIndex++
    }

    @ExperimentalSerializationApi
    override fun decodeSequentially(): Boolean = true
}