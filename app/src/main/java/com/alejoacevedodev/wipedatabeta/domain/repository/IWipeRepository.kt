package com.alejoacevedodev.wipedatabeta.domain.repository

import android.net.Uri
import com.alejoacevedodev.wipedatabeta.data.model.WipeResult
import com.alejoacevedodev.wipedatabeta.domain.model.WipeMethod

interface IWipeRepository {
    /**
     * Borra un archivo o el contenido de una carpeta
     * @param uri El URI del archivo o carpeta (obtenido de SAF)
     * @param method El método de borrado a aplicar
     */
    suspend fun wipe(uri: Uri, method: WipeMethod): Result<WipeResult>
}