package com.niko.macromenza.model

import com.google.gson.annotations.SerializedName

data class CestoJedenoJelo(
    @SerializedName("jelo")
    val jelo: Jelo,

    @SerializedName("brojPuta")
    val brojPuta: Int,

    @SerializedName("zadnjiPut")
    val zadnjiPut: String
)
