#!/bin/bash
# 1. ESTO DEBE IR PRIMERO: Bloquea la conversión de rutas de Windows
export MSYS_NO_PATHCONV=1

SHIZUKU_PKG="moe.shizuku.privileged.api"
ACTION_WIPE="com.alejoacevedodev.START_REMOTE_WIPE"
APK_APP="nullum_lite.apk"
APK_SHIZUKU="shizuku.apk"

procesar_dispositivo() {
    local SERIAL=$1
    local API=$(adb -s $SERIAL shell getprop ro.build.version.sdk | tr -d '\r' | xargs)
    local MODEL=$(adb -s $SERIAL shell getprop ro.product.model | tr -d '\r')
    local DISPLAY_NAME="[$MODEL | API $API | $SERIAL]"

    echo "$DISPLAY_NAME >>> Iniciando..."

    # Instalación
    adb -s $SERIAL install -r -g $APK_APP > /dev/null 2>&1
    adb -s $SERIAL install -r -g $APK_SHIZUKU > /dev/null 2>&1

    # --- ACTIVACIÓN DE SHIZUKU ---
    # Intento A: Método oficial
    adb -s $SERIAL shell "sh /sdcard/Android/data/moe.shizuku.privileged.api/files/start.sh" > /dev/null 2>&1
    sleep 3

    # Intento B: Método Forzado para API 29 (El rescate)
    if ! adb -s $SERIAL shell "ps -A" | grep -q "shizuku"; then
        echo "$DISPLAY_NAME [!] Shizuku no inició. Forzando método manual..."
        ABI=$(adb -s $SERIAL shell getprop ro.product.cpu.abi | tr -d '\r')
        # Extraer el binario directamente de la APK instalada
        APK_PATH=$(adb -s $SERIAL shell pm path $SHIZUKU_PKG | cut -d':' -f2 | tr -d '\r')

        # Copiar y ejecutar desde una zona neutral (/data/local/tmp)
        adb -s $SERIAL shell "cp $APK_PATH /data/local/tmp/sh.zip"
        adb -s $SERIAL shell "unzip -o /data/local/tmp/sh.zip lib/$ABI/libshizuku.so -d /data/local/tmp/"
        adb -s $SERIAL shell "chmod 777 /data/local/tmp/lib/$ABI/libshizuku.so"
        adb -s $SERIAL shell "/data/local/tmp/lib/$ABI/libshizuku.so" > /dev/null 2>&1 &
        sleep 5
    fi

    # --- PERMISOS ---
    REAL_PKG="com.alejoacevedodev.wipedatabeta.debug"

    # Otorgar permisos según API
    if [ "$API" -ge 30 ]; then
        adb -s $SERIAL shell appops set $REAL_PKG MANAGE_EXTERNAL_STORAGE 0 > /dev/null 2>&1
    else
        # Para el IDEMIA API 29
        adb -s $SERIAL shell pm grant $REAL_PKG android.permission.WRITE_EXTERNAL_STORAGE > /dev/null 2>&1
        adb -s $SERIAL shell pm grant $REAL_PKG android.permission.READ_EXTERNAL_STORAGE > /dev/null 2>&1
    fi

    # Whitelist Shizuku (Indispensable)
    adb -s $SERIAL shell "am broadcast -a moe.shizuku.privileged.api.intent.action.SET_PERMISSION --es packageName $REAL_PKG --ei permission 1" > /dev/null 2>&1

    # Despertar la app antes del envío
    adb -s $SERIAL shell monkey -p $REAL_PKG -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1
    sleep 2

    # --- DISPARO ---
    if adb -s $SERIAL shell "ps -A" | grep -q "shizuku"; then
        echo "$DISPLAY_NAME [OK] Enviando orden..."
        adb -s $SERIAL shell am broadcast -a $ACTION_WIPE --ez "run_full_auto" true
        echo "$DISPLAY_NAME ✅ ÉXITO."
    else
        echo "$DISPLAY_NAME ❌ ERROR: Shizuku falló tras reintento."
    fi
}

# Ejecución paralela
devices=$(adb devices | grep -v "List" | awk '{print $1}' | grep . )
for SERIAL in $devices; do procesar_dispositivo "$SERIAL" & done
wait
echo "Proceso finalizado."