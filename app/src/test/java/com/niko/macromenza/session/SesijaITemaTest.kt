package com.niko.macromenza.session

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.niko.macromenza.ui.theme.AppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** DataStore na pravoj (Robolectric) datoteci; sesija i tema moraju preživjeti ponovno otvaranje. */
@RunWith(RobolectricTestRunner::class)
class UserSessionManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val sesija = UserSessionManager(context)

    @Before
    fun cisto() = runBlocking { sesija.obrisiSesiju() }

    @Test
    fun praznaSesijaNijePrijavljena() = runBlocking {
        assertNull(sesija.korisnikId.first())
        assertNull(sesija.supabaseUid.first())
        assertFalse(sesija.isLoggedIn.first())
    }

    @Test
    fun spremljenaSesijaSeCita() = runBlocking {
        sesija.spremiSesiju(7, "uid-1")

        assertEquals(7L, sesija.korisnikId.first())
        assertEquals("uid-1", sesija.supabaseUid.first())
        assertTrue(sesija.isLoggedIn.first())
    }

    @Test
    fun drugaInstancaVidiIstuSesiju() = runBlocking {
        sesija.spremiSesiju(7, "uid-1")

        assertEquals(7L, UserSessionManager(context).korisnikId.first())
    }

    @Test
    fun neispravnaSesijaSeNeSprema() = runBlocking {
        sesija.spremiSesiju(0, "uid-1")
        sesija.spremiSesiju(-3, "uid-1")
        sesija.spremiSesiju(7, "   ")
        sesija.spremiSesiju(7, "")

        assertNull(sesija.korisnikId.first())
        assertFalse(sesija.isLoggedIn.first())
    }

    @Test
    fun novaSesijaPrepisujeStaru() = runBlocking {
        sesija.spremiSesiju(7, "uid-1")
        sesija.spremiSesiju(9, "uid-2")

        assertEquals(9L, sesija.korisnikId.first())
        assertEquals("uid-2", sesija.supabaseUid.first())
    }

    @Test
    fun odjavaBriseSesiju() = runBlocking {
        sesija.spremiSesiju(7, "uid-1")

        sesija.obrisiSesiju()

        assertNull(sesija.korisnikId.first())
        assertFalse(sesija.isLoggedIn.first())
    }
}

@RunWith(RobolectricTestRunner::class)
class ThemeManagerTest {

    private val tema = ThemeManager(ApplicationProvider.getApplicationContext())

    @Test
    fun zadanaTemaJeSustavska() = runBlocking {
        tema.saveTheme(AppTheme.SYSTEM)
        assertEquals(AppTheme.SYSTEM, tema.theme.first())
    }

    @Test
    fun odabranaTemaSePamti() = runBlocking {
        tema.saveTheme(AppTheme.DARK)
        assertEquals(AppTheme.DARK, tema.theme.first())

        tema.saveTheme(AppTheme.LIGHT)
        assertEquals(AppTheme.LIGHT, tema.theme.first())
    }
}
