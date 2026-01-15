#!/bin/bash

# CONFIGURACIÓN
SHIZUKU_PKG="moe.shizuku.privileged.api"
ACTION_WIPE="com.alejoacevedodev.START_REMOTE_WIPE"

echo "===================================================="
echo "   NULLUM LITE - AUTOMATIZACIÓN CON REINTENTO"
echo "===================================================="

# Obtener lista de dispositivos
devices=$(adb devices | grep -v "List" | awk '{print $1}' | grep . )

for SERIAL in $devices; do
    echo ""
    echo ">>> PROCESANDO DISPOSITIVO: $SERIAL"

    # 1. Instalación
    echo "[*] Instalando APKs..."
    adb -s $SERIAL install -r -g nullum_lite.apk
    adb -s $SERIAL install -r -g shizuku.apk

    echo "[*] Esperando registro inicial (5s)..."
    sleep 5

    # 2. Detección Dinámica del Paquete
    REAL_PKG=$(adb -s $SERIAL shell pm list packages | grep wipedatabeta | cut -d':' -f2 | tr -d '\r')
    echo "[*] Paquete detectado: $REAL_PKG"

    # 3. Iniciar servidor Shizuku (Nativo)
    echo "[*] Iniciando servidor Shizuku..."
    BASE_PATH=$(adb -s $SERIAL shell pm path $SHIZUKU_PKG | cut -d':' -f2 | sed 's/base.apk//g')
    SO_COMMAND="${BASE_PATH}lib/arm64/libshizuku.so"
    adb -s $SERIAL shell $SO_COMMAND > /dev/null 2>&1 &

    # 4. CICLO DE DOBLE INTENTO PARA PERMISOS Y VINCULACIÓN
    for i in {1..2}; do
        echo "[*] Intento de configuración #$i..."

        # Permisos de sistema
        adb -s $SERIAL shell appops set $REAL_PKG MANAGE_EXTERNAL_STORAGE allow
        adb -s $SERIAL shell appops set $REAL_PKG REQUEST_INSTALL_PACKAGES allow

        # Permiso de vinculación Shizuku
        adb -s $SERIAL shell pm grant $REAL_PKG android.permission.INTERACT_ACROSS_USERS 2>/dev/null

        # Forzar autorización en la base de datos de Shizuku
        adb -s $SERIAL shell "am broadcast -a moe.shizuku.privileged.api.intent.action.SET_PERMISSION --es packageName $REAL_PKG --ei permission 1" > /dev/null 2>&1

        # Abrir la app (esto ayuda a que el servicio se registre en el segundo intento)
        adb -s $SERIAL shell monkey -p $REAL_PKG -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1

        echo "[*] Esperando entre intentos..."
        sleep 4
    done

    # 5. LANZAR BORRADO FINAL
    echo "[*] Enviando señal de borrado remoto..."
    adb -s $SERIAL shell am broadcast -a $ACTION_WIPE

    echo "[OK] Dispositivo $SERIAL completado tras ciclo de reintento."
done

echo ""
echo "PROCESO FINALIZADO"