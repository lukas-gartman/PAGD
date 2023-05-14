package com.example.pagdapp.di

import android.content.Context
import com.example.pagdapp.data.model.audioclassifier.*
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
    @Named("ModelInterpreter_5")
    fun provideModelInterpreterModel5(@ApplicationContext context: Context): Interpreter {
        /* Load model */
        val fileDescriptor2 = context.assets.openFd("model5.tflite");
        val inputStream2 = FileInputStream(fileDescriptor2.fileDescriptor);
        val fileChannel2 = inputStream2.channel;
        val startOffset2 = fileDescriptor2.startOffset;
        val declaredLength2 = fileDescriptor2.declaredLength;
        val filemap2 =  fileChannel2.map(FileChannel.MapMode.READ_ONLY, startOffset2, declaredLength2);
        return Interpreter(filemap2)
    }
    @Provides
    @Singleton
    @Named("ModelInterpreter_8")
    fun provideModelInterpreterModel8(@ApplicationContext context: Context): Interpreter {
        /* Load model */
        val fileDescriptor2 = context.assets.openFd("model8.tflite");
        val inputStream2 = FileInputStream(fileDescriptor2.fileDescriptor);
        val fileChannel2 = inputStream2.channel;
        val startOffset2 = fileDescriptor2.startOffset;
        val declaredLength2 = fileDescriptor2.declaredLength;
        val filemap2 =  fileChannel2.map(FileChannel.MapMode.READ_ONLY, startOffset2, declaredLength2);
        return Interpreter(filemap2)
    }
    @Provides
    @Singleton
    @Named("ModelInterpreter_9")
    fun provideModelInterpreterModel9(@ApplicationContext context: Context): Interpreter {
        /* Load model */
        val fileDescriptor2 = context.assets.openFd("model9.tflite");
        val inputStream2 = FileInputStream(fileDescriptor2.fileDescriptor);
        val fileChannel2 = inputStream2.channel;
        val startOffset2 = fileDescriptor2.startOffset;
        val declaredLength2 = fileDescriptor2.declaredLength;
        val filemap2 =  fileChannel2.map(FileChannel.MapMode.READ_ONLY, startOffset2, declaredLength2);
        return Interpreter(filemap2)
    }

    @Provides
    @Singleton
    @Named("PAGDClassifierModel5")
    fun providePAGDClassifierModel5(
        @ApplicationContext context: Context,
        @Named("SignatureInterpreter") signature_interpreter: Interpreter,
        @Named("ModelInterpreter_5") model_interpreter: Interpreter
    ): IAudioClassifier {
        return PAGDClassifierModel5(context, signature_interpreter, model_interpreter)
    }
    @Provides
    @Singleton
    @Named("PAGDClassifierModel8")
    fun providePAGDClassifierModel8(
        @ApplicationContext context: Context,
        @Named("SignatureInterpreter") signature_interpreter: Interpreter,
        @Named("ModelInterpreter_8") model_interpreter: Interpreter
    ): IAudioClassifier {
        return PAGDClassifierModel8(context, signature_interpreter, model_interpreter)
    }
    @Provides
    @Singleton
    @Named("PAGDClassifierModel9")
    fun providePAGDClassifierModel9(
        @ApplicationContext context: Context,
        @Named("SignatureInterpreter") signature_interpreter: Interpreter,
        @Named("ModelInterpreter_9") model_interpreter: Interpreter
    ): IAudioClassifier {
        return PAGDClassifierModel9(context, signature_interpreter, model_interpreter)
    }

    @Provides
    @Singleton
    @Named("YamnetClassifier")
    fun provideAudioClassifier(@ApplicationContext context: Context): IAudioClassifier {
        return YamnetClassifier(context, Constants.AI_MODEL)
    }




}