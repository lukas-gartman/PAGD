package com.example.pagdapp.di

import android.content.Context
import com.example.pagdapp.data.model.audioclassifier.YamnetClassifier
import com.example.pagdapp.data.model.audioclassifier.PAGDClassifier
import com.example.pagdapp.data.model.audioclassifier.IAudioClassifier
import com.example.pagdapp.utils.Constants
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.channels.FileChannel
import javax.inject.Named
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AudioClassificationModule {


    @Provides
    @Singleton
    fun provideTfLiteInitializationOptions(): TfLiteInitializationOptions {
        return TfLiteInitializationOptions.builder()
            .setEnableGpuDelegateSupport(true)
            .build()
    }

    @Provides
    @Singleton
    @Named("SignatureInterpreter")
    fun provideSignatureInterpreter(@ApplicationContext context: Context): Interpreter {
        /* Load model */
        val fileDescriptor = context.assets.openFd("signature_model.tflite");
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor);
        val fileChannel = inputStream.channel;
        val startOffset = fileDescriptor.startOffset;
        val declaredLength = fileDescriptor.declaredLength;
        val filemap =  fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        return Interpreter(filemap)
    }

    @Provides
    @Singleton
    @Named("ModelInterpreter")
    fun provideModelInterpreter(@ApplicationContext context: Context): Interpreter {
        /* Load model */
        val fileDescriptor2 = context.assets.openFd("modelBest.tflite");
        val inputStream2 = FileInputStream(fileDescriptor2.fileDescriptor);
        val fileChannel2 = inputStream2.channel;
        val startOffset2 = fileDescriptor2.startOffset;
        val declaredLength2 = fileDescriptor2.declaredLength;
        val filemap2 =  fileChannel2.map(FileChannel.MapMode.READ_ONLY, startOffset2, declaredLength2);
        return Interpreter(filemap2)
    }

    @Provides
    @Singleton
    @Named("PAGDClassifier")
    fun provideAudioProcessor(
        @ApplicationContext context: Context,
        @Named("SignatureInterpreter") signature_interpreter: Interpreter,
        @Named("ModelInterpreter") model_interpreter: Interpreter
    ): IAudioClassifier {
        return PAGDClassifier(context, signature_interpreter, model_interpreter)
    }

    @Provides
    @Singleton
    @Named("YamnetClassifier")
    fun provideAudioClassifier(@ApplicationContext context: Context): IAudioClassifier {
        return YamnetClassifier(context, Constants.AI_MODEL)
    }




}