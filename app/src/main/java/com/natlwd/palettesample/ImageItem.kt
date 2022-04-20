package com.natlwd.palettesample

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ImageItem(
    val url: String
): Parcelable
