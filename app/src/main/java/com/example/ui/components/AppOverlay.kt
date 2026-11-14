package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Etkinliğin kök bileşiminde, her şeyin üstünde çizilen tek katman.
 * Sistem Dialog penceresi bazı telefonlarda ekranın altına taştığı için tam ekran sayfalar burada gösterilir.
 */
object AppOverlay {
    var content by mutableStateOf<(@Composable () -> Unit)?>(null)
}
