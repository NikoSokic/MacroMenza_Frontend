package com.niko.macromenza.testutil

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import org.junit.Assert.fail
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

/**
 * Gson puni objekte refleksijom i zaobilazi Kotlin null-safety: polje koje fali u JSON-u (ili je preimenovano)
 * ostaje null, a primitivno (Double, Int, Boolean) tiho postane 0 ili false. Ovo prolazi stablo objekata iz paketa
 * model usporedno s izvornim JSON-om i javlja svako ne-nullable svojstvo čijeg naziva nema u JSON-u ili je null.
 */
object NullProvjera {

    /** Samo null provjera (bez izvornog JSON-a); primitivna polja koja fale ne mogu se uočiti. */
    fun provjeri(objekt: Any?) = provjeri(objekt, null)

    /** [json] je izvorni tekst odgovora iz kojeg je [objekt] nastao. */
    fun provjeri(objekt: Any?, json: String?) {
        val greske = mutableListOf<String>()
        hodaj(objekt, json?.let { JsonParser.parseString(it) }, "$", greske)
        if (greske.isNotEmpty()) {
            fail("Odgovor nije usklađen s Kotlin modelom:\n  " + greske.joinToString("\n  "))
        }
    }

    private fun hodaj(o: Any?, j: JsonElement?, put: String, greske: MutableList<String>) {
        when (o) {
            null, is String, is Number, is Boolean, is Char, is Enum<*> -> return
            is Iterable<*> -> o.forEachIndexed { i, x ->
                hodaj(x, (j as? JsonArray)?.get(i), "$put[$i]", greske)
            }
            is Map<*, *> -> o.forEach { (k, v) ->
                hodaj(v, (j as? JsonObject)?.get(k.toString()), "$put.$k", greske)
            }
            else -> {
                if (!o.javaClass.name.startsWith("com.niko.macromenza.model.")) return
                val objekt = j as? JsonObject
                o::class.memberProperties.forEach { svojstvo ->
                    svojstvo.isAccessible = true
                    val naziv = svojstvo.javaField?.getAnnotation(SerializedName::class.java)?.value ?: svojstvo.name
                    val vrijednost = svojstvo.getter.call(o)
                    val nullable = svojstvo.returnType.isMarkedNullable

                    if (objekt != null && !nullable && !objekt.has(naziv)) {
                        greske += "$put.$naziv: nema ga u JSON-u (${o.javaClass.simpleName}.${svojstvo.name})"
                    }
                    if (vrijednost == null && !nullable) {
                        greske += "$put.$naziv: null, a Kotlin tip nije nullable (${o.javaClass.simpleName}.${svojstvo.name})"
                    }
                    hodaj(vrijednost, objekt?.get(naziv), "$put.$naziv", greske)
                }
            }
        }
    }
}
