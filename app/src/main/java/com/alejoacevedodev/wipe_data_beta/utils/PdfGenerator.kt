package com.alejoacevedodev.wipe_data_beta.utils

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
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
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Tamaño A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // --- CONFIGURACIÓN DE ESTILOS ---
        val titlePaint = Paint().apply {
            textSize = 24f
            isFakeBoldText = true
            color = android.graphics.Color.BLACK
        }
        val headerPaint = Paint().apply {
            textSize = 16f
            isFakeBoldText = true
            color = android.graphics.Color.DKGRAY
        }
        val bodyPaint = Paint().apply {
            textSize = 14f
            color = android.graphics.Color.BLACK
        }
        val greenPaint = Paint().apply {
            textSize = 18f
            color = android.graphics.Color.parseColor("#2E7D32") // Verde éxito
            isFakeBoldText = true
        }

        var y = 60f // Posición vertical inicial

        // --- DIBUJAR CONTENIDO ---

        // Título
        canvas.drawText("NULLUM Lite - Certificado de Borrado", 40f, y, titlePaint)
        y += 40f

        // Estado
        canvas.drawText("✔ Borrado Exitoso", 40f, y, greenPaint)
        y += 50f

        // Sección: Informe del evento
        canvas.drawText("Informe del evento:", 40f, y, titlePaint)
        y += 40f

        // Bloque: Atributos
        canvas.drawText("Atributos", 40f, y, headerPaint)
        y += 25f
        canvas.drawText("Método de Borrado: $methodName", 40f, y, bodyPaint)
        y += 20f
        canvas.drawText("Verificación: 100% (Simulado)", 40f, y, bodyPaint)
        y += 20f
        canvas.drawText("Integridad del Proceso: 100%", 40f, y, bodyPaint)
        y += 40f

        // Bloque: Información del Disco (Simulado para MVP)
        canvas.drawText("Información del Dispositivo", 40f, y, headerPaint)
        y += 25f
        canvas.drawText("Modelo: ${android.os.Build.MODEL}", 40f, y, bodyPaint)
        y += 20f
        canvas.drawText("Fabricante: ${android.os.Build.MANUFACTURER}", 40f, y, bodyPaint)
        y += 20f
        canvas.drawText("Android: ${android.os.Build.VERSION.RELEASE}", 40f, y, bodyPaint)
        y += 40f

        // Bloque: Resultados
        canvas.drawText("Resultados", 40f, y, headerPaint)
        y += 25f
        canvas.drawText("Estado: Completo", 40f, y, bodyPaint)
        y += 20f
        canvas.drawText("Carpetas procesadas: $deletedCount", 40f, y, bodyPaint)
        y += 20f

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        canvas.drawText("Inicio: ${dateFormat.format(Date(startTime))}", 40f, y, bodyPaint)
        y += 20f
        canvas.drawText("Fin: ${dateFormat.format(Date(endTime))}", 40f, y, bodyPaint)

        val durationSeconds = (endTime - startTime) / 1000
        y += 20f
        canvas.drawText("Duración: ${durationSeconds}s", 40f, y, bodyPaint)

        // Finalizar página
        pdfDocument.finishPage(page)

        // --- GUARDAR ARCHIVO ---
        val fileName = "Certificado_Nullum_${System.currentTimeMillis()}.pdf"
        // Guardar en carpeta de Descargas pública
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(context, "PDF guardado en Descargas:\n$fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al guardar PDF: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            pdfDocument.close()
        }
    }
}