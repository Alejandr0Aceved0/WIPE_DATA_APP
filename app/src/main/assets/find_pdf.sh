#!/bin/bash

# --- SOLUCIÓN PARA WINDOWS ---
# Esto evita que Git Bash convierta las rutas de Android en rutas de Windows
export MSYS_NO_PATHCONV=1

REMOTE_PATH="/storage/sdcard0/Documents/Reportes_Nullum"
LOCAL_DIR="./CERTIFICADOS_RECUPERADOS"
FECHA_HOY=$(date +%Y-%m-%d)
DESTINO_FINAL="$LOCAL_DIR/$FECHA_HOY"

mkdir -p "$DESTINO_FINAL"

echo "===================================================="
echo "   NULLUM LITE - EXTRACTOR DE CERTIFICADOS"
echo "===================================================="

# Obtener dispositivos
devices=$(adb devices | grep -v "List" | awk '{print $1}' | grep .)

count=0

for SERIAL in $devices; do
    MODEL=$(adb -s $SERIAL shell getprop ro.product.model | tr -d '\r\n')
    echo "--- Escaneando: $MODEL [$SERIAL] ---"

    # Obtenemos los archivos limpiando los retornos de carro de Android
    files=$(adb -s $SERIAL shell "ls $REMOTE_PATH/*.pdf 2>/dev/null" | tr -d '\r')

    if [ -z "$files" ]; then
        echo "    ⚠️ No se encontraron archivos."
        continue
    fi

    # Usamos un bucle for simple para evitar subshells
    for REMOTE_FILE in $files; do
        # Limpiar cualquier espacio o caracter extraño
        REMOTE_FILE=$(echo "$REMOTE_FILE" | xargs)

        if [[ -n "$REMOTE_FILE" && "$REMOTE_FILE" == *.pdf ]]; then
            FILE_NAME=$(basename "$REMOTE_FILE")
            echo "    [📥] Descargando: $FILE_NAME"

            # El pull ahora recibirá la ruta pura de Android
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
    # Convertimos la ruta para que Windows Explorer la entienda
    explorer.exe "$(cygpath -w "$DESTINO_FINAL")"
else
    echo "⚠️ No se pudo recuperar ningún archivo."
fi