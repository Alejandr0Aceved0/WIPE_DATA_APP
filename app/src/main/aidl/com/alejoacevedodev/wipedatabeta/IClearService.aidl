// IClearService.aidl
package com.alejoacevedodev.wipedatabeta;

interface IClearService {
    /**
     * Ejecuta el comando 'pm clear' en el proceso privilegiado (UID 2000).
     * @param packageName El nombre del paquete a limpiar.
     * @return String El resultado de la operación (SUCCESS/FAILED/EXCEPTION).
     */
    String clearAppData(String packageName);
}