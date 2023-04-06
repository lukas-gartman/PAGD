import tensorflow as tf
# Define the model
model = tf.keras.Sequential(
    [
        tf.keras.layers.Conv2D(16, (3,3), activation='relu', input_shape=(256, 257, 1)),
        tf.keras.layers.Conv2D(16, (3,3), activation='relu'),
        tf.keras.layers.Flatten(),
        tf.keras.layers.Dense(128, activation='relu'),
        tf.keras.layers.Dense(1, activation='sigmoid')
    ]
)

model.compile(optimizer='adam', loss='binary_crossentropy', metrics=[tf.keras.metrics.Recall(),tf.keras.metrics.Precision()])

model.summary()