package com.alejoacevedodev.wipedatabeta.presentation.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.alejoacevedodev.wipedatabeta.R

/**
 * Copyright 2026 Wireless&Mobile, Inc. All rights reserved
 * File CompositeLogo 1.2.5 8/01/26
 *
 * @author Carlos Alejandro Acevedo Espitia
 * @version 1.2.5 8/01/26
 * @since 1.2.5
 */

@Composable
fun CompositeLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(110.dp),
        contentAlignment = Alignment.Center
    ) {
        // Capa 1: Bote de basura
        Image(
            painter = painterResource(id = R.drawable.img_trash),
            contentDescription = null,
            // Eliminamos fillMaxSize() para que size(90.dp) funcione
            modifier = Modifier
                .size(110.dp)
                .offset(x = (-15).dp, y = (-10).dp),
            contentScale = ContentScale.Fit // Asegura que la imagen escale internamente
        )

        // Capa 2: Borrador
        Image(
            painter = painterResource(id = R.drawable.img_eraser),
            contentDescription = null,
            modifier = Modifier
                .size(55.dp) // Ahora este tamaño sí se aplicará
                .align(Alignment.BottomStart)
                .offset(x = -8.dp, y = (-2).dp),
            contentScale = ContentScale.Fit
        )
    }
}