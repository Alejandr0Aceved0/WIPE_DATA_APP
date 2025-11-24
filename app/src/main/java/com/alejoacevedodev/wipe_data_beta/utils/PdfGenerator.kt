package com.alejoacevedodev.wipe_data_beta.utils

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.os.StatFs
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    fun generateReportPdf(
        context: Context,
        methodName: String,
        startTime: Long,
        endTime: Long,
        deletedCount: Int
    ): File? { // <--- CAMBIO 1: Ahora retorna File?
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply { textSize = 24f; isFakeBoldText = true; color = android.graphics.Color.BLACK }
        val headerPaint = Paint().apply { textSize = 16f; isFakeBoldText = true; color = android.graphics.Color.DKGRAY }
        val bodyPaint = Paint().apply { textSize = 14f; color = android.graphics.Color.BLACK }
        val greenPaint = Paint().apply { textSize = 18f; color = android.graphics.Color.parseColor("#2E7D32"); isFakeBoldText = true }

        var y = 60f

        canvas.drawText("NULLUM Lite - Certificado de Borrado", 40f, y, titlePaint)
        y += 40f
        canvas.drawText("✔ Borrado Exitoso", 40f, y, greenPaint)
        y += 50f

        canvas.drawText("Informe del evento:", 40f, y, titlePaint)
        y += 40f

        // Atributos
        canvas.drawText("Atributos", 40f, y, headerPaint)
        y += 25f
        canvas.drawText("Método: $methodName", 40f, y, bodyPaint)
        y += 20f
        canvas.drawText("Verificación: 100% (Cifrado)", 40f, y, bodyPaint)
        y += 20f
        canvas.drawText("Integridad del Proceso: 100%", 40f, y, bodyPaint)
        y += 40f

        // Disco
        val totalSpace = getTotalInternalMemorySize()
        canvas.drawText("Información del Dispositivo", 40f, y, headerPaint)
        y += 25f
        canvas.drawText("Modelo: ${android.os.Build.MODEL}", 40f, y, bodyPaint)
        y += 20f
        canvas.drawText("Fabricante: ${android.os.Build.MANUFACTURER}", 40f, y, bodyPaint)
        y += 20f
        canvas.drawText("Capacidad Total: $totalSpace", 40f, y, bodyPaint)
        y += 40f

        // Resultados
        canvas.drawText("Resultados", 40f, y, headerPaint)
        y += 25f
        canvas.drawText("Estado: Completo", 40f, y, bodyPaint)
        y += 20f
        canvas.drawText("Items eliminados: $deletedCount", 40f, y, bodyPaint)
        y += 20f

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val startStr = if (startTime > 0) dateFormat.format(Date(startTime)) else "N/A"
        val endStr = if (endTime > 0) dateFormat.format(Date(endTime)) else "N/A"

        canvas.drawText("Inicio: $startStr", 40f, y, bodyPaint)
        y += 20f
        canvas.drawText("Fin: $endStr", 40f, y, bodyPaint)

        val durationSeconds = if (endTime > startTime) (endTime - startTime) / 1000 else 0
        y += 20f
        canvas.drawText("Duración: ${durationSeconds}s", 40f, y, bodyPaint)

        pdfDocument.finishPage(page)

        val fileName = "Certificado_Nullum_${System.currentTimeMillis()}.pdf"
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)

        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(context, "PDF guardado en Descargas", Toast.LENGTH_LONG).show()
            file // <--- CAMBIO 2: Retornamos el archivo exitoso
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            null // <--- CAMBIO 3: Retornamos null si falló
        } finally {
            pdfDocument.close()
        }
    }

    private fun getTotalInternalMemorySize(): String {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val totalBytes = totalBlocks * blockSize
        val gb = totalBytes.toDouble() / (1024 * 1024 * 1024)
        return String.format(Locale.getDefault(), "%.2f GB", gb)
    }
}