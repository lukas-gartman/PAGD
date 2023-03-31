import tensorflow as tf
import numpy as np
import json
import os


def tensorReadJSON(path):
    with open(path, 'r') as f:
        data = json.load(f)
    # Convert to FLOAT32 data type
    data = tf.cast(data, dtype=tf.float32)
    return data


# Load the TFLite model and allocate tensors.
interpreter = tf.lite.Interpreter(model_path="model.tflite2")
interpreter.allocate_tensors()

# Get input and output tensors.
input_details = interpreter.get_input_details()
#print(input_details)
output_details = interpreter.get_output_details()
print(output_details)
positivePath = './testdata'
result = []
def test():
    posFilesPath = os.listdir('./testdata')
    # Define a test input.
    for i in range(10):
        test_input = tensorReadJSON(positivePath + '/' + posFilesPath[i])
        for j in range(len(test_input)):
            test = tf.expand_dims(test_input[j], axis=0)
            # Set the input tensor.
            interpreter.set_tensor(input_details[0]['index'], test)
            # Run inference.
            interpreter.invoke()
            # Get the output tensor and print the result.
            output_data = interpreter.get_tensor(output_details[0]['index'])
            result.append(output_data[0][0])
    print(sum(result)/len(result))
    
    
if __name__ == '__main__':
    test()
