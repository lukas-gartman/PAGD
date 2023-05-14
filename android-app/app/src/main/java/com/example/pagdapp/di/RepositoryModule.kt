package com.example.pagdapp.di

import android.content.Context
import com.example.pagdapp.data.model.audioclassifier.IAudioClassifier
import com.example.pagdapp.data.repository.SharedRepository
import com.example.pagdapp.data.remote.api.IPAGDApi
import com.example.pagdapp.data.remote.TokenProvider
import com.example.pagdapp.data.remote.api.IGoogleApi
import com.example.pagdapp.data.repository.GoogleRepository
import com.example.pagdapp.data.repository.IGoogleRepository
import com.example.pagdapp.data.repository.IPAGDRepository
import com.example.pagdapp.data.repository.PAGDRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideSharedRepository(
        @ApplicationContext context: Context,
        @Named("PAGDClassifierModel5") audioClassifierPAGD_5: IAudioClassifier,
        @Named("PAGDClassifierModel8") audioClassifierPAGD_8: IAudioClassifier,
        @Named("PAGDClassifierModel9") audioClassifierPAGD_9: IAudioClassifier,
        @Named("YamnetClassifier") audioClassifierYamnet: IAudioClassifier
    ): SharedRepository {
        return SharedRepository(
            context,
            audioClassifierPAGD_5,
            audioClassifierPAGD_8,
            audioClassifierPAGD_9,
            audioClassifierYamnet
        )
    }

    @Provides
    @Singleton
    fun providesTokenProvider(
        @ApplicationContext context: Context,
        pagdApi: IPAGDApi
    ): TokenProvider {
        return TokenProvider(context, pagdApi)
    }

    @Provides
    @Singleton
    fun providePAGDRepository(pagdApi: IPAGDApi, tokenProvider: TokenProvider): IPAGDRepository {
        return PAGDRepository(pagdApi, tokenProvider)
    }

    @Provides
    @Singleton
    fun provideGoogleRepository(googleApi: IGoogleApi): IGoogleRepository {
        return GoogleRepository(googleApi)
    }
}