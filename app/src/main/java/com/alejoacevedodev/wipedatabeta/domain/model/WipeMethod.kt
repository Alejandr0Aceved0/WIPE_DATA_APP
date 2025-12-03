package com.alejoacevedodev.wipedatabeta.domain.model

enum class WipeMethod {
    DoD_5220_22_M, // Departamento de Defensa
    NIST_SP_800_88, // NIST (Borrado Criptográfico)
    BSI_TL_03423,    // BSI (Alemán)
    PM_CLEAR    // Método para limpieza con Shell (pm clear)
}