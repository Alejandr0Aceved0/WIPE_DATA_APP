package com.alejoacevedodev.wipedatabeta.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import com.alejoacevedodev.wipedatabeta.data.model.WipeResult
import com.alejoacevedodev.wipedatabeta.domain.model.WipeMethod
import com.alejoacevedodev.wipedatabeta.domain.repository.IWipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.util.ArrayDeque
import kotlin.random.Random

class WipeRepositoryImpl(
    private val context: Context
) : IWipeRepository {

    override suspend fun wipe(uri: Uri, method: WipeMethod): Result<WipeResult> = withContext(Dispatchers.IO) {
        try {
            // Determinamos si es un árbol de carpetas (Tree) o un archivo (Document)
            val rootDocumentId = if (DocumentsContract.isTreeUri(uri)) {
                DocumentsContract.getTreeDocumentId(uri)
            } else {
                null
            }

            // Caso: Archivo único seleccionado directamente (no es una carpeta entera)
            if (rootDocumentId == null) {
                val name = getFileName(uri) ?: uri.lastPathSegment ?: "Archivo"
                val size = getFileSize(uri) // Calculamos tamaño antes de borrar
                return@withContext try {
                    wipeSingleFile(uri, method)
                    Result.success(WipeResult(listOf("Archivo: $name"), size))
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            // Caso: Carpeta (Árbol) -> Usamos algoritmo iterativo anti-crash
            val result = wipeIterative(uri, rootDocumentId, method)
            Result.success(result)

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Algoritmo iterativo (BFS/Pila) para recorrer y borrar carpetas profundas.
     * Evita StackOverflowError y calcula el tamaño total liberado.
     */
    private suspend fun wipeIterative(treeUri: Uri, rootId: String, method: WipeMethod): WipeResult {
        val deletedLog = ArrayList<String>()
        var totalBytesFreed = 0L

        // Pilas para organizar el descubrimiento y borrado
        val foldersToDelete = ArrayDeque<Pair<String, String>>() // Guardamos (ID, Nombre) para borrar al final
        val filesToDelete = ArrayList<Pair<Uri, String>>()       // Guardamos (URI, Nombre) para borrar primero

        // Cola para el descubrimiento (Breadth-First Search)
        val queue = ArrayDeque<String>()
        queue.add(rootId)

        val visited = HashSet<String>() // Protección contra ciclos simbólicos

        Log.d("WipeRepo", "Iniciando escaneo iterativo...")

        // --- FASE 1: DESCUBRIMIENTO (Sin borrar nada aún) ---
        while (!queue.isEmpty()) {
            val currentId = queue.removeFirst()
            if (visited.contains(currentId)) continue
            visited.add(currentId)

            val currentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, currentId)
            val mimeType = getMimeType(currentUri)
            val name = getFileName(currentUri) ?: currentId

            if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                // Es CARPETA: La agendamos para borrar al final (cuando esté vacía)
                foldersToDelete.addLast(Pair(currentId, name))

                // Buscamos sus hijos
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
                // Es ARCHIVO: Lo agendamos para borrar
                filesToDelete.add(Pair(currentUri, name))
            }
        }

        // --- FASE 2: BORRADO DE ARCHIVOS ---
        for ((fileUri, fileName) in filesToDelete) {
            try {
                // 1. Medir tamaño antes de destruir
                val size = getFileSize(fileUri)

                // 2. Ejecutar borrado (Sobrescritura o eliminación según método)
                wipeSingleFile(fileUri, method)

                // 3. Registrar éxito
                totalBytesFreed += size
                deletedLog.add("Archivo: $fileName (${formatSize(size)})")

            } catch (e: Exception) {
                Log.e("WipeRepo", "Fallo al borrar archivo: $fileName", e)
                deletedLog.add("ERROR: $fileName")
            }
        }

        // --- FASE 3: BORRADO DE CARPETAS (Orden Inverso LIFO) ---
        // Borramos desde la más profunda hacia la raíz
        while (!foldersToDelete.isEmpty()) {
            val (folderId, folderName) = foldersToDelete.removeLast()
            val folderUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, folderId)
            try {
                if (deleteDocument(folderUri)) {
                    deletedLog.add("Carpeta: $folderName")
                } else {
                    Log.w("WipeRepo", "No se pudo borrar carpeta: $folderName (posiblemente no vacía)")
                }
            } catch (e: Exception) {
                Log.e("WipeRepo", "Excepción borrando carpeta: $folderName", e)
            }
        }

        return WipeResult(deletedLog, totalBytesFreed)
    }

    /**
     * Aplica el método seleccionado a un solo archivo.
     */
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
                val patterns = listOf(
                    Pattern.ZERO, Pattern.ONE, Pattern.ZERO, Pattern.ONE,
                    Pattern.ZERO, Pattern.ONE, Pattern.RANDOM
                )
                overwriteAndWipe(uri, patterns)
            }

            WipeMethod.PM_CLEAR -> Log.w("WipeRepo", "PM_CLEAR no es aplicable para archivos individuales.")
        }
    }

    /**
     * Lógica de sobrescritura física.
     */
    private suspend fun overwriteAndWipe(uri: Uri, patterns: List<Pattern>) {
        try {
            context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                val len = pfd.statSize
                if (len == 0L) return@use

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
        } catch (e: Exception) {
            Log.w("WipeRepo", "Error sobrescribiendo $uri: ${e.message}. Intentando borrar directo.")
        }
        if (!deleteDocument(uri)) throw Exception("Error final delete")
    }

    // --- HELPERS ROBUSTOS ---

    private fun getFileSize(uri: Uri): Long {
        // Intento 1: Leer desde Content Provider (Rápido)
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        val size = cursor.getLong(sizeIndex)
                        if (size > 0) return size
                    }
                }
            }
        } catch (e: Exception) { }

        // Intento 2: Leer desde FileDescriptor (Preciso)
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
        } catch (e: Exception) { 0L }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    private fun getFileName(uri: Uri): String? {
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) return it.getString(0)
            }
        } catch (e: Exception) { }
        return null
    }

    private fun getMimeType(uri: Uri): String? {
        try {
            context.contentResolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)?.use {
                if (it.moveToFirst()) return it.getString(0)
            }
        } catch (e: Exception) { }
        return null
    }

    private fun deleteDocument(uri: Uri): Boolean {
        return try {
            DocumentsContract.deleteDocument(context.contentResolver, uri)
        } catch (e: Exception) { false }
    }

    private enum class Pattern { ZERO, ONE, RANDOM }
}