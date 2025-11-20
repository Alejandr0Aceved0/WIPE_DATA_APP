package com.alejoacevedodev.wipe_data_beta.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
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

    override suspend fun wipe(uri: Uri, method: WipeMethod): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Obtenemos el ID raíz.
            val rootDocumentId = if (DocumentsContract.isTreeUri(uri)) {
                DocumentsContract.getTreeDocumentId(uri)
            } else {
                null
            }

            if (rootDocumentId == null) {
                // Intento de borrado simple si no es árbol
                return@withContext if (deleteDocument(uri)) Result.success(1) else Result.success(0)
            }

            // Usamos el método iterativo
            val count = wipeIterative(uri, rootDocumentId, method)

            Result.success(count)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private suspend fun wipeIterative(treeUri: Uri, rootId: String, method: WipeMethod): Int {
        var deletedCount = 0

        // Pila para carpetas (LIFO - Last In First Out)
        val foldersToDelete = ArrayDeque<String>()
        // Lista para archivos
        val filesToDelete = ArrayList<Uri>()

        val queue = ArrayDeque<String>()
        queue.add(rootId)
        val visited = HashSet<String>()

        Log.d("WipeRepo", "Iniciando escaneo desde: $rootId")

        // --- FASE 1: DESCUBRIMIENTO ---
        while (!queue.isEmpty()) {
            val currentId = queue.removeFirst()
            if (visited.contains(currentId)) continue
            visited.add(currentId)

            val currentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, currentId)
            val mimeType = getMimeType(currentUri)

            if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                // Es CARPETA
                foldersToDelete.addLast(currentId)

                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, currentId)
                try {
                    context.contentResolver.query(
                        childrenUri,
                        arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                        null, null, null
                    )?.use { cursor ->
                        while (cursor.moveToNext()) {
                            val childId = cursor.getString(0)
                            if (childId != null) queue.addLast(childId)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WipeRepo", "Error leyendo hijos de $currentId", e)
                }
            } else {
                // Es ARCHIVO
                filesToDelete.add(currentUri)
            }
        }

        Log.d("WipeRepo", "Archivos a borrar: ${filesToDelete.size}, Carpetas a borrar: ${foldersToDelete.size}")

        // --- FASE 2: BORRAR ARCHIVOS ---
        for (fileUri in filesToDelete) {
            try {
                wipeSingleFile(fileUri, method)
                deletedCount++
            } catch (e: Exception) {
                Log.e("WipeRepo", "Fallo al borrar archivo: $fileUri", e)
            }
        }

        // --- FASE 3: BORRAR CARPETAS (Orden inverso) ---
        while (!foldersToDelete.isEmpty()) {
            val folderId = foldersToDelete.removeLast()
            val folderUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, folderId)
            try {
                if (deleteDocument(folderUri)) {
                    deletedCount++
                } else {
                    Log.w("WipeRepo", "No se pudo borrar carpeta: $folderId (¿quizás no está vacía?)")
                }
            } catch (e: Exception) {
                Log.e("WipeRepo", "Excepción borrando carpeta: $folderId", e)
            }
        }

        return deletedCount
    }

    private suspend fun wipeSingleFile(uri: Uri, method: WipeMethod) {
        when (method) {
            WipeMethod.NIST_SP_800_88 -> {
                if (!deleteDocument(uri)) throw Exception("NIST delete failed")
            }
            WipeMethod.DoD_5220_22_M -> {
                val patterns = listOf(Pattern.ZERO, Pattern.ONE, Pattern.RANDOM)
                overwriteAndWipe(uri, patterns)
            }
            WipeMethod.BSI_TL_03423 -> {
                val patterns = listOf(Pattern.ZERO, Pattern.ONE, Pattern.ZERO, Pattern.ONE, Pattern.ZERO, Pattern.ONE, Pattern.RANDOM)
                overwriteAndWipe(uri, patterns)
            }
        }
    }

    private suspend fun overwriteAndWipe(uri: Uri, patterns: List<Pattern>) {
        try {
            context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                val len = pfd.statSize
                if (len > 0L) {
                    FileOutputStream(pfd.fileDescriptor).use { fos ->
                        val buf = ByteArray(4096)
                        for (pat in patterns) {
                            var written = 0L
                            fos.channel.position(0)
                            while (written < len) {
                                when (pat) {
                                    Pattern.ZERO -> buf.fill(0)
                                    Pattern.ONE -> buf.fill(-1)
                                    Pattern.RANDOM -> Random.nextBytes(buf)
                                }
                                val toWrite = minOf(buf.size.toLong(), len - written).toInt()
                                fos.write(buf, 0, toWrite)
                                written += toWrite
                            }
                            fos.flush()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("WipeRepo", "Error sobrescribiendo $uri: ${e.message}. Intentando borrar directo.")
        }

        if (!deleteDocument(uri)) throw Exception("Error final deleting $uri")
    }

    private fun getMimeType(uri: Uri): String? {
        try {
            context.contentResolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)?.use {
                if (it.moveToFirst()) return it.getString(0)
            }
        } catch (e: Exception) {
            Log.e("WipeRepo", "Error obteniendo MimeType", e)
        }
        return null
    }

    private fun deleteDocument(uri: Uri): Boolean {
        return try {
            DocumentsContract.deleteDocument(context.contentResolver, uri)
        } catch (e: Exception) {
            false
        }
    }

    private enum class Pattern { ZERO, ONE, RANDOM }
}