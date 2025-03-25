/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.xn32.json5k.serialization

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.*


public abstract class CompoundEncoder : Encoder, CompositeEncoder {

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder = this

    override fun endStructure(descriptor: SerialDescriptor) {}


    abstract fun beginElement(descriptor: SerialDescriptor, index: Int)

    abstract fun endElement(descriptor: SerialDescriptor, index: Int)

    /**
     * Invoked to encode a value when specialized `encode*` method was not overridden.
     */

    override fun encodeInline(descriptor: SerialDescriptor): Encoder = this

    // Delegating implementation of CompositeEncoder
    final override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) {
        beginElement(descriptor, index)
        encodeBoolean(value)
        endElement(descriptor, index)
    }
    final override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) {
        beginElement(descriptor, index)
        encodeByte(value)
        endElement(descriptor, index)
    }
    final override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) {
        beginElement(descriptor, index)
        encodeShort(value)
        endElement(descriptor, index)
    }
    final override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) {
        beginElement(descriptor, index)
        encodeInt(value)
        endElement(descriptor, index)
    }

    final override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) {
        beginElement(descriptor, index)
        encodeLong(value)
        endElement(descriptor, index)
    }

    final override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) {
        beginElement(descriptor, index)
        encodeFloat(value)
        endElement(descriptor, index)
    }
    final override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) {
        beginElement(descriptor, index)
        encodeDouble(value)
        endElement(descriptor, index)
    }
    final override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) {
        beginElement(descriptor, index)
        encodeChar(value)
        endElement(descriptor, index)
    }
    final override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
        beginElement(descriptor, index)
        encodeString(value)
        endElement(descriptor, index)
    }

    final override fun encodeInlineElement(
        descriptor: SerialDescriptor,
        index: Int
    ): Encoder = DelegatedInlineEncoder(this, descriptor, index)

    override fun <T : Any?> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        beginElement(descriptor, index)
        encodeSerializableValue(serializer, value)
        endElement(descriptor, index)
    }

    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
    ) {
        beginElement(descriptor, index)
        encodeNullableSerializableValue(serializer, value)
        endElement(descriptor, index)
    }
}

private class DelegatedInlineEncoder(
    val delegate: CompoundEncoder,
    val descriptor: SerialDescriptor,
    val index: Int
) : Encoder {
    override val serializersModule: SerializersModule
        get() = delegate.serializersModule

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder = delegate

    override fun encodeBoolean(value: Boolean) {
        delegate.encodeBooleanElement(descriptor, index, value)
    }

    override fun encodeByte(value: Byte) {
        delegate.encodeByteElement(descriptor, index, value)
    }

    override fun encodeChar(value: Char) {
        delegate.encodeCharElement(descriptor, index, value)
    }

    override fun encodeDouble(value: Double) {
        delegate.encodeDoubleElement(descriptor, index, value)
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        delegate.beginElement(descriptor, this.index)
        delegate.encodeEnum(enumDescriptor, index)
        delegate.endElement(descriptor, this.index)
    }

    override fun encodeFloat(value: Float) {
        delegate.encodeFloatElement(descriptor, index, value)
    }

    override fun encodeInline(descriptor: SerialDescriptor): Encoder = this

    override fun encodeInt(value: Int) {
        delegate.encodeIntElement(descriptor, index, value)
    }

    override fun encodeLong(value: Long) {
        delegate.encodeLongElement(descriptor, index, value)
    }

    @ExperimentalSerializationApi
    override fun encodeNull() {
        delegate.beginElement(descriptor, index)
        delegate.encodeNull()
        delegate.endElement(descriptor, index)
    }

    override fun encodeShort(value: Short) {
        delegate.encodeShortElement(descriptor, index, value)
    }

    override fun encodeString(value: String) {
        delegate.encodeStringElement(descriptor, index, value)
    }
}