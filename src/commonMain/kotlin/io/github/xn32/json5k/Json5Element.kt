@file:OptIn(ExperimentalContracts::class)
package io.github.xn32.json5k

import kotlinx.serialization.Serializable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@Serializable(with = Json5ElementSerializer::class)
sealed interface Json5Element

fun Json5Element.asPrimitive(): Json5Primitive {
    contract {
        returns() implies (this@asPrimitive is Json5Primitive)
    }
    return this as? Json5Primitive ?: error("$this is not a Json5Primitive")
}

fun Json5Element.asObject(): Json5Object {
    contract {
        returns() implies (this@asObject is Json5Object)
    }
    return this as? Json5Object ?: error("$this is not a Json5Object")
}

fun Json5Element.asArray(): Json5Array {
    contract {
        returns() implies (this@asArray is Json5Array)
    }
    return this as? Json5Array ?: error("$this is not a Json5Array")
}

@Serializable(with = Json5PrimitiveSerializer::class)
sealed class Json5Primitive : Json5Element {

    abstract val type: Type

    abstract val content: String

    override fun toString(): String = if(type == Type.STRING) "\"$content\"" else content

    enum class Type {
        STRING,
        BOOLEAN,
        NUMBER,
        NULL
    }

}

fun Json5Primitive(value: String): Json5Primitive = Json5Literal(value, Json5Primitive.Type.STRING)

fun Json5Primitive(value: Number): Json5Primitive = Json5Literal(value.toString(), Json5Primitive.Type.NUMBER)

fun Json5Primitive(value: Boolean): Json5Primitive = Json5Literal(if(value) "true" else "false", Json5Primitive.Type.BOOLEAN)

fun Json5Primitive(value: Nothing?): Json5Null = Json5Null

object Json5Null : Json5Primitive() {
    override val type get() = Type.NULL
    override val content: String = "null"
}

val Json5Primitive.byteOrNull: Byte?
    get() = content.parseLong(Byte.MIN_VALUE.toLong()..Byte.MAX_VALUE.toLong()).getOrNull()?.toByte()

val Json5Primitive.byte: Byte
    get() = content.parseLong(Byte.MIN_VALUE.toLong()..Byte.MAX_VALUE.toLong()).getOrThrow().toByte()

val Json5Primitive.shortOrNull: Short?
    get() = content.parseLong(Short.MIN_VALUE.toLong()..Short.MAX_VALUE.toLong()).getOrNull()?.toShort()

val Json5Primitive.short: Short
    get() = content.parseLong(Short.MIN_VALUE.toLong()..Short.MAX_VALUE.toLong()).getOrThrow().toShort()

val Json5Primitive.intOrNull: Int?
    get() = content.parseLong(Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()).getOrNull()?.toInt()

val Json5Primitive.int: Int
    get() = content.parseLong(Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()).getOrThrow().toInt()

val Json5Primitive.longOrNull: Long?
    get() = content.parseLong(Long.MIN_VALUE..Long.MAX_VALUE).getOrNull()

val Json5Primitive.long: Long
    get() = content.parseLong(Long.MIN_VALUE..Long.MAX_VALUE).getOrThrow()

val Json5Primitive.floatOrNull: Float?
    get() = content.toFloatOrNull()

val Json5Primitive.float: Float
    get() = content.toFloat()

val Json5Primitive.doubleOrNull: Double?
    get() = content.toDoubleOrNull()

val Json5Primitive.double: Double
    get() = content.toDouble()

val Json5Primitive.boolean: Boolean
    get() = content.toBoolean()

val Json5Primitive.booleanOrNull: Boolean?
    get() = content.toBooleanStrictOrNull()

val Json5Primitive.isString: Boolean
    get() = type == Json5Primitive.Type.STRING

val Json5Primitive.stringOrNull: String?
    get() = if(isString) content else null

val Json5Primitive.charOrNull: Char?
    get() = if(isString && content.length == 1) content[0] else null

val Json5Primitive.char: Char
    get() = charOrNull ?: error("element is not a char")

internal class Json5Literal(override val content: String, override val type: Type) : Json5Primitive()

@Serializable(with = Json5ObjectSerializer::class)
open class Json5Object(open val content: Map<String, Json5Element>) : Json5Element, Map<String, Json5Element> by content {

    internal open val comments: Map<String, String> get() = emptyMap()

    override fun equals(other: Any?): Boolean = content == other
    override fun hashCode(): Int = content.hashCode()
    override fun toString(): String {
        return content.entries.joinToString(
            separator = ",",
            prefix = "{",
            postfix = "}",
            transform = { (k, v) ->
                buildString {
                    append(k)
                    append(':')
                    append(v)
                }
            }
        )
    }
}

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
@Serializable(MutableJson5ObjectSerializer::class)
class MutableJson5Object(override val content: MutableMap<String, Json5Element>) : Json5Object(content), MutableMap<String, Json5Element> by content {
    override val comments: MutableMap<String, String> = mutableMapOf()

    constructor() : this(mutableMapOf())

    constructor(from: Json5Object) : this(from.content.toMutableMap())

    fun putComment(key: String, value: String) {
        comments[key] = value
    }
}

@Serializable(with = Json5ArraySerializer::class)
open class Json5Array(open val content: List<Json5Element>) : Json5Element, List<Json5Element> by content {
    override fun equals(other: Any?): Boolean = content == other
    override fun hashCode(): Int = content.hashCode()
    override fun toString(): String = content.joinToString(prefix = "[", postfix = "]", separator = ",")
}

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
@Serializable(with = MutableJson5ArraySerializer::class)
class MutableJson5Array(override val content: MutableList<Json5Element>) : Json5Array(content), MutableList<Json5Element> by content {
    constructor() : this(mutableListOf())
    constructor(from: Json5Array) : this(from.content.toMutableList())
}

fun MutableJson5Object.toImmutable() = Json5Object(this)

fun MutableJson5Array.toImmutable() = Json5Array(this)

fun Json5Object.toMutable() = MutableJson5Object(this)

fun Json5Array.toMutable() = MutableJson5Array(this)

private fun String.isHexNumber() = startsWith("0x") || startsWith("-0x")
private fun String.removeHexPrefix() = replaceFirst("0x", "")

private fun String.parseLong(bounds: LongRange): Result<Long> {
    val result = runCatching { if(isHexNumber()) removeHexPrefix().toLong(16) else toLong() }
    return result.mapCatching { require(it in bounds); it }
}