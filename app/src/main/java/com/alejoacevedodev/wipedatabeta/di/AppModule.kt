package com.alejoacevedodev.wipedatabeta.di

import android.content.Context
import androidx.room.Room
import com.alejoacevedodev.wipedatabeta.data.local.AppDatabase
import com.alejoacevedodev.wipedatabeta.data.local.LogDao
import com.alejoacevedodev.wipedatabeta.data.repository.LogRepositoryImpl
import com.alejoacevedodev.wipedatabeta.data.repository.WipeRepositoryImpl
import com.alejoacevedodev.wipedatabeta.domain.repository.ILogRepository
import com.alejoacevedodev.wipedatabeta.domain.repository.IWipeRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de Hilt encargado de proveer instancias de dependencias externas o que requieren
 * una configuración personalizada (como Room y Repositorios con dependencias de Contexto).
 * * Se instala en [SingletonComponent], lo que garantiza que las instancias vivan durante
 * todo el ciclo de vida de la aplicación.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provee la instancia única de la base de datos persistente de la aplicación.
     * @param context Contexto global de la aplicación provisto por Hilt.
     * @return Instancia de [AppDatabase] configurada con Room.
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "wipe_database.db"
        ).fallbackToDestructiveMigration() // Opcional: Maneja cambios en el esquema borrando la DB
            .build()
    }

    /**
     * Provee el Objeto de Acceso a Datos (DAO) para las operaciones de log.
     * @param database Instancia de la base de datos inyectada automáticamente.
     * @return El [LogDao] necesario para persistir el historial de borrado.
     */
    @Provides
    @Singleton
    fun provideLogDao(database: AppDatabase): LogDao {
        return database.logDao()
    }

    /**
     * Provee la implementación del repositorio de borrado.
     * Se usa @Provides aquí debido a que [WipeRepositoryImpl] requiere acceso directo al
     * ContentResolver a través del contexto de la aplicación.
     * * @param context Contexto de la aplicación para operaciones de archivos y SAF.
     */
    @Provides
    @Singleton
    fun provideWipeRepository(@ApplicationContext context: Context): IWipeRepository {
        return WipeRepositoryImpl(context)
    }
}

/**
 * Módulo encargado de vincular interfaces con sus implementaciones concretas.
 * * Se utiliza una clase abstracta y la anotación [Binds] por eficiencia, ya que Hilt
 * no necesita generar código para crear la instancia, sino que simplemente delega
 * la interfaz a la implementación que ya tiene un constructor inyectable.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Vincula la interfaz [ILogRepository] con su implementación [LogRepositoryImpl].
     * [LogRepositoryImpl] debe tener la anotación @Inject en su constructor para que esto funcione.
     */
    @Binds
    @Singleton
    abstract fun bindLogRepository(
        logRepositoryImpl: LogRepositoryImpl
    ): ILogRepository
}