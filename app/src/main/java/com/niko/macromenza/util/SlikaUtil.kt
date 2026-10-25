package com.niko.macromenza.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

private const val NAJVECA_STRANICA = 1600
private const val NAJVECA_STRANICA_PREGLEDA = 900
private const val JPEG_KVALITETA = 85

/**
 * Smanji sliku na najviše 1600 px po duljoj strani, poštuje EXIF orijentaciju
 * i sprema JPEG (kvaliteta 85) u cache. Pozivatelj briše datoteku nakon slanja.
 */
suspend fun pripremiSliku(context: Context, uri: Uri): File = withContext(Dispatchers.IO) {
    val bitmapa = dekodirajOrijentirano(context, uri, NAJVECA_STRANICA)

    val mapa = File(context.cacheDir, "prepoznavanje").apply { mkdirs() }
    val datoteka = File(mapa, "slika_${UUID.randomUUID()}.jpg")
    try {
        FileOutputStream(datoteka).use { bitmapa.compress(Bitmap.CompressFormat.JPEG, JPEG_KVALITETA, it) }
    } catch (e: IOException) {
        datoteka.delete()
        throw e
    } finally {
        bitmapa.recycle()
    }

    datoteka
}

/** Manja, ispravno okrenuta slika za pregled na ekranu; null ako se ne može pročitati. */
suspend fun ucitajPregled(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
        dekodirajOrijentirano(context, uri, NAJVECA_STRANICA_PREGLEDA)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // IOException, SecurityException (URI bez prava čitanja) i sl.: pregled je samo dodatak.
        null
    } catch (e: OutOfMemoryError) {
        null
    }
}

private fun dekodirajOrijentirano(context: Context, uri: Uri, najvecaStranica: Int): Bitmap {
    val resolver = context.contentResolver

    val granice = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    // Uz inJustDecodeBounds decodeStream uvijek vraća null; mjerodavne su granice.
    (resolver.openInputStream(uri) ?: throw IOException("Slika se ne može otvoriti."))
        .use { BitmapFactory.decodeStream(it, null, granice) }
    if (granice.outWidth <= 0 || granice.outHeight <= 0) {
        throw IOException("Odabrana datoteka nije slika.")
    }

    var uzorak = 1
    while (maxOf(granice.outWidth, granice.outHeight) / (uzorak * 2) >= najvecaStranica) {
        uzorak *= 2
    }

    val opcije = BitmapFactory.Options().apply { inSampleSize = uzorak }
    val dekodirana = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opcije) }
        ?: throw IOException("Slika se ne može pročitati.")

    val orijentacija = try {
        resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (e: IOException) {
        ExifInterface.ORIENTATION_NORMAL
    }

    val matrica = Matrix()
    when (orijentacija) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrica.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrica.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrica.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrica.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrica.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrica.postRotate(90f)
            matrica.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrica.postRotate(270f)
            matrica.postScale(-1f, 1f)
        }
    }

    // Dovršetak smanjivanja (inSampleSize je samo potencija broja 2).
    val najveca = maxOf(dekodirana.width, dekodirana.height)
    if (najveca > najvecaStranica) {
        val faktor = najvecaStranica.toFloat() / najveca
        matrica.postScale(faktor, faktor)
    }

    if (matrica.isIdentity) return dekodirana

    val konacna = Bitmap.createBitmap(dekodirana, 0, 0, dekodirana.width, dekodirana.height, matrica, true)
    if (konacna !== dekodirana) dekodirana.recycle()
    return konacna
}
