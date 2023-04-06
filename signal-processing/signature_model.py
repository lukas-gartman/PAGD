import tensorflow as tf

class Model(tf.Module):
    @tf.function(input_signature=[tf.TensorSpec(shape=[None], dtype=tf.float32, name="wave")])
    def spectrogram(self, wave):
        spectrogram = tf.signal.stft(wave, frame_length=512, frame_step=64, pad_end=True, window_fn=tf.signal.hann_window)
        # Turn the complex-valued array into a real-valued array (Spectrogram)
        spectrogram = tf.abs(spectrogram)
        # Add a dimension to the spectrogram for the expected mono-channel audio
        spectrogram = tf.expand_dims(spectrogram, axis=2)
        return {"spectrogram": spectrogram}
    
SAVED_MODEL_PATH = ".\signature_model"

model = Model()

tf.saved_model.save(model, SAVED_MODEL_PATH,
    signatures={
      'spectrogram': model.spectrogram.get_concrete_function()
    })

converter = tf.lite.TFLiteConverter.from_saved_model(SAVED_MODEL_PATH)
converter.target_spec.supported_ops = [
    tf.lite.OpsSet.TFLITE_BUILTINS,  # enable TensorFlow Lite ops.
    tf.lite.OpsSet.SELECT_TF_OPS  # enable TensorFlow ops.
]
tflite_model = converter.convert()

# Print the signatures from the converted model
interpreter = tf.lite.Interpreter(model_content=tflite_model)
signatures = interpreter.get_signature_list()
print(signatures)

with open('signature_model.tflite', 'wb') as f:
    f.write(tflite_model)
f.close()