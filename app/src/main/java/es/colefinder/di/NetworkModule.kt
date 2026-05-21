package es.colefinder.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import es.colefinder.data.Supabase
import es.colefinder.data.repository.ColegioRepository
import es.colefinder.data.repository.SupabaseColegioRepository
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return Supabase.client
    }
}

/**
 * Módulo separado para bindings de repositorios.
 * Permite sustituir SupabaseColegioRepository por una implementación fake en tests.
 *
 * [es.colefinder.data.repository.AppConfigRepository] se registra por constructor
 * (`@Inject` + `@Singleton`), igual que [es.colefinder.data.repository.UserPreferencesRepository].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindColegioRepository(
        impl: SupabaseColegioRepository
    ): ColegioRepository
}
