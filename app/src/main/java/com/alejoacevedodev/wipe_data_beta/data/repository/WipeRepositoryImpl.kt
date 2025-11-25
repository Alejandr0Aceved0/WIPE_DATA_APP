package com.alejoacevedodev.wipe_data_beta.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import com.alejoacevedodev.wipe_data_beta.domain.model.WipeMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.util.ArrayDeque
import kotlin.random.Random

class WipeRepositoryImpl(
    private val context: Context
) : IWipeRepository {

    override suspend fun wipe(uri: Uri, method: WipeMethod): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            // Determinamos si es un árbol de carpetas o un archivo suelto
            val rootDocumentId = if (DocumentsContract.isTreeUri(uri)) {
                DocumentsContract.getTreeDocumentId(uri)
            } else {
                null
            }

            // Caso 1: Es un archivo único seleccionado directamente (poco común en este flujo)
            if (rootDocumentId == null) {
                val name = getFileName(uri) ?: uri.lastPathSegment ?: "Archivo"
                return@withContext try {
                    wipeSingleFile(uri, method)
                    Result.success(listOf("Archivo: $name"))
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            // Caso 2: Es una carpeta (Árbol) -> Usamos el algoritmo iterativo robusto
            val deletedFilesLog = wipeIterative(uri, rootDocumentId, method)
            Result.success(deletedFilesLog)

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Recorre la estructura de carpetas sin usar recursión (evita StackOverflow).
     * Retorna la lista de nombres de los elementos borrados.
     */
    private suspend fun wipeIterative(treeUri: Uri, rootId: String, method: WipeMethod): List<String> {
        val deletedLog = ArrayList<String>()

        // Pilas para organizar el borrado
        val foldersToDelete = ArrayDeque<Pair<String, String>>() // Guardamos (ID, Nombre)
        val filesToDelete = ArrayList<Pair<Uri, String>>()       // Guardamos (URI, Nombre)

        // Cola para el descubrimiento (BFS)
        val queue = ArrayDeque<String>()
        queue.add(rootId)

        val visited = HashSet<String>() // Evitar ciclos infinitos

        Log.d("WipeRepo", "Iniciando escaneo...")

        // --- FASE 1: DESCUBRIMIENTO ---
        while (!queue.isEmpty()) {
            val currentId = queue.removeFirst()
            if (visited.contains(currentId)) continue
            visited.add(currentId)

            val currentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, currentId)
            val mimeType = getMimeType(currentUri)
            val name = getFileName(currentUri) ?: currentId

            if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                // Es Carpeta: La guardamos para borrar al final y buscamos sus hijos
                foldersToDelete.addLast(Pair(currentId, name))

                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, currentId)
                try {
                    context.contentResolver.query(
                        childrenUri,
                        arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                        null, null, null
                    )?.use { cursor ->
                        while (cursor.moveToNext()) {
                            val childId = cursor.getString(0)
                            if (!childId.isNullOrEmpty()) queue.addLast(childId)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WipeRepo", "Error leyendo hijos de $name", e)
                }
            } else {
                // Es Archivo: Lo guardamos para borrar primero
                filesToDelete.add(Pair(currentUri, name))
            }
        }

        // --- FASE 2: BORRADO DE ARCHIVOS (SOBRESCRITURA) ---
        for ((fileUri, fileName) in filesToDelete) {
            try {
                wipeSingleFile(fileUri, method)
                deletedLog.add("Archivo: $fileName")
            } catch (e: Exception) {
                Log.e("WipeRepo", "Fallo al borrar archivo: $fileName", e)
                deletedLog.add("ERROR: $fileName")
            }
        }

        // --- FASE 3: BORRADO DE CARPETAS (VACÍAS) ---
        // Iteramos en orden inverso (LIFO) para borrar desde lo más profundo hacia arriba
        while (!foldersToDelete.isEmpty()) {
            val (folderId, folderName) = foldersToDelete.removeLast()
            val folderUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, folderId)
            try {
                if (deleteDocument(folderUri)) {
                    deletedLog.add("Carpeta: $folderName")
                } else {
                    Log.w("WipeRepo", "No se pudo borrar carpeta: $folderName")
                }
            } catch (e: Exception) {
                Log.e("WipeRepo", "Excepción borrando carpeta: $folderName", e)
            }
        }

        return deletedLog
    }

    /**
     * Aplica el método de borrado seleccionado a un solo archivo.
     */
    private suspend fun wipeSingleFile(uri: Uri, method: WipeMethod) {
        when (method) {
            WipeMethod.NIST_SP_800_88 -> {
                // Borrado criptográfico (Eliminación simple en Android FBE)
                if (!deleteDocument(uri)) throw Exception("NIST delete failed")
            }
            WipeMethod.DoD_5220_22_M -> {
                val patterns = listOf(Pattern.ZERO, Pattern.ONE, Pattern.RANDOM)
                overwriteAndWipe(uri, patterns)
            }
            WipeMethod.BSI_TL_03423 -> {
                val patterns = listOf(
                    Pattern.ZERO, Pattern.ONE, Pattern.ZERO, Pattern.ONE,
                    Pattern.ZERO, Pattern.ONE, Pattern.RANDOM
                )
                overwriteAndWipe(uri, patterns)
            }
        }
    }

    /**
     * Sobrescribe el contenido del archivo con los patrones dados y luego lo borra.
     */
    private suspend fun overwriteAndWipe(uri: Uri, patterns: List<Pattern>) {
        try {
            context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                val len = pfd.statSize
                // Si está vacío, no hay nada que sobrescribir, salimos para borrar directo
                if (len == 0L) return@use

                FileOutputStream(pfd.fileDescriptor).use { fos ->
                    val buf = ByteArray(4096) // Buffer de 4KB

                    for (pat in patterns) {
                        var written = 0L
                        fos.channel.position(0) // Reiniciar posición al inicio del archivo

                        while (written < len) {
                            when (pat) {
                                Pattern.ZERO -> buf.fill(0)
                                Pattern.ONE -> buf.fill(-1) // 0xFF
                                Pattern.RANDOM -> Random.nextBytes(buf)
                            }

                            val toWrite = minOf(buf.size.toLong(), len - written).toInt()
                            fos.write(buf, 0, toWrite)
                            written += toWrite
                        }
                        fos.flush() // Forzar escritura al disco
                    }
                }
            }
        } catch (e: Exception) {
            // Si falla la sobrescritura (ej. permisos raros), logueamos pero intentamos borrar el archivo igual
            Log.w("WipeRepo", "Error sobrescribiendo $uri: ${e.message}. Intentando borrar directo.")
        }

        // Paso final obligatorio: Eliminar el archivo del sistema
        if (!deleteDocument(uri)) {
            throw Exception("Error final deleting $uri")
        }
    }

    // --- HELPERS ---

    private fun getFileName(uri: Uri): String? {
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) return it.getString(0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getMimeType(uri: Uri): String? {
        try {
            context.contentResolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)?.use {
                if (it.moveToFirst()) return it.getString(0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun deleteDocument(uri: Uri): Boolean {
        return try {
            DocumentsContract.deleteDocument(context.contentResolver, uri)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private enum class Pattern { ZERO, ONE, RANDOM }
}