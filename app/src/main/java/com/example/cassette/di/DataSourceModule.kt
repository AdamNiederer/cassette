package com.example.cassette.di

import android.content.Context
import androidx.room.Room
import com.example.cassette.data.db.CassetteDatabase
import com.example.cassette.data.db.TrackDao
import com.example.cassette.data.db.AlbumDao
import com.example.cassette.data.db.ConfigDao
import com.example.cassette.data.repositories.MusicRepository
import com.example.cassette.data.repositories.PlayerRepository
import com.example.cassette.data.repositories.ConfigRepository
import com.example.cassette.data.repositories.VolumeRepository
import com.example.cassette.data.sources.CacheDataSource
import com.example.cassette.data.sources.LocalMusicSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import androidx.media3.exoplayer.ExoPlayer
import javax.inject.Singleton

import androidx.work.WorkManager

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideLocalMusicSource(@ApplicationContext context: Context): LocalMusicSource {
        return LocalMusicSource(context)
    }

    @Provides
    @Singleton
    fun provideCacheDataSource(trackDao: TrackDao, albumDao: AlbumDao, configDao: ConfigDao): CacheDataSource {
        return CacheDataSource(trackDao, albumDao, configDao)
    }

     @Provides
     @Singleton
     fun provideExoPlayer(@ApplicationContext context: Context): ExoPlayer {
         return ExoPlayer.Builder(context).build()
     }
}

@Module
@InstallIn(SingletonComponent::class)
object MusicRepositoryModule {
    @Provides
    @Singleton
    fun provideMusicRepository(
        localMusicSource: LocalMusicSource,
        cacheDataSource: CacheDataSource,
        workManager: WorkManager
    ): MusicRepository {
        return MusicRepository(localMusicSource, cacheDataSource, workManager)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object PlayerRepositoryModule {
    @Provides
    @Singleton
    fun providePlayerRepository(
        @ApplicationContext context: Context,
        cacheDataSource: CacheDataSource,
        configRepository: ConfigRepository,
        exoPlayer: ExoPlayer
    ): PlayerRepository {
        return PlayerRepository(context, cacheDataSource, configRepository, exoPlayer)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object VolumeRepositoryModule {
    @Provides
    @Singleton
    fun provideVolumeRepository(
        @ApplicationContext context: Context
    ): VolumeRepository {
        return VolumeRepository(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CassetteDatabase {
        return Room.databaseBuilder(
            context,
            CassetteDatabase::class.java,
            "cassette_database"
        ).fallbackToDestructiveMigration(true).build()
    }

    @Provides
    @Singleton
    fun provideTrackDao(database: CassetteDatabase): TrackDao {
        return database.trackDao()
    }

    @Provides
    @Singleton
    fun provideAlbumDao(database: CassetteDatabase): AlbumDao {
        return database.albumDao()
    }

    @Provides
    @Singleton
    fun provideConfigDao(database: CassetteDatabase): ConfigDao {
        return database.configDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object ViewModelModule
