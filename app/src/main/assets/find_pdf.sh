#!/bin/zsh

# --- CONFIGURACIÓN ---
REMOTE_PATH="/sdcard/Documents/Reportes_Nullum"
LOCAL_DIR="./CERTIFICADOS_RECUPERADOS"
FECHA_HOY=$(date +%Y-%m-%d)
DESTINO_FINAL="$LOCAL_DIR/$FECHA_HOY"

echo "===================================================="
echo "   NULLUM LITE - EXTRACTOR DE CERTIFICADOS"
echo "===================================================="

# Crear carpetas locales
mkdir -p "$DESTINO_FINAL"

# Obtener dispositivos conectados
devices=$(adb devices | grep -v "List" | awk '{print $1}' | grep . )

if [ -z "$devices" ]; then
    echo "❌ Error: No se detectaron dispositivos por ADB."
    exit 1
fi

count=0

for SERIAL in $devices; do
    # Obtener nombre del modelo para el log
    MODEL=$(adb -s $SERIAL shell getprop ro.product.model | tr -d '\r')
    echo "--- Escaneando: $MODEL [$SERIAL] ---"

    # Buscamos los archivos y los procesamos línea por línea de forma compatible
    adb -s $SERIAL shell "ls $REMOTE_PATH/*.pdf 2>/dev/null" | while read -r REMOTE_FILE; do
        # Limpiar caracteres ocultos del nombre del archivo
        REMOTE_FILE=$(echo "$REMOTE_FILE" | tr -d '\r')

        if [[ -n "$REMOTE_FILE" ]]; then
            FILE_NAME=$(basename "$REMOTE_FILE")

            echo "    [📥] Descargando: $FILE_NAME"
            # Descargamos con comillas para evitar errores si hay espacios
            adb -s $SERIAL pull "$REMOTE_FILE" "$DESTINO_FINAL/" > /dev/null 2>&1

            if [ $? -eq 0 ]; then
                ((count++))
            fi
        fi
    done
done

echo "===================================================="
if [ $count -gt 0 ]; then
    echo "✅ EXITOSO: Se recuperaron $count certificados."
    echo "📂 Ubicación: $DESTINO_FINAL"
    # Abrir la carpeta en el Finder de Mac
    open "$DESTINO_FINAL"
    say "Certificados recuperados"
else
    echo "⚠️ No se encontró ningún archivo PDF en $REMOTE_PATH"
fi