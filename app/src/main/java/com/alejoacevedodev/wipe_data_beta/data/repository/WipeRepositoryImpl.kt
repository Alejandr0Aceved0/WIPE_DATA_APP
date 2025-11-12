package com.alejoacevedodev.wipe_data_beta.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.alejoacevedodev.wipe_data_beta.domain.model.WipeMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import kotlin.random.Random

class WipeRepositoryImpl(
    private val context: Context
) : IWipeRepository {

    /**
     * Función pública: cambia al hilo de IO y delega la lógica.
     */
    override suspend fun wipe(uri: Uri, method: WipeMethod): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val rootDoc = DocumentFile.fromTreeUri(context, uri)
                    ?: throw Exception("No se pudo acceder al URI: $uri")
                wipeRecursive(rootDoc, method)
                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    /**
     * Lógica recursiva: borra carpetas y archivos según el método.
     */
    private suspend fun wipeRecursive(file: DocumentFile, method: WipeMethod) {
        if (file.isDirectory) {
            // --- Carpeta: recorrer recursivamente ---
            file.listFiles().forEach { child ->
                wipeRecursive(child, method)
            }

            // Borrar la carpeta cuando esté vacía
            if (!file.delete()) {
                throw Exception("No se pudo borrar la carpeta: ${file.uri}")
            }

        } else {
            // --- Archivo: aplicar método de borrado ---
            when (method) {
                WipeMethod.NIST_SP_800_88 -> {
                    if (!file.delete()) {
                        throw Exception("No se pudo borrar el archivo: ${file.uri}")
                    }
                }

                WipeMethod.DoD_5220_22_M -> {
                    val patterns = listOf(Pattern.ZERO, Pattern.ONE, Pattern.RANDOM)
                    overwriteAndWipe(file, patterns)
                }

                WipeMethod.BSI_TL_03423 -> {
                    val patterns = listOf(
                        Pattern.ZERO, Pattern.ONE, Pattern.ZERO, Pattern.ONE,
                        Pattern.ZERO, Pattern.ONE, Pattern.RANDOM
                    )
                    overwriteAndWipe(file, patterns)
                }
            }
        }
    }

    /**
     * Sobrescribe un archivo varias veces y luego lo borra.
     */
    private suspend fun overwriteAndWipe(file: DocumentFile, patterns: List<Pattern>) {
        context.contentResolver.openFileDescriptor(file.uri, "rw")?.use { fd ->
            val fileLength = fd.statSize
            if (fileLength == 0L) {
                file.delete()
                return
            }

            FileOutputStream(fd.fileDescriptor).use { outputStream ->
                val buffer = ByteArray(4096)
                for (pattern in patterns) {
                    var totalWritten = 0L
                    outputStream.channel.position(0)

                    while (totalWritten < fileLength) {
                        when (pattern) {
                            Pattern.ZERO -> buffer.fill(0x00)
                            Pattern.ONE -> buffer.fill(0xFF.toByte())
                            Pattern.RANDOM -> Random.nextBytes(buffer)
                        }
                        val bytesToWrite = minOf(buffer.size.toLong(), fileLength - totalWritten).toInt()
                        outputStream.write(buffer, 0, bytesToWrite)
                        totalWritten += bytesToWrite
                    }
                    outputStream.flush()
                }
            }
        }

        if (!file.delete()) {
            throw Exception("Fallo al borrar archivo tras sobrescritura: ${file.uri}")
        }
    }

    private enum class Pattern { ZERO, ONE, RANDOM }
}
