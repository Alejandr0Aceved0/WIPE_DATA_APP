#!/bin/zsh

# CONFIGURACIÓN
ACTION_WIPE="com.alejoacevedodev.START_REMOTE_WIPE"

echo "===================================================="
echo "   NULLUM LITE - DISPARADOR DE FLUJO AUTOMÁTICO"
echo "===================================================="

devices=$(adb devices | grep -v "List" | awk '{print $1}' | grep . )

for SERIAL in $devices; do
    # Extraemos el Android ID de la consola para mandarlo a la App
    AID_ADB=$(adb -s $SERIAL shell settings get secure android_id | tr -d '\r')

    echo "[*] Ordenando inicio de proceso en: $SERIAL"

    # Enviamos el comando con un flag "auto_start" en true
    adb -s $SERIAL shell am broadcast -a $ACTION_WIPE \
        --es "android_id_console" "$AID_ADB" \
        --ez "auto_start" true

    echo "[OK] Señal enviada a $SERIAL. Revisa el equipo."
done

say "Órdenes de borrado enviadas"