#!/bin/bash

# --- SOLUCIÓN PARA WINDOWS ---
export MSYS_NO_PATHCONV=1

# Definimos las rutas posibles en una lista
POSIBLES_RUTAS=(
    "/storage/emulated/0/Documents/Reportes_Nullum"
    "/storage/sdcard0/Documents/Reportes_Nullum"
)

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

    encontrado_en_dispositivo=0

    # Probamos cada ruta definida arriba
    for RUTA in "${POSIBLES_RUTAS[@]}"; do
        # Intentamos listar archivos en la ruta actual
        files=$(adb -s $SERIAL shell "ls $RUTA/*.pdf 2>/dev/null" | tr -d '\r')

        if [[ -n "$files" ]]; then
            # Procesamos los archivos encontrados
            for REMOTE_FILE in $files; do
                REMOTE_FILE=$(echo "$REMOTE_FILE" | xargs)

                if [[ -n "$REMOTE_FILE" && "$REMOTE_FILE" == *.pdf ]]; then
                    FILE_NAME=$(basename "$REMOTE_FILE")
                    echo "    [📥] Descargando ($RUTA): $FILE_NAME"

                    adb -s $SERIAL pull "$REMOTE_FILE" "$DESTINO_FINAL/" > /dev/null 2>&1

                    if [ $? -eq 0 ]; then
                        ((count++))
                        ((encontrado_en_dispositivo++))
                    fi
                fi
            done
        fi
    done

    if [ $encontrado_en_dispositivo -eq 0 ]; then
        echo "    ⚠️ No se encontraron archivos en ninguna de las rutas conocidas."
    fi
done

echo "===================================================="
if [ $count -gt 0 ]; then
    echo "✅ EXITOSO: Se recuperaron $count certificados."
    echo "📂 Ubicación: $DESTINO_FINAL"
    # Abrir carpeta en Windows
    explorer.exe "$(cygpath -w "$DESTINO_FINAL")"
else
    echo "⚠️ No se pudo recuperar ningún archivo."
fi