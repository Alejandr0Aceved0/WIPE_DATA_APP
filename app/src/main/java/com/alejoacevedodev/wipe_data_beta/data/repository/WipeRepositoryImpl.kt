    package com.alejoacevedodev.wipe_data_beta.data.repository

    import android.content.Context
    import android.net.Uri
    import android.provider.DocumentsContract
    import com.alejoacevedodev.wipe_data_beta.domain.model.WipeMethod
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.withContext
    import kotlin.random.Random
    import java.io.FileOutputStream

    class WipeRepositoryImpl(
        private val context: Context
    ) : IWipeRepository {

        override suspend fun wipe(uri: Uri, method: WipeMethod): Result<Unit> = withContext(Dispatchers.IO) {
            try {
                when (method) {
                    // El método más simple y efectivo en Android moderno
                    WipeMethod.NIST_SP_800_88 -> {
                        deleteByUri(uri)
                    }

                    // Estos dos son simulaciones de sobrescritura
                    WipeMethod.DoD_5220_22_M -> {
                        val patterns = listOf(Pattern.ZERO, Pattern.ONE, Pattern.RANDOM)
                        overwriteAndWipe(uri, patterns)
                    }
                    WipeMethod.BSI_TL_03423 -> {
                        val patterns = listOf(
                            Pattern.ZERO, Pattern.ONE, Pattern.ZERO, Pattern.ONE,
                            Pattern.ZERO, Pattern.ONE, Pattern.RANDOM
                        ) // (Simplificado, puedes usar los 7 pases reales)
                        overwriteAndWipe(uri, patterns)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }

        // --- MÉTODOS PRIVADOS DE AYUDA ---

        private fun deleteByUri(uri: Uri): Boolean {
            // Usa el Storage Access Framework (SAF) para borrar
            return DocumentsContract.deleteDocument(context.contentResolver, uri)
        }

        private suspend fun overwriteAndWipe(uri: Uri, patterns: List<Pattern>) {
            // 1. Abre el ParcelFileDescriptor en un bloque .use
            //    Esto asegura que se cierre automáticamente
            context.contentResolver.openFileDescriptor(uri, "rw")?.use { fileDescriptor ->
                val fileLength = fileDescriptor.statSize

                // 2. Crea un FileOutputStream (que SÍ tiene .channel)
                //    usando el fileDescriptor del PFD
                FileOutputStream(fileDescriptor.fileDescriptor).use { outputStream ->

                    val buffer = ByteArray(4096)

                    for (pattern in patterns) {
                        var totalWritten = 0L

                        // 3. ¡Esta línea ahora funciona!
                        outputStream.channel.position(0)

                        while (totalWritten < fileLength) {
                            // Prepara el buffer con el patrón
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
                } // El FileOutputStream se cierra aquí
            } // El ParcelFileDescriptor se cierra aquí

            // Paso final: Borrar el archivo después de sobrescribirlo
            deleteByUri(uri)
        }

        private enum class Pattern { ZERO, ONE, RANDOM }
    }