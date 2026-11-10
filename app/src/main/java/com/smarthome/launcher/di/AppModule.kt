package com.smarthome.launcher.di

import android.content.Context
import com.smarthome.launcher.calendar.CalendarRepository
import com.smarthome.launcher.frigate.FrigateClient
import com.smarthome.launcher.homeassistant.HomeAssistantClient
import com.smarthome.launcher.screensaver.BingImageClient
import com.smarthome.launcher.settings.AppSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideAppSettings(@ApplicationContext context: Context): AppSettings {
        return AppSettings(context)
    }

    @Provides
    @Singleton
    fun provideCalendarRepository(@ApplicationContext context: Context): CalendarRepository {
        return CalendarRepository(context)
    }

    @Provides
    @Singleton
    fun provideHomeAssistantClient(okHttpClient: OkHttpClient): HomeAssistantClient {
        return HomeAssistantClient(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideFrigateClient(okHttpClient: OkHttpClient): FrigateClient {
        return FrigateClient(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideBingImageClient(okHttpClient: OkHttpClient): BingImageClient {
        return BingImageClient(okHttpClient)
    }
}
