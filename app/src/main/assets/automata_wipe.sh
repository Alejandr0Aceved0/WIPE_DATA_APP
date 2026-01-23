#!/bin/zsh

# CONFIGURACIÓN
SHIZUKU_PKG="moe.shizuku.privileged.api"
ACTION_WIPE="com.alejoacevedodev.START_REMOTE_WIPE"

echo "===================================================="
echo "   NULLUM LITE - CONTROLADOR MAESTRO (MAC)"
echo "===================================================="

devices=$(adb devices | grep -v "List" | awk '{print $1}' | grep . )

if [ -z "$devices" ]; then
    echo "Error: No se detectaron dispositivos conectados."
    exit 1
fi

for SERIAL in $devices; do
    echo ""
    echo "===================================================="

    # 1. OBTENER NÚMERO DE KIT (Nombre Bluetooth)
    KIT_NUMBER=$(adb -s $SERIAL shell settings get global bluetooth_name | tr -d '\r')
    if [[ "$KIT_NUMBER" == "null" || -z "$KIT_NUMBER" ]]; then
        KIT_NUMBER=$(adb -s $SERIAL shell settings get secure bluetooth_name | tr -d '\r')
    fi

    # Respaldo si no hay nombre: usar el Serial
    if [[ "$KIT_NUMBER" == "null" || -z "$KIT_NUMBER" ]]; then
        KIT_NUMBER=$SERIAL
    fi

    # 2. EXTRACCIÓN DE METADATOS
    FABRICANTE_RAW=$(adb -s $SERIAL shell getprop ro.product.manufacturer | tr -d '\r')
    FABRICANTE=$(echo "$FABRICANTE_RAW" | tr '[:lower:]' '[:upper:]')
    MODELO=$(adb -s $SERIAL shell getprop ro.product.model | tr -d '\r')
    ANDROID_ID=$(adb -s $SERIAL shell settings get secure android_id | tr -d '\r')
    IMEI=$(adb -s $SERIAL shell "service call iphonesubinfo 1 | cut -d \"'\" -f2 | grep -v \"\.\" | tr -d \" \" | tr -d '\n'" 2>/dev/null)

    echo ">>> DISPOSITIVO: $SERIAL"
    echo "    FABRICANTE:  $FABRICANTE"
    echo "    MODELO:      $MODELO"
    echo "    ANDROID ID:  $ANDROID_ID"
    # Si después de la limpieza el IMEI sigue vacío, mostramos "Protegido"
        if [[ -z "$IMEI" ]]; then
            echo "    IMEI:        Protegido/Android 10+"
        else
            echo "    IMEI:        $IMEI"
        fi
        echo "    KIT_NUMBER:  $KIT_NUMBER"
        echo "----------------------------------------------------"

    # 3. LIMPIEZA
    echo "[*] Limpiando instalaciones previas..."
    REAL_PKG=$(adb -s $SERIAL shell pm list packages | grep wipedatabeta | cut -d':' -f2 | tr -d '\r')
    if [ ! -z "$REAL_PKG" ]; then
        adb -s $SERIAL shell am force-stop $REAL_PKG
        adb -s $SERIAL uninstall $REAL_PKG > /dev/null 2>&1
    fi
    adb -s $SERIAL shell pm trim-caches 999M > /dev/null 2>&1

    # 4. INSTALACIÓN
    echo "[*] Instalando APKs..."
    adb -s $SERIAL install -r -g nullum_lite.apk
    adb -s $SERIAL install -r -g shizuku.apk
    sleep 12

    # 5. REDETECCIÓN DE PAQUETE
    REAL_PKG=$(adb -s $SERIAL shell pm list packages | grep wipedatabeta | cut -d':' -f2 | tr -d '\r')
    echo "[*] Paquete activo: $REAL_PKG"

    # 6. INICIAR SHIZUKU
    echo "[*] Iniciando servidor Shizuku..."
    BASE_PATH=$(adb -s $SERIAL shell pm path $SHIZUKU_PKG | cut -d':' -f2 | sed 's/base.apk//g')
    SO_COMMAND="${BASE_PATH}lib/arm64/libshizuku.so"
    adb -s $SERIAL shell $SO_COMMAND > /dev/null 2>&1 &
    sleep 8

    # 7. DOBLE VINCULACIÓN
    for i in {1..2}; do
        echo "[*] Intento de vinculación de permisos #$i..."
        adb -s $SERIAL shell pm grant $REAL_PKG android.permission.INTERACT_ACROSS_USERS 2>/dev/null
        adb -s $SERIAL shell appops set $REAL_PKG MANAGE_EXTERNAL_STORAGE allow
        adb -s $SERIAL shell "am broadcast -a moe.shizuku.privileged.api.intent.action.SET_PERMISSION --es packageName $REAL_PKG --ei permission 1" > /dev/null 2>&1
        adb -s $SERIAL shell monkey -p $REAL_PKG -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1
        sleep 5
    done

    # 8. SEÑAL DE BORRADO (INCLUYENDO EL KIT_NUMBER)
    echo "[*] ENVIANDO ORDEN DE BORRADO REMOTO..."
    # Añadimos el "Extra" para que la App reciba el Kit Number
    adb -s $SERIAL shell am broadcast -a $ACTION_WIPE --es "kit_number" "$KIT_NUMBER"

    echo "[OK] Dispositivo $SERIAL procesado exitosamente."
    echo "===================================================="
done

echo ""
echo "PROCESO FINALIZADO."
say "Proceso terminado"