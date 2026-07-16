package com.appswithlove.updraft.ui.feedback

import androidx.compose.ui.graphics.ImageBitmap
import com.appswithlove.updraft.ui.drawing.DrawnPath

expect fun decodePng(bytes: ByteArray): ImageBitmap
expect fun renderAnnotated(base: ByteArray, paths: List<DrawnPath>): ByteArray
