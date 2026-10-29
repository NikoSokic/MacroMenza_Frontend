package com.niko.macromenza.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Tekst poruke iz ViewModela bez Contexta: naš tekst je resurs (s argumentima, npr. kod greške),
 * a poruka s backenda ("poruka" iz JSON-a) ili iz iznimke prikazuje se kakva jest.
 */
sealed interface UiTekst {

    class Resurs(@StringRes val id: Int, vararg val argumenti: Any) : UiTekst {
        // vararg je niz, pa jednakost (StateFlow, LaunchedEffect ključevi) uspoređuje sadržaj.
        override fun equals(other: Any?): Boolean =
            other is Resurs && id == other.id && argumenti.contentEquals(other.argumenti)

        override fun hashCode(): Int = 31 * id + argumenti.contentHashCode()
    }

    data class Dinamicki(val tekst: String) : UiTekst
}

@Composable
fun UiTekst.uString(): String = when (this) {
    is UiTekst.Resurs -> stringResource(id, *argumenti)
    is UiTekst.Dinamicki -> tekst
}

/** Za snackbar i ostalo izvan kompozicije. */
fun UiTekst.uString(context: Context): String = when (this) {
    is UiTekst.Resurs -> context.getString(id, *argumenti)
    is UiTekst.Dinamicki -> tekst
}
