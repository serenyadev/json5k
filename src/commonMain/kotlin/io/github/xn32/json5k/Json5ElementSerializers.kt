package io.github.xn32.json5k

import io.github.xn32.json5k.deserialization.*
import io.github.xn32.json5k.deserialization.MainDecoder
import io.github.xn32.json5k.deserialization.StructDecoder
import io.github.xn32.json5k.format.Token
import io.github.xn32.json5k.generation.FormatGenerator
import io.github.xn32.json5k.parsing.InjectableLookaheadParser
import io.github.xn32.json5k.serialization.MainEncoder
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
internal object Json5ElementSerializer : KSerializer<Json5Element> {
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor(Json5Element::class.simpleName!!, SerialKind.CONTEXTUAL)

    override fun deserialize(decoder: Decoder): Json5Element {
        return when (val token = decoder.getParser("Json5Element").peek().item) {
            is Token.Value -> Json5PrimitiveSerializer.deserialize(decoder)
            is Token.BeginObject -> Json5ObjectSerializer.deserialize(decoder)
            is Token.BeginArray -> Json5ArraySerializer.deserialize(decoder)
            is Token.EndOfFile -> Json5Null
            else -> error("Unexpected token: $token")
        }
    }


    override fun serialize(encoder: Encoder, value: Json5Element) {
        when (value) {
            is Json5Object -> Json5ObjectSerializer.serialize(encoder, value)
            is Json5Array -> Json5ArraySerializer.serialize(encoder, value)
            is Json5Primitive -> Json5PrimitiveSerializer.serialize(encoder, value)
        }
    }
}

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
internal object Json5ObjectSerializer : KSerializer<Json5Object> {

    override val descriptor: SerialDescriptor = buildSerialDescriptor(Json5Object::class.simpleName!!, SerialKind.CONTEXTUAL)

    override fun deserialize(decoder: Decoder): Json5Object {
        val parser = decoder.getParser("Json5Object")
        if(parser.next().item != Token.BeginObject) error("Not an object")
        val map = mutableMapOf<String, Json5Element>()
        var memberName: String? = null
        while(true) {
            when(val token = parser.peek().item) {
                is Token.MemberName -> {
                    require(memberName == null) { "Duplicate member name" }
                    memberName = token.name
                }
                is Token.Value -> {
                    requireNotNull(memberName) { "Member must have a name" }
                    map[memberName] = Json5PrimitiveSerializer.deserialize(decoder)
                    memberName = null
                }
                Token.BeginObject -> {
                    requireNotNull(memberName) { "Member must have a name" }
                    map[memberName] = deserialize(decoder)
                    memberName = null
                }
                Token.BeginArray -> {
                    requireNotNull(memberName) { "Member must have a name" }
                    map[memberName] = Json5ArraySerializer.deserialize(decoder)
                    memberName = null
                }
                is Token.EndObject -> break
                else -> error("Unexpected token $token")
            }
            parser.next()
        }

        return Json5Object(map)
    }

    override fun serialize(encoder: Encoder, value: Json5Object) {
        val generator = encoder.getGenerator("Json5Object")
        generator.put(Token.BeginObject)
        for((key, element) in value) {
            if(value.comments.containsKey(key)) generator.writeComment(value.comments[key]!!)
            generator.put(Token.MemberName(key))
            Json5ElementSerializer.serialize(encoder, element)
        }
        generator.put(Token.EndObject)
    }
}

internal object MutableJson5ObjectSerializer : KSerializer<MutableJson5Object> {
    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = SerialDescriptor(MutableJson5Object::class.simpleName!!, Json5ObjectSerializer.descriptor)

    override fun deserialize(decoder: Decoder): MutableJson5Object = MutableJson5Object(Json5ObjectSerializer.deserialize(decoder))


    override fun serialize(encoder: Encoder, value: MutableJson5Object) {
        Json5ObjectSerializer.serialize(encoder, value)
    }
}

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
internal object Json5ArraySerializer : KSerializer<Json5Array> {

    override val descriptor: SerialDescriptor = buildSerialDescriptor(Json5Array::class.simpleName!!, SerialKind.CONTEXTUAL)

    override fun deserialize(decoder: Decoder): Json5Array {
        val parser = decoder.getParser("Json5Array")
        val output = mutableListOf<Json5Element>()
        if(parser.next().item != Token.BeginArray) error("Not an array")
        while(true) {
            when(val token = parser.peek().item) {
                is Token.Value -> output.add(Json5PrimitiveSerializer.deserialize(decoder))
                Token.BeginObject -> output.add(Json5ObjectSerializer.deserialize(decoder))
                Token.BeginArray -> output.add(this.deserialize(decoder))
                Token.EndArray -> break
                else -> error("Unexpected token $token")
            }
            parser.next()
        }
        return Json5Array(output)
    }

    override fun serialize(encoder: Encoder, value: Json5Array) {
        val generator = encoder.getGenerator("Json5Array")
        generator.put(Token.BeginArray)
        for(element in value) {
            Json5ElementSerializer.serialize(encoder, element)
        }
        generator.put(Token.EndArray)
    }
}

internal object MutableJson5ArraySerializer : KSerializer<MutableJson5Array> {
    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = SerialDescriptor(MutableJson5Array::class.simpleName!!, Json5ArraySerializer.descriptor)

    override fun deserialize(decoder: Decoder): MutableJson5Array = MutableJson5Array(Json5ArraySerializer.deserialize(decoder))
    override fun serialize(encoder: Encoder, value: MutableJson5Array) {
        TODO("Not yet implemented")
    }
}

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
internal object Json5PrimitiveSerializer : KSerializer<Json5Primitive> {

    override val descriptor: SerialDescriptor = buildSerialDescriptor(Json5Primitive::class.simpleName!!, SerialKind.CONTEXTUAL)

    override fun deserialize(decoder: Decoder): Json5Primitive {
        val parser = decoder.getParser("Json5Primitive")
        return when (val token = parser.peek().mapType<Token.Value>().item) {
            is Token.Num -> Json5Literal(token.rep, Json5Primitive.Type.NUMBER)
            is Token.Bool -> Json5Primitive(token.bool)
            is Token.Str -> Json5Primitive(token.string)
            Token.Null -> Json5Null
        }
    }

    override fun serialize(encoder: Encoder, value: Json5Primitive) {
        val generator = encoder.getGenerator("Json5Primitive")
        when(value.type) {
            Json5Primitive.Type.STRING -> generator.put(Token.Str(value.content))
            Json5Primitive.Type.BOOLEAN -> generator.put(Token.Bool(value.content.toBooleanStrict()))
            Json5Primitive.Type.NUMBER -> generator.put(Token.Num(value.content))
            Json5Primitive.Type.NULL -> generator.put(Token.Null)
        }
    }
}

private fun Decoder.getParser(elementName: String): InjectableLookaheadParser<Token> = when(this) {
    is MainDecoder -> parser
    is StructDecoder -> parser
    else -> error("$elementName can only be decoded from json5")
}

private fun Encoder.getGenerator(elementName: String): FormatGenerator = when(this) {
    is MainEncoder -> generator
    else -> error("$elementName can only be encoded from json5")
}
