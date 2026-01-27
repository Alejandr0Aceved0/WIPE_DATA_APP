#!/bin/zsh

# --- CONFIGURACIÓN ---
SHIZUKU_PKG="moe.shizuku.privileged.api"
ACTION_WIPE="com.alejoacevedodev.START_REMOTE_WIPE"
APK_APP="nullum_lite.apk"
APK_SHIZUKU="shizuku.apk"

# Función que procesa cada dispositivo
procesar_dispositivo() {
    local SERIAL=$1

    # EXTRAER INFO PARA DISPLAY (Igual que Android Studio)
    local MODEL=$(adb -s $SERIAL shell getprop ro.product.model | tr -d '\r')
    local BRAND=$(adb -s $SERIAL shell getprop ro.product.manufacturer | tr -d '\r')
    local API=$(adb -s $SERIAL shell getprop ro.build.version.sdk | tr -d '\r')
    local DISPLAY_NAME="[$BRAND $MODEL | API $API | $SERIAL]"

    echo "$DISPLAY_NAME >>> Iniciando proceso..."

    # 1. METADATOS
    KIT_NUMBER=$(adb -s $SERIAL shell settings get global bluetooth_name | tr -d '\r')
    [[ "$KIT_NUMBER" == "null" || -z "$KIT_NUMBER" ]] && KIT_NUMBER=$(adb -s $SERIAL shell settings get secure bluetooth_name | tr -d '\r')
    [[ "$KIT_NUMBER" == "null" || -z "$KIT_NUMBER" ]] && KIT_NUMBER=$SERIAL
    ANDROID_ID=$(adb -s $SERIAL shell settings get secure android_id | tr -d '\r')

    # 2. DETECTAR ARQUITECTURA
    ABI=$(adb -s $SERIAL shell getprop ro.product.cpu.abi | tr -d '\r')
    if [[ "$ABI" == *"arm64"* ]]; then LIB_DIR="arm64"; else LIB_DIR="arm"; fi

    # 3. INSTALACIÓN Y CONFIGURACIÓN
    shizuku_activo=false
    for intento in 1 2; do
        [[ "$shizuku_activo" == "true" ]] && break

        echo "$DISPLAY_NAME [*] Intento $intento: Instalando y configurando..."

        # Reinstalación limpia
        REAL_PKG=$(adb -s $SERIAL shell pm list packages | grep wipedatabeta | cut -d':' -f2 | tr -d '\r')
        [[ -n "$REAL_PKG" ]] && adb -s $SERIAL uninstall $REAL_PKG > /dev/null 2>&1

        adb -s $SERIAL install -r -g $APK_APP > /dev/null 2>&1
        adb -s $SERIAL install -r -g $APK_SHIZUKU > /dev/null 2>&1
        sleep 5

        # INICIAR SHIZUKU
        BASE_PATH=$(adb -s $SERIAL shell pm path $SHIZUKU_PKG | head -n 1 | cut -d':' -f2 | sed 's/base.apk//g')
        SO_COMMAND="${BASE_PATH}lib/${LIB_DIR}/libshizuku.so"

        adb -s $SERIAL shell "$SO_COMMAND" > /dev/null 2>&1 &
        sleep 8

        # PERMISOS
        REAL_PKG=$(adb -s $SERIAL shell pm list packages | grep wipedatabeta | cut -d':' -f2 | tr -d '\r')
        adb -s $SERIAL shell pm grant $REAL_PKG android.permission.INTERACT_ACROSS_USERS 2>/dev/null
        adb -s $SERIAL shell appops set $REAL_PKG MANAGE_EXTERNAL_STORAGE allow
        adb -s $SERIAL shell "am broadcast -a moe.shizuku.privileged.api.intent.action.SET_PERMISSION --es packageName $REAL_PKG --ei permission 1" > /dev/null 2>&1
        adb -s $SERIAL shell monkey -p $REAL_PKG -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1
        sleep 5

        # VALIDACIÓN (Corregida)
        SH_CHECK=$(adb -s $SERIAL shell "ps -A | grep shizuku | grep -v grep")
        if [[ -n "$SH_CHECK" ]]; then
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
say "Borrado terminado en todos los dispositivos"