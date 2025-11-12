package com.alejoacevedodev.wipe_data_beta.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.alejoacevedodev.wipe_data_beta.domain.model.WipeLog

/**
 * La base de datos principal de la aplicación.
 */
@Database(
    entities = [WipeLog::class], // 1. Lista todas las tablas (entidades)
    version = 1,                 // 2. Versión de la base de datos
    exportSchema = false         // 3. (No es necesario para el MVP)
)
@TypeConverters(Converters::class) // 4. Le dice a Room que use nuestros convertidores
abstract class AppDatabase : RoomDatabase() {

    // 5. Expone el DAO para que los repositorios lo usen
    abstract fun logDao(): LogDao
}