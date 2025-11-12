package com.alejoacevedodev.wipe_data_beta.data.local

import androidx.room.TypeConverter
import com.alejoacevedodev.wipe_data_beta.domain.model.WipeMethod

/**
 * Le dice a Room cómo convertir tipos de datos que no entiende.
 */
class Converters {

    @TypeConverter
    fun fromWipeMethod(method: WipeMethod): String {
        // Convierte el enum (ej. WipeMethod.NIST_SP_800_88)
        // a un String (ej. "NIST_SP_800_88")
        return method.name
    }

    @TypeConverter
    fun toWipeMethod(value: String): WipeMethod {
        // Convierte el String de la DB (ej. "NIST_SP_800_88")
        // de vuelta al objeto enum (WipeMethod.NIST_SP_800_88)
        return WipeMethod.valueOf(value)
    }
}