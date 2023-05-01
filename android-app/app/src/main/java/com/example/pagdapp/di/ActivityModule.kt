package com.example.pagdapp.di

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.example.pagdapp.utils.PermissionHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
object ActivityModule {


    @Provides
    @ActivityScoped
    fun providePermissionHandler(@ActivityContext context: Context): PermissionHandler {
        return PermissionHandler(context as AppCompatActivity)
    }


}