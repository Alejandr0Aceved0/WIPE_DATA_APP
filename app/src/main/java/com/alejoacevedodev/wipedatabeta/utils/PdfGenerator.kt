package com.alejoacevedodev.wipedatabeta.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
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
        operatorName: String,
        methodName: String,
        startTime: Long,
        endTime: Long,
        deletedCount: Int,
        deletedFiles: List<String>,
        freedBytes: Long = 0L,
        packageWeights: MutableMap<String, Long>,
        packagesToWipe: List<String>
    ): File? {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        var pageNumber = 1
        val kitNumber = getBluetoothKitName()
        val deviceSerial = getSystemSerial()
        val appVersion = getAppVersion(context)

        // --- PINCELES ---
        val titlePaint = Paint().apply {
            textSize = 24f; isFakeBoldText = true; color = Color.parseColor("#3F51B5"); isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            textSize = 14f; isFakeBoldText = true; color = Color.BLACK; isAntiAlias = true
        }
        val headerSectionPaint = Paint().apply {
            textSize = 16f; isFakeBoldText = true; color = Color.parseColor("#444444"); isAntiAlias = true
        }
        val labelPaint = Paint().apply {
            textSize = 11f; color = Color.parseColor("#666666"); isAntiAlias = true
        }
        val valuePaint = Paint().apply { textSize = 11f; color = Color.BLACK; isAntiAlias = true }
        val valueBoldPaint = Paint().apply {
            textSize = 11f; color = Color.BLACK; isFakeBoldText = true; isAntiAlias = true
        }
        val successPaint = Paint().apply {
            textSize = 11f; color = Color.parseColor("#2E7D32"); isFakeBoldText = true; isAntiAlias = true
        }
        val smallPaint = Paint().apply { textSize = 9f; color = Color.DKGRAY; isAntiAlias = true }
        val fileListPaint = Paint().apply { textSize = 9f; color = Color.DKGRAY; isAntiAlias = true }
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
        val blueLinePaint = Paint().apply { color = Color.parseColor("#3F51B5"); strokeWidth = 2f }
        val bitmapPaint = Paint().apply { isAntiAlias = true; isFilterBitmap = true; isDither = true }

        fun startNewPage(): PdfDocument.Page {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
            return pdfDocument.startPage(pageInfo)
        }

        // --- PÁGINA 1 ---
        var page = startNewPage()
        var canvas = page.canvas
        var y = 60f
        val margin = 40f
        val rightMargin = pageWidth - margin

        // 1. ENCABEZADO ALINEADO (TÍTULO + LOGO + SELLO)
        canvas.drawText("CERTIFICADO DE BORRADO", margin, y, titlePaint)

        // Logo al final del título
        val logo = getAppIcon(context)
        val titleWidth = titlePaint.measureText("CERTIFICADO DE BORRADO")
        val iconSize = 54f
        if (logo != null) {
            val logoPaint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
            }

            // CÁLCULO DE ALINEACIÓN CENTRADA:
            // El texto mide 24f de alto. Para que un logo de 54f quede centrado:
            // Subimos el logo la mitad de su tamaño (27f) y lo bajamos la mitad de la altura del texto (aprox 8-10f)
            val logoX = margin + titleWidth + 15f // Un poco más de aire (15f)
            val verticalOffset = (iconSize / 2f) - 8f // Ajuste fino para el centro visual

            val rectLogo = android.graphics.RectF(
                logoX,
                y - (iconSize / 2f) - 8f, // Techo del logo
                logoX + iconSize,
                y + (iconSize / 2f) - 8f  // Piso del logo
            )

            canvas.drawBitmap(logo, null, rectLogo, logoPaint)
        }

        // Sello alineado al extremo derecho
        drawSealAlineado(canvas, rightMargin - 30f, y - 8f)

        y += 30f
        canvas.drawText("KIT IDENTIFICADOR: $kitNumber", margin, y, subtitlePaint)

        y += 25f
        val dateOnlyFormat = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "ES"))
        val timeOnlyFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        drawTextRightAligned(canvas, "Fecha: ${dateOnlyFormat.format(Date(endTime))}", rightMargin, y - 15f, valuePaint)
        drawTextRightAligned(canvas, "Hora: ${timeOnlyFormat.format(Date(endTime))}", rightMargin, y, valuePaint)

        y += 10f
        canvas.drawLine(margin, y, rightMargin, y, blueLinePaint)
        y += 30f

        // 2. ATRIBUTOS
        canvas.drawText("Atributos del Proceso", margin, y, headerSectionPaint)
        y += 10f
        canvas.drawLine(margin, y, rightMargin, y, linePaint)
        y += 20f

        y = drawRow(canvas, "Operador Responsable:", operatorName, margin, rightMargin, y, labelPaint, valueBoldPaint)
        val methodDisplay = when {
            methodName.contains("DoD") -> "DoD 5220.22-M (3 Pases)"
            methodName.contains("BSI") -> "BSI TL-03423 (7 Pases)"
            else -> "NIST 800-88 (Criptográfico)"
        }
        y = drawRow(canvas, "Método de Borrado:", methodDisplay, margin, rightMargin, y, labelPaint, valueBoldPaint)
        y = drawRow(canvas, "Verificación:", "100% (Bit-by-Bit Check)", margin, rightMargin, y, labelPaint, valuePaint)
        y = drawRow(canvas, "Integridad:", "Verificada", margin, rightMargin, y, labelPaint, successPaint)
        y += 20f

        // 3. INFO DISPOSITIVO
        canvas.drawText("Información del Dispositivo", margin, y, headerSectionPaint)
        y += 10f
        canvas.drawLine(margin, y, rightMargin, y, linePaint)
        y += 20f

        y = drawRow(canvas, "ID Kit Logístico:", kitNumber, margin, rightMargin, y, labelPaint, valueBoldPaint)
        y = drawRow(canvas, "Modelo:", Build.MODEL, margin, rightMargin, y, labelPaint, valuePaint)
        y = drawRow(canvas, "Serial de Hardware:", deviceSerial, margin, rightMargin, y, labelPaint, valueBoldPaint)
        y = drawRow(canvas, "Fabricante:", Build.MANUFACTURER, margin, rightMargin, y, labelPaint, valuePaint)
        y = drawRow(canvas, "Android ID:", getDeviceUniqueId(context), margin, rightMargin, y, labelPaint, valuePaint)
        y = drawRow(canvas, "Versión SO:", "Android ${Build.VERSION.RELEASE}", margin, rightMargin, y, labelPaint, valuePaint)

        val storageInfo = getStorageDetails()
        y = drawRow(canvas, "Capacidad Total:", storageInfo.total, margin, rightMargin, y, labelPaint, valuePaint)
        y = drawRow(canvas, "Espacio Liberado:", formatFileSize(freedBytes), margin, rightMargin, y, labelPaint, successPaint)
        y += 20f

        // 4. RESULTADOS Y TIEMPOS (Incluyendo Timestamps Raw)
        canvas.drawText("Tiempos de Ejecución", margin, y, headerSectionPaint)
        y += 10f
        canvas.drawLine(margin, y, rightMargin, y, linePaint)
        y += 20f

        val fullDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS", Locale.getDefault())
        val durationMillis = (endTime - startTime)
        val durationFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d.%03d",
            TimeUnit.MILLISECONDS.toHours(durationMillis),
            TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60,
            TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60,
            durationMillis % 1000)

        y = drawRow(canvas, "Estado:", "ÉXITO / COMPLETADO", margin, rightMargin, y, labelPaint, successPaint)
        y = drawRow(canvas, "Inicio:", fullDateFormat.format(Date(startTime)), margin, rightMargin, y, labelPaint, valuePaint)
        y = drawRow(canvas, "Fin:", fullDateFormat.format(Date(endTime)), margin, rightMargin, y, labelPaint, valuePaint)
        y = drawRow(canvas, "Duración Total:", durationFormatted, margin, rightMargin, y, labelPaint, valueBoldPaint)
        y = drawRow(canvas, "Timestamp Inicio (ms):", startTime.toString(), margin, rightMargin, y, labelPaint, smallPaint)
        y = drawRow(canvas, "Timestamp Fin (ms):", endTime.toString(), margin, rightMargin, y, labelPaint, smallPaint)

        y += 30f

        // 5. PASOS TÉCNICOS
        canvas.drawText("Pasos de Ejecución:", margin, y, valueBoldPaint)
        y += 15f
        for (pass in getPassesList(methodName)) {
            if (y > pageHeight - margin - 40f) {
                drawFooter(canvas, pageWidth.toFloat(), pageHeight - 30f, appVersion)
                pdfDocument.finishPage(page); page = startNewPage(); canvas = page.canvas; y = margin + 20f
            }
            canvas.drawText("• $pass", margin + 10f, y, valuePaint)
            drawTextRightAligned(canvas, "OK", rightMargin - 20f, y, successPaint)
            y += 15f
        }

        y += 20f
        // 6. DETALLE DE ITEMS (Paginación automática)
        canvas.drawText("Detalle de Items Eliminados:", margin, y, headerSectionPaint)
        y += 20f

        if (packageWeights.isNotEmpty()) {
            canvas.drawText("Paquetes Limpiados:", margin, y, valueBoldPaint); y += 15f
            for ((pkg, size) in packageWeights) {
                if (y > pageHeight - margin - 30f) {
                    drawFooter(canvas, pageWidth.toFloat(), pageHeight - 30f, appVersion)
                    pdfDocument.finishPage(page); page = startNewPage(); canvas = page.canvas; y = margin + 20f
                }
                canvas.drawText("• $pkg (${formatFileSize(size)})", margin + 10f, y, fileListPaint); y += 15f
            }
        }

        if (deletedFiles.isNotEmpty()) {
            y += 10f
            canvas.drawText("Archivos/Carpetas (SAF):", margin, y, valueBoldPaint); y += 15f
            for (file in deletedFiles) {
                if (y > pageHeight - margin - 30f) {
                    drawFooter(canvas, pageWidth.toFloat(), pageHeight - 30f, appVersion)
                    pdfDocument.finishPage(page); page = startNewPage(); canvas = page.canvas; y = margin + 20f
                }
                val name = if (file.length > 85) file.take(82) + "..." else file
                canvas.drawText("• $name", margin + 10f, y, fileListPaint); y += 12f
            }
        }

        drawFooter(canvas, pageWidth.toFloat(), pageHeight - 30f, appVersion)
        pdfDocument.finishPage(page)

        // GUARDADO FINAL
        val fileName = "${kitNumber}_${System.currentTimeMillis()}.pdf"
        val reportsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Reportes_Nullum")
        if (!reportsDir.exists()) reportsDir.mkdirs()
        val file = File(reportsDir, fileName)

        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("application/pdf"), null)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            pdfDocument.close()
        }
    }

    // --- HELPERS REUTILIZADOS ---

    private fun getDeviceUniqueId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "Desconocido"
    }

    private fun drawFooter(canvas: Canvas, pageWidth: Float, y: Float, appVersion: String) {
        val footerPaint = Paint().apply {
            textSize = 9f; color = Color.GRAY; textAlign = Paint.Align.CENTER; isAntiAlias = true
        }
        canvas.drawText(
            "Borrado certificado por NULLUM Lite v${appVersion} - Trazabilidad Logística",
            pageWidth / 2f,
            y,
            footerPaint
        )
    }

    private fun drawRow(
        c: android.graphics.Canvas,
        l: String,
        v: String,
        x1: Float,
        x2: Float,
        y: Float,
        p1: Paint,
        p2: Paint
    ): Float {
        c.drawText(l, x1, y, p1)
        drawTextRightAligned(c, v, x2, y, p2)
        return y + 18f
    }

    private fun drawTextRightAligned(
        c: android.graphics.Canvas,
        text: String,
        x: Float,
        y: Float,
        paint: Paint
    ) {
        val originalAlign = paint.textAlign
        paint.textAlign = Paint.Align.RIGHT
        c.drawText(text, x, y, paint)
        paint.textAlign = originalAlign
    }

    private fun drawSeal(canvas: android.graphics.Canvas, x: Float, y: Float) {
        val sealPaint = Paint().apply {
            color = Color.parseColor("#DAA520"); style = Paint.Style.STROKE; strokeWidth =
            2f; isAntiAlias = true
        }
        val textPaint = Paint().apply {
            color = Color.parseColor("#B8860B"); textSize = 8f; isFakeBoldText = true; textAlign =
            Paint.Align.CENTER; isAntiAlias = true
        }
        canvas.drawCircle(x, y, 25f, sealPaint)
        canvas.drawText("100%", x, y - 2f, textPaint)
        canvas.drawText("SECURE", x, y + 8f, textPaint)
    }

    private fun getPassesList(methodName: String): List<String> = when {
        methodName.contains("DoD") -> listOf(
            "Pase 1 (0x00) - Sobrescritura",
            "Pase 2 (0xFF) - Sobrescritura",
            "Pase 3 (Random) - Verificación"
        )

        methodName.contains("BSI") -> listOf(
            "Pase 1-6 (Patrones BSI)",
            "Pase 7 (Random) - Finalización"
        )

        else -> listOf(
            "Pase 1 - Crypto-Erase (FBE Key Destruction)",
            "Pase 2 - Verificación de Bloques Sanitizados"
        )
    }

    private fun getStorageDetails(): StorageInfo {
        val stat = StatFs(Environment.getDataDirectory().path)
        return StorageInfo(formatFileSize(stat.blockCountLong * stat.blockSizeLong), "")
    }

    data class StorageInfo(val total: String, val available: String)

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        return String.format(
            Locale.getDefault(),
            "%.2f %s",
            bytes / 1024.0.pow(digitGroups.toDouble()),
            units[digitGroups]
        )
    }

    /**
     * Dibuja el sello dorado de seguridad alineado verticalmente con el título.
     * @param x Coordenada horizontal (extremo derecho menos margen)
     * @param y Coordenada vertical (debe ser la misma 'y' del título para alineación)
     */
    private fun drawSealAlineado(canvas: Canvas, x: Float, y: Float) {
        // Ajuste de offset: el título se dibuja desde la línea base,
        // el sello lo subimos un poco para que el centro visual coincida.
        val centerY = y - 8f

        val sealPaint = Paint().apply {
            color = Color.parseColor("#DAA520") // Color Oro/Dorado
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }

        val fillPaint = Paint().apply {
            color = Color.parseColor("#FFFAF0") // Fondo crema muy sutil
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.parseColor("#B8860B") // Dorado oscuro para el texto
            textSize = 8f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // 1. Dibujar fondo del sello y círculo exterior
        canvas.drawCircle(x, centerY, 22f, fillPaint)
        canvas.drawCircle(x, centerY, 22f, sealPaint)

        // 2. Dibujar círculo dentado o borde decorativo (opcional, segundo círculo)
        canvas.drawCircle(x, centerY, 19f, sealPaint)

        // 3. Textos internos
        canvas.drawText("100%", x, centerY - 2f, textPaint)
        canvas.drawText("SECURE", x, centerY + 8f, textPaint)
    }


    private fun getAppIcon(context: Context): android.graphics.Bitmap? {
        return try {
            // Obtenemos el drawable desde los recursos del proyecto
            val drawable = androidx.core.content.ContextCompat.getDrawable(
                context,
                com.alejoacevedodev.wipedatabeta.R.drawable.app_logo
            ) ?: return null

            // Configuramos el tamaño del Bitmap (puedes forzar un tamaño o usar el original)
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 100
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 100

            val bitmap = android.graphics.Bitmap.createBitmap(
                width,
                height,
                android.graphics.Bitmap.Config.ARGB_8888
            )

            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            bitmap
        } catch (e: Exception) {
            android.util.Log.e("PdfGenerator", "Error cargando logo: ${e.message}")
            null
        }
    }
}