package com.alejoacevedodev.wipedatabeta.di

import android.content.Context
import androidx.room.Room
import com.alejoacevedodev.wipedatabeta.data.local.AppDatabase // <-- (Asegúrate de tener esta clase)
import com.alejoacevedodev.wipedatabeta.data.local.LogDao
import com.alejoacevedodev.wipedatabeta.data.repository.IWipeRepository
import com.alejoacevedodev.wipedatabeta.data.repository.LogRepositoryImpl
import com.alejoacevedodev.wipedatabeta.data.repository.WipeRepositoryImpl
import com.alejoacevedodev.wipedatabeta.domain.repository.ILogRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Esto hace que las dependencias vivan durante toda la app
object AppModule {

    // --- 1. Provee la Base de Datos (Room) ---
    @Provides
    @Singleton // Queremos una sola instancia de la DB
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "wipe_database.db" // Nombre del archivo de la DB
        ).build()
    }

    // --- 2. Provee el DAO ---
    @Provides
    @Singleton // El DAO también debe ser único
    fun provideLogDao(database: AppDatabase): LogDao {
        return database.logDao() // Le pide el DAO a la DB
    }

    // --- 3. Provee el WipeRepository ---
    // Usamos @Provides porque WipeRepositoryImpl necesita 'Context',
    // que debemos inyectar aquí.
    @Provides
    @Singleton
    fun provideWipeRepository(@ApplicationContext context: Context): IWipeRepository {
        return WipeRepositoryImpl(context)
    }
}

// --- 4. Módulo Aparte para "Binds" (Enlaces) ---
// Usamos @Binds para decirle a Hilt qué implementación
// usar para una interfaz. Es más eficiente que @Provides.

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLogRepository(
        logRepositoryImpl: LogRepositoryImpl
    ): ILogRepository

    // NOTA: No ponemos 'IWipeRepository' aquí porque ya lo
    // proveímos con la función @Provides en AppModule.
}