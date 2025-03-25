package io.github.xn32.json5k.serialization

import io.github.xn32.json5k.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
class Json5ElementEncoder(
    val json5: Json5
) : CompoundEncoder() {
    override val serializersModule: SerializersModule
        get() = json5.serializersModule

    private val elementStack: ArrayDeque<Json5Element> = ArrayDeque()

    private var <T> ArrayDeque<T>.last: T
        get() = last()
        set(value) { set(elementStack.lastIndex, value) }

    init {
        elementStack.addLast(Json5Null)
    }

    public fun get(): Json5Element {
        if(elementStack.size > 1) error("Incomplete elements found")
        return elementStack.last
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        when(descriptor.kind) {
            StructureKind.CLASS, StructureKind.OBJECT, StructureKind.MAP, is PolymorphicKind -> elementStack.last = MutableJson5Object()
            StructureKind.LIST -> elementStack.last = MutableJson5Array()
            else -> throw UnsupportedOperationException()
        }
        return this
    }

    override fun beginElement(descriptor: SerialDescriptor, index: Int) {
        elementStack.addLast(Json5Null)
    }

    override fun endElement(descriptor: SerialDescriptor, index: Int) {
        val element = elementStack.removeLast()
        when (val up = elementStack.last) {
            is MutableJson5Object -> {
                val comment = descriptor.getElementAnnotations(index).find { it is SerialComment } as? SerialComment
                val key = descriptor.getElementName(index)
                up[key] = element
                comment?.let { up.putComment(key, it.value) }
            }
            is MutableJson5Array -> up.add(element)
            else -> throw UnsupportedOperationException()
        }
    }

    override fun encodeBoolean(value: Boolean) {
        elementStack.last = Json5Primitive(value)
    }

    override fun encodeByte(value: Byte) {
        elementStack.last = Json5Primitive(value)
    }

    override fun encodeChar(value: Char) {
        elementStack.last = Json5Primitive(value.toString())
    }

    override fun encodeDouble(value: Double) {
        elementStack.last = Json5Primitive(value)
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        elementStack.last = Json5Primitive(enumDescriptor.getElementName(index))
    }

    override fun encodeFloat(value: Float) {
        elementStack.last = Json5Primitive(value)
    }

    override fun encodeInt(value: Int) {
        elementStack.last = Json5Primitive(value)
    }

    override fun encodeLong(value: Long) {
        elementStack.last = Json5Primitive(value)
    }

    override fun encodeNull() {
        elementStack.last = Json5Null
    }

    override fun encodeShort(value: Short) {
        elementStack.last = Json5Primitive(value)
    }

    override fun encodeString(value: String) {
        elementStack.last = Json5Primitive(value)
    }

    override fun encodeInline(descriptor: SerialDescriptor): Encoder = this
}