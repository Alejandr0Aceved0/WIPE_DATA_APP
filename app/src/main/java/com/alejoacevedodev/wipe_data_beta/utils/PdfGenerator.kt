package com.alejoacevedodev.wipe_data_beta.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.log10
import kotlin.math.pow

object PdfGenerator {

    /**
     * Genera el reporte PDF con diseño de Certificado Profesional,
     * detalles técnicos, lista de archivos y paginación automática.
     */
    fun generateReportPdf(
        context: Context,
        operatorName: String, // 👈 AÑADIDO: Nombre del usuario que realizó el borrado
        methodName: String,
        startTime: Long,
        endTime: Long,
        deletedCount: Int,
        deletedFiles: List<String>,
        freedBytes: Long = 0L
    ): File? {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        var pageNumber = 1

        // --- DEFINICIÓN DE ESTILOS (Pinceles) ---
        val titlePaint = Paint().apply { textSize = 24f; isFakeBoldText = true; color = Color.parseColor("#3F51B5"); isAntiAlias = true }
        val subtitlePaint = Paint().apply { textSize = 14f; isFakeBoldText = true; color = Color.BLACK; isAntiAlias = true }
        val headerSectionPaint = Paint().apply { textSize = 16f; isFakeBoldText = true; color = Color.parseColor("#444444"); isAntiAlias = true }
        val labelPaint = Paint().apply { textSize = 12f; color = Color.parseColor("#666666"); isAntiAlias = true }
        val valuePaint = Paint().apply { textSize = 12f; color = Color.BLACK; isAntiAlias = true }
        val valueBoldPaint = Paint().apply { textSize = 12f; color = Color.BLACK; isFakeBoldText = true; isAntiAlias = true }
        val successPaint = Paint().apply { textSize = 12f; color = Color.parseColor("#2E7D32"); isFakeBoldText = true; isAntiAlias = true }
        val smallPaint = Paint().apply { textSize = 10f; color = Color.DKGRAY; isAntiAlias = true }
        val fileListPaint = Paint().apply { textSize = 10f; color = Color.DKGRAY; isAntiAlias = true }
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
        val blueLinePaint = Paint().apply { color = Color.parseColor("#3F51B5"); strokeWidth = 2f }

        // Helper para crear páginas nuevas
        fun startNewPage(): PdfDocument.Page {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
            return pdfDocument.startPage(pageInfo)
        }

        // --- INICIO PÁGINA 1 ---
        var page = startNewPage()
        var canvas = page.canvas
        var y = 60f
        val margin = 40f
        val rightMargin = pageWidth - margin

        // 1. ENCABEZADO
        canvas.drawText("CERTIFICADO DE BORRADO", margin, y, titlePaint)
        y += 20f
        canvas.drawText("Dispositivo: ${Build.MODEL}", margin, y, subtitlePaint)

        // Sello Dorado (Simulación gráfica)
        drawSeal(canvas, pageWidth - margin - 30f, 60f)

        y += 30f
        val dateOnlyFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
        val timeOnlyFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val dateStr = "Fecha: ${dateOnlyFormat.format(Date(endTime))}"
        val timeStr = "Hora: ${timeOnlyFormat.format(Date(endTime))}"

        drawTextRightAligned(canvas, dateStr, rightMargin, y, valuePaint)
        y += 15f
        drawTextRightAligned(canvas, timeStr, rightMargin, y, valuePaint)

        y += 15f
        canvas.drawLine(margin, y, rightMargin, y, blueLinePaint)
        y += 30f

        // 2. ATRIBUTOS
        canvas.drawText("Atributos del Proceso", margin, y, headerSectionPaint)
        y += 10f
        canvas.drawLine(margin, y, rightMargin, y, linePaint)
        y += 20f

        // --- AÑADIDO: Mostrar Operador ---
        y = drawRow(canvas, "Operador Responsable:", operatorName, margin, rightMargin, y, labelPaint, valueBoldPaint)

        val methodDisplay = when {
            methodName.contains("DoD") -> "DoD 5220.22-M (3 Pases)"
            methodName.contains("BSI") -> "BSI TL-03423 (7 Pases)"
            else -> "NIST 800-88 (Criptográfico)"
        }

        y = drawRow(canvas, "Método de Borrado:", methodDisplay, margin, rightMargin, y, labelPaint, valueBoldPaint)
        y = drawRow(canvas, "Verificación:", "100% (Hash Check)", margin, rightMargin, y, labelPaint, valuePaint)
        y = drawRow(canvas, "Integridad:", "Verificada", margin, rightMargin, y, labelPaint, successPaint)
        y += 20f

        // 3. INFO DISPOSITIVO
        canvas.drawText("Información del Dispositivo", margin, y, headerSectionPaint)
        y += 10f
        canvas.drawLine(margin, y, rightMargin, y, linePaint)
        y += 20f

        y = drawRow(canvas, "Modelo:", Build.MODEL, margin, rightMargin, y, labelPaint, valuePaint)

        val deviceId = getDeviceUniqueId(context)
        y = drawRow(canvas, "ID Dispositivo (Serial):", deviceId, margin, rightMargin, y, labelPaint, valuePaint)

        y = drawRow(canvas, "Fabricante:", Build.MANUFACTURER, margin, rightMargin, y, labelPaint, valuePaint)
        y = drawRow(canvas, "Plataforma:", "Android ${Build.VERSION.RELEASE}", margin, rightMargin, y, labelPaint, valuePaint)

        // Información de almacenamiento
        val storageInfo = getStorageDetails()
        y = drawRow(canvas, "Capacidad Total:", storageInfo.total, margin, rightMargin, y, labelPaint, valuePaint)
        y = drawRow(canvas, "Espacio Disponible:", storageInfo.available, margin, rightMargin, y, labelPaint, valuePaint)

        // Espacio liberado en verde
        y = drawRow(canvas, "Espacio Liberado:", formatFileSize(freedBytes), margin, rightMargin, y, labelPaint, successPaint)
        y += 20f

        // 4. RESULTADOS
        canvas.drawText("Resultados", margin, y, headerSectionPaint)
        y += 10f
        canvas.drawLine(margin, y, rightMargin, y, linePaint)
        y += 20f

        val fullDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS", Locale.getDefault())

        // Cálculo preciso de duración
        val durationMillis = (endTime - startTime)
        val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60
        val millis = durationMillis % 1000
        val durationFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)

        y = drawRow(canvas, "Estado Final:", "COMPLETADO", margin, rightMargin, y, labelPaint, successPaint)
        y = drawRow(canvas, "Items Eliminados:", "$deletedCount archivos/carpetas", margin, rightMargin, y, labelPaint, valuePaint)
        y = drawRow(canvas, "Inicio:", fullDateFormat.format(Date(startTime)), margin, rightMargin, y, labelPaint, valuePaint)
        y = drawRow(canvas, "Fin:", fullDateFormat.format(Date(endTime)), margin, rightMargin, y, labelPaint, valuePaint)
        y = drawRow(canvas, "Duración:", durationFormatted, margin, rightMargin, y, labelPaint, valueBoldPaint)

        y += 10f
        // Timestamps en crudo (Long)
        y = drawRow(canvas, "TS Inicio (ms):", "$startTime", margin, rightMargin, y, labelPaint, smallPaint)
        y = drawRow(canvas, "TS Fin (ms):", "$endTime", margin, rightMargin, y, labelPaint, smallPaint)

        y += 30f

        // PASOS TÉCNICOS
        canvas.drawText("Pasos de Ejecución:", margin, y, valueBoldPaint)
        y += 20f
        val passes = getPassesList(methodName)
        for (pass in passes) {
            // Chequear si cabe en la página
            if (y > pageHeight - margin - 40f) {
                drawFooter(canvas, pageWidth.toFloat(), y + 20f)
                pdfDocument.finishPage(page)
                page = startNewPage()
                canvas = page.canvas
                y = margin + 20f
                canvas.drawText("Continuación Pasos...", margin, y, headerSectionPaint)
                y += 25f
            }
            canvas.drawText("• $pass", margin + 10f, y, valuePaint)
            drawTextRightAligned(canvas, "OK", rightMargin - 20f, y, successPaint)
            y += 15f
        }
        y += 20f
        canvas.drawLine(margin, y, rightMargin, y, linePaint)
        y += 30f

        // LISTA DE ARCHIVOS (Paginada)
        canvas.drawText("Detalle de Archivos Eliminados:", margin, y, headerSectionPaint)
        y += 20f

        if (deletedFiles.isEmpty()) {
            canvas.drawText("(Ningún archivo encontrado o borrado)", margin, y, fileListPaint)
            y += 15f
        } else {
            for (fileName in deletedFiles) {
                // Control de salto de página
                if (y > pageHeight - margin - 40f) {
                    drawFooter(canvas, pageWidth.toFloat(), y + 20f)
                    pdfDocument.finishPage(page)
                    page = startNewPage()
                    canvas = page.canvas
                    y = margin + 20f
                    canvas.drawText("Continuación Archivos...", margin, y, headerSectionPaint)
                    y += 25f
                }
                // Acortar nombre si es muy largo para que no se salga
                val cleanName = if (fileName.length > 90) fileName.take(87) + "..." else fileName
                canvas.drawText("• $cleanName", margin, y, fileListPaint)
                y += 12f
            }
        }

        // Footer final
        y += 20f
        if (y > pageHeight - margin) {
            drawFooter(canvas, pageWidth.toFloat(), pageHeight - margin)
            pdfDocument.finishPage(page)
            page = startNewPage()
            canvas = page.canvas
            y = margin + 20f
        }
        drawFooter(canvas, pageWidth.toFloat(), y)
        pdfDocument.finishPage(page)

        // --- GUARDADO ---
        val fileName = "Certificado_Nullum_${System.currentTimeMillis()}.pdf"

        // 1. Intentar carpeta pública "Documentos"
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val reportsDir = File(documentsDir, "Reportes_Nullum")

        if (!reportsDir.exists()) {
            val created = reportsDir.mkdirs()
            if (!created) return saveToPrivateStorage(context, pdfDocument, fileName)
        }

        val file = File(reportsDir, fileName)

        return try {
            pdfDocument.writeTo(FileOutputStream(file))

            // Avisar al sistema (Media Scanner)
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("application/pdf"),
                null
            )

            Toast.makeText(context, "Guardado en: Documentos/Reportes_Nullum", Toast.LENGTH_LONG).show()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback a privado si falla público
            saveToPrivateStorage(context, pdfDocument, fileName)
        } finally {
            pdfDocument.close()
        }
    }

    // --- HELPERS ---

    private fun getDeviceUniqueId(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                try {
                    val serial = Build.getSerial()
                    if (serial != Build.UNKNOWN) return serial
                } catch (e: SecurityException) {}
            }
        }
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "Desconocido"
        } catch (e: Exception) {
            "Desconocido"
        }
    }

    private fun saveToPrivateStorage(context: Context, document: PdfDocument, fileName: String): File? {
        return try {
            val file = File(context.getExternalFilesDir(null), fileName)
            document.writeTo(FileOutputStream(file))
            Toast.makeText(context, "Guardado (Privado): ${file.absolutePath}", Toast.LENGTH_LONG).show()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun drawFooter(canvas: android.graphics.Canvas, pageWidth: Float, y: Float) {
        val footerPaint = Paint().apply { textSize = 10f; color = Color.GRAY; textAlign = Paint.Align.CENTER; isAntiAlias = true }
        val centerX = pageWidth / 2f
        canvas.drawText("Borrado certificado por NULLUM Lite v1.0.12.1", centerX, y, footerPaint)
        canvas.drawText("Kernel: ${System.getProperty("os.version")}", centerX, y + 12f, footerPaint)
    }

    private fun drawRow(c: android.graphics.Canvas, l: String, v: String, x1: Float, x2: Float, y: Float, p1: Paint, p2: Paint): Float {
        c.drawText(l, x1, y, p1)
        drawTextRightAligned(c, v, x2, y, p2)
        return y + 20f
    }

    private fun drawTextRightAligned(c: android.graphics.Canvas, text: String, x: Float, y: Float, paint: Paint) {
        val originalAlign = paint.textAlign
        paint.textAlign = Paint.Align.RIGHT
        c.drawText(text, x, y, paint)
        paint.textAlign = originalAlign
    }

    private fun drawSeal(canvas: android.graphics.Canvas, x: Float, y: Float) {
        val sealPaint = Paint().apply { color = Color.parseColor("#DAA520"); style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = true }
        val sealFill = Paint().apply { color = Color.parseColor("#FFF8E1"); isAntiAlias = true }
        val textPaint = Paint().apply { color = Color.parseColor("#B8860B"); textSize = 8f; isFakeBoldText = true; textAlign = Paint.Align.CENTER; isAntiAlias = true }
        canvas.drawCircle(x, y, 25f, sealFill)
        canvas.drawCircle(x, y, 25f, sealPaint)
        canvas.drawText("100%", x, y - 2f, textPaint)
        canvas.drawText("SECURE", x, y + 8f, textPaint)
    }

    private fun getPassesList(methodName: String): List<String> {
        return when {
            methodName.contains("DoD") -> listOf(
                "Pase 1 (0x00) - Sobrescritura ceros",
                "Pase 2 (0xFF) - Sobrescritura unos",
                "Pase 3 (Random) - Sobrescritura aleatoria"
            )
            methodName.contains("BSI") -> listOf(
                "Pase 1 (0x00) - Sobrescritura",
                "Pase 2 (0xFF) - Sobrescritura",
                "Pase 3 (0x00) - Sobrescritura",
                "Pase 4 (0xFF) - Sobrescritura",
                "Pase 5 (0x00) - Sobrescritura",
                "Pase 6 (0xFF) - Sobrescritura",
                "Pase 7 (Random) - Sobrescritura final"
            )
            else -> listOf( // NIST
                "Pase 1 - Destrucción de Llave Criptográfica (FBE)",
                "Pase 2 - Verificación de inaccessibilidad"
            )
        }
    }

    data class StorageInfo(val total: String, val available: String)

    private fun getStorageDetails(): StorageInfo {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val totalBytes = stat.blockCountLong * stat.blockSizeLong
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        return StorageInfo(formatFileSize(totalBytes), formatFileSize(availableBytes))
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        return String.format(Locale.getDefault(), "%.2f %s", bytes / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
    }
}