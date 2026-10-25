package com.niko.macromenza.model

import com.google.gson.annotations.SerializedName

data class KategorijaJela(
    @SerializedName("naziv")
    val naziv: String,

    @SerializedName("brojJela")
    val brojJela: Int
)
