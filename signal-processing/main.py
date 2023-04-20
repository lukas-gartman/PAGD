import os
import numpy as np
from scipy.io.wavfile import read
from matplotlib import pyplot as plt
import tensorflow as tf
import json
from scipy.signal import butter, lfilter
from model import *
import librosa
import random


# Visualize the wave
def plotWave(wave, sampleRate):
    f = sampleRate
    t = len(wave) / f
    t = np.arange(0, t, step=1 / f)
    plt.plot(t, wave, linewidth=0.5)
    plt.xlabel('Seconds')
    plt.ylabel('Amplitude [A.U]')
    plt.show()


# Plot a spectrogram with accurate axis description
def plotSpectrogram(spectrogram, wave, sampleRate):
    spectrogram = tf.squeeze(spectrogram, axis=-1)  # Unpack unnecessary arrays
    spectrogram = tf.transpose(spectrogram)
    print("\n Spectrogram shape \n", spectrogram.shape)
    fd, td = spectrogram.shape
    t = np.arange(0, len(wave) / sampleRate, step=(len(wave) / sampleRate) / td)
    f = np.arange(0, sampleRate / 2, sampleRate / (fd * 2))
    plt.pcolormesh(t, f, spectrogram, shading='gouraud')
    plt.ylabel('Frequency [Hz]')
    plt.xlabel('Time [sec]')
    plt.colorbar()
    plt.show()


# Plot a spectrogram without accurate axis description
# The advantage of this is that you don't need wavelength or sampleRate to view the spectrogram
def plotSpectrogramSimple(spectrogram):
    plt.figure(figsize=(15, 15))
    # Turn the spectrogram 90 degrees
    plt.imshow(tf.transpose(spectrogram)[0])
    plt.show()


# Load an audio file into 16khz format
def load_wav_16k_mono(filePath):
    sampleRate, wav = read(filePath)  # Read file and get sample rate and audio data
    if wav.ndim == 1:  # Check if audio data is mono
        wav = np.stack((wav, wav), axis=-1)  # Convert mono to stereo
    wav_16k = []

    # Downsample 44.1khz with librosa
    if sampleRate == 44100:
        wav_16k, ignored = librosa.load(filePath, sr=16000)
        return np.array(wav_16k)

    # Select downsampling factor
    if sampleRate == 96000:
        downsampleFac = 6
    elif sampleRate == 48000:
        downsampleFac = 3
    elif sampleRate == 32000:
        downsampleFac = 2
    elif sampleRate == 16000:
        downsampleFac = 1
    else:
        # Uncomment to remove exceptions if you have gathered bad data
        # os.remove(filePath)
        # print("File removed:", filePath, "\nWith SampleRate = ", sampleRate)
        # return
        raise Exception("Sample rate not meeting the expected possible sample rates: ", sampleRate)
    for el in wav[::downsampleFac]:
        wav_16k.append(el[0])
    np.array(wav_16k)
    return wav_16k


# Load an audio file into 8khz format
def load_wav_8k_mono(filePath):
    sampleRate, wav = read(filePath)  # Read file and get sample rate and audio data
    if wav.ndim == 1:  # Check if audio data is mono
        wav = np.stack((wav, wav), axis=-1)  # Convert mono to stereo
    wav_8k = []

    # Down-sample 44.1khz with librosa (Be careful with how you down-sample)
    if sampleRate == 44100:
        wav_8k, ignored = librosa.load(filePath, sr=8000)
        return np.array(wav_8k)

    # Select down-sampling factor
    if sampleRate == 96000:
        downsampleFac = 12
    elif sampleRate == 48000:
        downsampleFac = 6
    elif sampleRate == 32000:
        downsampleFac = 4
    elif sampleRate == 16000:
        downsampleFac = 3
    elif sampleRate == 8000:
        downsampleFac = 1
    else:
        # Uncomment to remove exceptions if you have gathered bad data
        # os.remove(filePath)
        # print("File removed:", filePath, "\nWith SampleRate = ", sampleRate)
        # return
        raise Exception("Sample rate not meeting the expected possible sample rates: ", sampleRate)
    # Downsample to 8khz
    for el in wav[::downsampleFac]:
        wav_8k.append(el[0])
    np.array(wav_8k)
    return wav_8k


# Collect all file paths for a folder containing folders that in turn contain .wav files
def collectPaths(path):
    folders = os.listdir(path)
    paths = []
    for i in range(len(folders)):
        if folders[i] == "__MACOSX":
            continue  # Skip macosx data, cant read it on windows
        filePaths = os.listdir(path + "/" + folders[i])
        for filePath in filePaths:
            paths.append((path + "/" + folders[i] + '/' + filePath, folders[i], filePath))
    return paths


# Get a wave with 16khz or 8khz sample rate from a .wav file
def getWave(fileName, sampleRate):
    # Load audio as waveform
    if sampleRate == 16000:
        wave = load_wav_16k_mono(fileName)
    elif sampleRate == 8000:
        wave = load_wav_8k_mono(fileName)
    else:
        raise Exception("Bad sample rate, only 8khz and 16khz is supported")
    return wave


# Write a tensor list of spectrograms to a json file
def tensorWriteJSON(data, path):
    with open(path, 'w') as f:
        # Convert the tensor array to a numpy list so that it can be written as JSON
        data = np.array(data)
        data = data.tolist()
        json.dump(data, f)
        f.close()


# Read a json file as a tensor list of spectrograms
def tensorReadJSON(path):
    print(path)
    f = open(path, 'r')
    data = json.load(f)  # Parse data
    f.close()
    # Convert datatype to tensorflow
    data = tf.convert_to_tensor(data)
    data = tf.data.Dataset.from_tensor_slices(data)
    return data


def butter_bandpass_filter(data, lowcut, highcut, fs, order=5):
    nyq = 0.5 * fs
    low = lowcut / nyq
    high = highcut / nyq
    b, a = butter(order, [low, high], btype='band')
    y = lfilter(b, a, data)
    return y


# Find the index of the gunshot in a wave
def determineGunshotLocation(wave, sampleRate):
    # Filter wave between 1KHz and the nyquist frequency
    wave = butter_bandpass_filter(wave, 1000, sampleRate / 2 - 1, sampleRate, 5)
    # Short time fourier transform the wave
    spectrogram = tf.signal.stft(wave, frame_length=512, frame_step=64, pad_end=True, window_fn=tf.signal.hann_window)
    # Turn the complex-valued array into a real-valued array (Spectrogram)
    spectrogram = tf.abs(spectrogram)
    maxSum = 0
    maxIndex = 0
    # Find the approximate index of the maximum amplitude of the frequency
    for i in range(len(spectrogram)):
        frame = spectrogram[i]
        currentSum = 0
        for j in range(0, len(frame), 80):
            currentSum += frame[j]
        if currentSum > maxSum:
            maxSum = currentSum
            maxIndex = i
    # Return index multiplied by frame_step
    return maxIndex * 64


# Extract 250ms from a 8khz audio clip that centers on the gunshot
def shortenWave(wave, sampleRate):
    # Set length of wave to 2048 samples
    wave = preProcessWave2s(wave, sampleRate)
    # Find position of gunshot
    index = determineGunshotLocation(wave, sampleRate)
    # Set the start at 30ms before the max amplitude
    if index <= 240:
        index = 0
    else:
        index -= 240
    # Check if remaining samples are less than 4096 samples ~(0.5s)
    sampleAmount = 2048
    if len(wave) - index < 2048:
        sampleAmount = len(wave) - index
    # Return new wave consisting only of the gunshot
    return wave[:sampleAmount + index][index:]


# Cut the wave to the same length and pad out with zeroes (Length is arbitrarily chosen for now)
def preProcessWave2s(wave, sampleRate):
    if sampleRate == 8000:
        wave = wave[:16384]
        zero_padding = tf.zeros([16384] - tf.shape(wave), dtype=tf.float32)
        wave = tf.concat([zero_padding, wave], 0)
    else:
        wave = wave[:32786]
        zero_padding = tf.zeros([32786] - tf.shape(wave), dtype=tf.float32)
        wave = tf.concat([zero_padding, wave], 0)
    return wave


# Standardize length of wave
def preProcessWave0_25s(wave, sampleRate):
    if sampleRate == 8000:
        wave = wave[:2048]
        zero_padding = tf.zeros([2048] - tf.shape(wave), dtype=tf.float32)
        wave = tf.concat([zero_padding, wave], 0)
    else:
        wave = wave[:4096]
        zero_padding = tf.zeros([4096] - tf.shape(wave), dtype=tf.float32)
        wave = tf.concat([zero_padding, wave], 0)
    return wave


# Transform a wave into a spectrogram with dimensions (64,65,1)
def getSpectrogram(wave, sampleRate):
    # Bandpass filter wave between 1KHz and the nyquist frequency
    wave = butter_bandpass_filter(wave, 1000, int(sampleRate / 2) - 1, sampleRate, 5)
    # Short time fourier transform the wave
    spectrogram = tf.signal.stft(wave, frame_length=128, frame_step=32, pad_end=True, window_fn=tf.signal.hann_window)
    # Turn the complex-valued array into a real-valued array (Spectrogram)
    spectrogram = tf.abs(spectrogram)
    # Add a dimension to the spectrogram for the expected mono-channel audio
    spectrogram = tf.expand_dims(spectrogram, axis=2)
    # Normalize spectrogram in respect to max-value
    max_val = tf.reduce_max(spectrogram)
    if max_val.numpy() != 0:
        spectrogram /= max_val
    return spectrogram


# Divide each audio clip in folder into arrays of 0.25s long spectrograms
def processNegativeFolderFully(sampleRate: int, path: str, overWrite: bool):
    filePaths = os.listdir(path)
    for i in range(len(filePaths)):
        filepath = path + "/" + filePaths[i]  # Complete path to file
        fileName = filePaths[i]  # Name of the .wav file in the folder
        wave = getWave(filepath, sampleRate)  # Load file and get wave
        buffer = []

        if sampleRate == 8000:
            step = 2048  # 2 seconds 8khz
            destinationFolder = "./trainingDataNeg8khz"
        else:
            step = 4096  # 2 seconds 16khz
            destinationFolder = "./trainingDataNeg16khz"
        for i in range(0, len(wave), step):
            x = i
            buffer.append(wave[x:x + step])
        for i in range(len(buffer)):
            wave = buffer[i]  # Demo
            buffer[i] = preProcessWave0_25s(buffer[i], sampleRate)
            buffer[i] = getSpectrogram(buffer[i], sampleRate)  # Obtain the spectrogram
            plotSpectrogram(buffer[i], wave, sampleRate)  # Demo
        index = 0
        for i in range(0, len(buffer), 120):
            x = i
            destinationFilePath = destinationFolder + '/' + fileName[:-4] + str(index) + '.json'  # File written to
            index += 1
            if os.path.isfile(destinationFilePath) and not overWrite:
                continue
            tensorWriteJSON(buffer[x:x + 120], destinationFilePath)


# Select a folder where the sub-folders containing .wav files gets converted into arrays of 0.25s long spectrograms
def processFolder(sampleRate: int, path: str, negative: bool, debug: bool, overWrite: bool):
    filePaths = collectPaths(path)
    labelType = "Pos"
    if negative:
        labelType = "Neg"
    # Destination folder has path "./trainingData(Neg/Pos)(8/16)khz"
    destinationFolder = './trainingData' + labelType + str(int(sampleRate / 1000)) + 'khz'
    previousFileName = filePaths[0][1]  # Set first fileName
    length = len(filePaths)
    output = []
    filePath = ""

    # Process all files
    for i in range(length):
        path = filePaths[i][0]  # Complete path to file
        fileName = filePaths[i][1]  # Name of the weapon in the folder
        # fileName = paths[i][2] # Not used currently
        filePath = destinationFolder + '/' + previousFileName + '.json'  # File written to

        # Status update
        if fileName != previousFileName:
            print(str(int(i / length * 100)) + "%")

        # Overwrite protection
        if os.path.isfile(filePath) and not overWrite:
            previousFileName = fileName
            continue

        # Save file and continue
        if fileName != previousFileName:
            tensorWriteJSON(output, filePath)
            output = []
            previousFileName = fileName

        # Process the audio and generate the spectrogram
        wave = getWave(path, sampleRate)  # Load file and get wave
        if not negative:
            # If the data is positive then we expect to be able to extract exactly one gunshot
            wave = shortenWave(wave, sampleRate)
        wave = preProcessWave0_25s(wave, sampleRate)  # Correct the dimensions of the wave
        spectrogram = getSpectrogram(wave, sampleRate)  # Obtain the spectrogram

        # If debug is on it will plot the spectrograms instead of writing them to a file
        if debug:
            plotWave(wave, sampleRate)
            plotSpectrogram(spectrogram, wave, sampleRate)
            print(path)
            print("Wavelength:", len(wave))
            print("Current folder processed:", fileName, "\nSpectrogram shape:", spectrogram.shape,
                  "\nIndex processed:",
                  i)

        # Save the processed data as a json file
        else:
            output.append(spectrogram)
    if os.path.isfile(filePath) and not overWrite:
        return
    tensorWriteJSON(output, filePath)


# Function to help with concatenation of nonexistent datasets
def safeTensorConcatenate(dataset1, dataset2):
    if dataset1 is None:
        return dataset2
    if dataset2 is None:
        return dataset1
    return dataset1.concatenate(dataset2)


# Train a folder will all data from a positive and negative directory
def trainModel(positivePath="./trainingDataPos8khz", negativePath="./trainingDataNeg8khz"):
    # Get list of file paths from positive and negative folders, shuffle them
    posFilesPath = os.listdir(positivePath)
    random.shuffle(posFilesPath)
    negFilesPath = os.listdir(negativePath)
    random.shuffle(negFilesPath)
    paths = []

    # Combine file paths with their labels (1 for positive, 0 for negative) into a list
    for el in posFilesPath:
        paths.append((el, 1))
    for el in negFilesPath:
        paths.append((el, 0))
    random.shuffle(paths)

    trainingData = None
    testData = None

    # Split data into batches, with each batch containing batchSize files
    for pathLabelTuple in paths:
        path = pathLabelTuple[0]
        label = pathLabelTuple[1]
        if label == 1:
            # Load and preprocess positive files
            file = tensorReadJSON(positivePath + '/' + path)
            # Label them with 1
            file = tf.data.Dataset.zip((file, tf.data.Dataset.from_tensor_slices(tf.ones(len(file)))))
            # Add some files to test data (for evaluation purposes)
            test_len = int(len(file) * 0.05)
            testData = safeTensorConcatenate(testData, file.take(test_len))
            # Add remaining files to training data
            trainingData = safeTensorConcatenate(trainingData, file.skip(test_len).take(len(file) - test_len))
        else:
            # Load negative files
            file = tensorReadJSON(negativePath + '/' + path)
            # Label them with 0
            file = tf.data.Dataset.zip((file, tf.data.Dataset.from_tensor_slices(tf.zeros(len(file)))))
            # Add some files to test data (for evaluation purposes)
            test_len = int(len(file) * 0.05)
            testData = safeTensorConcatenate(testData, file.take(test_len))
            # Add remaining files to training data
            trainingData = safeTensorConcatenate(trainingData, file.skip(test_len).take(len(file) - test_len))

        # Preprocess and batch the test data
    testData = testData.cache()
    testData = testData.batch(40)
    testData = testData.prefetch(20)

    # Preprocess, shuffle and batch the training data
    trainingData = trainingData.cache()
    trainingData = trainingData.shuffle(buffer_size=len(trainingData))
    trainingData = trainingData.batch(40)
    trainingData = trainingData.prefetch(20)

    # Define class weights (adjust as needed)
    # We weight them differently since we have more negative data,
    # and a false-positive are worse than false-negative
    class_weight = {0: 3, 1: 1}
    # Train the model for one epoch with the current batch
    model.fit(trainingData, epochs=4, class_weight=class_weight)  # , validation_data=testData)
    # Save the trained model
    model.save("AI_PAGD")

    # Evaluate the model on the test data
    results = model.evaluate(testData)
    print("Results:", results)

    # Make predictions on some additional data and print the results
    '''posData, negData = predict("./Evaluation_data/pos", "./Evaluation_data/Clapping_hands")
    predictions = model.predict(posData).flatten()
    recall = 0
    for el in predictions:
        recall += el
    recall /= len(predictions)
    print("Handguns")
    print("\nPositive Predictions -- Recall:", recall)
    print("Predictions:", predictions)

    print("\nClapping hands")
    predictions = model.predict(negData).flatten()
    precision = 0
    for el in predictions:
        precision += el
    precision = 1 - precision / len(predictions)
    print("\nNegative Predictions -- Precision:", precision)
    print("Predictions:", predictions)'''


def predict(posFolderPath, negFolderPath):
    posFilePaths = os.listdir(posFolderPath)
    negFilePaths = os.listdir(negFolderPath)
    posData = None
    negData = None
    for filePath in posFilePaths:
        file = tensorReadJSON(posFolderPath + '/' + filePath)
        file = tf.data.Dataset.zip((file, tf.data.Dataset.from_tensor_slices(tf.ones(len(file)))))
        posData = safeTensorConcatenate(posData, file.take(len(file)))
    for filePath in negFilePaths:
        file = tensorReadJSON(negFolderPath + '/' + filePath)
        file = tf.data.Dataset.zip((file, tf.data.Dataset.from_tensor_slices(tf.zeros(len(file)))))
        negData = safeTensorConcatenate(negData, file.take(len(file)))
    posData = posData.cache()
    posData = posData.batch(len(posData))
    posData = posData.prefetch(10)
    negData = negData.cache()
    negData = negData.batch(len(negData))
    negData = negData.prefetch(10)
    return posData, negData


def convertAI(input_path='./AI_PAGD'):
    converter = tf.lite.TFLiteConverter.from_saved_model(input_path)
    tflite_model = converter.convert()

    # Save the converted model
    with open('model.tflite', 'wb') as f:
        f.write(tflite_model)
    f.close()


def processAllData():
    processFolder(8000, "./data/Gunshot_Sounds/Iphone_7", False, True, False)
    #processFolder(8000, "./data/Gunshot_Sounds/Samsung_Edge_S7", False, False, False)
    #processFolder(8000, "./data/Gunshot_Sounds/Zoom_H4N_Handheld_Recording_Device", False, True, False)
    #processFolder(8000, "./data/Gunshot_Sounds_Own_Recordings/Samsung_Edge_S6", False, False, False)
    #processFolder(8000, "./data/Gunshot_Sounds_Own_Recordings/Samsung_Galaxy_S20", False, False, False)
    #processNegativeFolderFully(8000,"./data/Negative_Data/Own_Recordings",True)


# Press the green button in the gutter to run the script.
if __name__ == '__main__':
    processAllData()
    #trainModel("./trainingDataPos8khz", "./trainingDataNeg8khz")
    #convertAI()
    pass
