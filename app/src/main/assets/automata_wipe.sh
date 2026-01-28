#!/bin/bash

# --- SOLUCIÓN PARA WINDOWS (Evita errores de ruta C:/Program...) ---
export MSYS_NO_PATHCONV=1

# --- CONFIGURACIÓN ---
SHIZUKU_PKG="moe.shizuku.privileged.api"
ACTION_WIPE="com.alejoacevedodev.START_REMOTE_WIPE"
APK_APP="nullum_lite.apk"
APK_SHIZUKU="shizuku.apk"

# Función que procesa cada dispositivo
procesar_dispositivo() {
    local SERIAL=$1

    # EXTRAER INFO PARA DISPLAY
    local MODEL=$(adb -s $SERIAL shell getprop ro.product.model | tr -d '\r')
    local BRAND=$(adb -s $SERIAL shell getprop ro.product.manufacturer | tr -d '\r')
    local API=$(adb -s $SERIAL shell getprop ro.build.version.sdk | tr -d '\r' | xargs)
    local DISPLAY_NAME="[$BRAND $MODEL | API $API | $SERIAL]"

    echo "$DISPLAY_NAME >>> Iniciando proceso..."

    # 1. METADATOS
    KIT_NUMBER=$(adb -s $SERIAL shell settings get global bluetooth_name | tr -d '\r')
    [[ "$KIT_NUMBER" == "null" || -z "$KIT_NUMBER" ]] && KIT_NUMBER=$(adb -s $SERIAL shell settings get secure bluetooth_name | tr -d '\r')
    [[ "$KIT_NUMBER" == "null" || -z "$KIT_NUMBER" ]] && KIT_NUMBER=$SERIAL
    ANDROID_ID=$(adb -s $SERIAL shell settings get secure android_id | tr -d '\r')

    # 2. DETECTAR ARQUITECTURA
    ABI=$(adb -s $SERIAL shell getprop ro.product.cpu.abi | tr -d '\r')
    [[ "$ABI" == *"arm64"* ]] && LIB_DIR="arm64" || LIB_DIR="arm"

    # 3. INSTALACIÓN Y CONFIGURACIÓN
    shizuku_activo=false
    for intento in 1 2; do
        [[ "$shizuku_activo" == "true" ]] && break

        echo "$DISPLAY_NAME [*] Intento $intento: Instalando y configurando..."

        # Reinstalación limpia
        REAL_PKG=$(adb -s $SERIAL shell pm list packages | grep wipedatabeta | head -n 1 | cut -d':' -f2 | tr -d '\r')
        [[ -n "$REAL_PKG" ]] && adb -s $SERIAL uninstall $REAL_PKG > /dev/null 2>&1

        adb -s $SERIAL install -r -g $APK_APP > /dev/null 2>&1
        adb -s $SERIAL install -r -g $APK_SHIZUKU > /dev/null 2>&1
        sleep 4

        # INICIAR SHIZUKU (Método Híbrido: start.sh + .so)
        # Intentamos el método oficial primero (mejor para API 29)
        adb -s $SERIAL shell "sh /sdcard/Android/data/moe.shizuku.privileged.api/files/start.sh" > /dev/null 2>&1

        # Lanzamos el .so de respaldo
        BASE_PATH=$(adb -s $SERIAL shell pm path $SHIZUKU_PKG | head -n 1 | cut -d':' -f2 | sed 's/base.apk//g' | tr -d '\r')
        adb -s $SERIAL shell "${BASE_PATH}lib/${LIB_DIR}/libshizuku.so" > /dev/null 2>&1 &
        sleep 8

        # PERMISOS AUTOMÁTICOS
        REAL_PKG=$(adb -s $SERIAL shell pm list packages | grep wipedatabeta | head -n 1 | cut -d':' -f2 | tr -d '\r')

        adb -s $SERIAL shell pm grant $REAL_PKG android.permission.INTERACT_ACROSS_USERS 2>/dev/null

        # Validar si el comando appops es compatible (Solo API 30+)
        if [ "$API" -ge 30 ]; then
            adb -s $SERIAL shell appops set $REAL_PKG MANAGE_EXTERNAL_STORAGE allow
        fi

        # WHITELIST AUTOMÁTICA
        adb -s $SERIAL shell "am broadcast -a moe.shizuku.privileged.api.intent.action.SET_PERMISSION --es packageName $REAL_PKG --ei permission 1" > /dev/null 2>&1

        # Lanzar App y esperar vinculación
        adb -s $SERIAL shell monkey -p $REAL_PKG -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1
        sleep 5

        # VALIDACIÓN DE ESTADO
        if adb -s $SERIAL shell "ps -A" | grep -q "shizuku"; then
            shizuku_activo=true
        fi
    done

    # 4. DISPARO FINAL
    if [[ "$shizuku_activo" == "true" ]]; then
        echo "$DISPLAY_NAME [OK] Shizuku activo. ENVIANDO ORDEN..."
        adb -s $SERIAL shell am broadcast -a $ACTION_WIPE \
            --es "kit_number" "$KIT_NUMBER" \
            --es "android_id_adb" "$ANDROID_ID" \
            --ez "run_full_auto" true
        echo "$DISPLAY_NAME ✅ PROCESADO CON ÉXITO."
    else
        echo "$DISPLAY_NAME ❌ ERROR CRÍTICO: Shizuku falló."
    fi
}

echo "===================================================="
echo "   NULLUM LITE - MODO PARALELO (STREAMS ACTIVOS)"
echo "===================================================="

devices=$(adb devices | grep -v "List" | awk '{print $1}' | grep . )

for SERIAL in $devices; do
    procesar_dispositivo "$SERIAL" &
done

wait
echo "===================================================="
echo "PROCESO FINALIZADO EN TODOS LOS EQUIPOS."
# Notificación sonora compatible con Windows
echo -e "\a"