import os
import numpy as np
from matplotlib import pyplot as plt
import tensorflow as tf

# import tensorflow_io as tfio

POS = './data/Gunshot_Sounds'
DEMO_FILE = '/Samsung_Edge_S7/BoltAction22_Samsung/SA_004B_S02.wav'
PARENT_DIR = './data/Gunshot_Sounds/Samsung_Edge_S7/Glock45_Samsung'
pos = tf.data.Dataset.list_files("./data/Gunshot_Sounds/Samsung_Edge_S7/Glock45_Samsung" + '\*.wav')
data = tf.data.Dataset.zip((pos, tf.data.Dataset.from_tensor_slices(tf.ones(len(pos)))))


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
    plt.show()


# Plot a spectrogram without accurate axis description
# The advantage of this is that you don't need wavelength or sampleRate to view the spectrogram
def plotSpectrogramSimple(spectrogram):
    plt.figure(figsize=(15, 15))
    # Turn the spectrogram 90 degrees
    plt.imshow(tf.transpose(spectrogram)[0])
    plt.show()


# Load an audio file into 16khz format
def load_wav_16k_mono(fileName):
    contents = tf.io.read_file(fileName)  # Read file
    wav, sampleRate = tf.audio.decode_wav(contents, desired_channels=1)  # Decode audio
    wav = tf.squeeze(wav, axis=-1)  # Unpack unnecessary arrays
    wav_16k = []
    # Down-sample to 16khz if required
    if sampleRate == 48000:
        for el in wav[::3]:
            wav_16k.append(el)
        np.array(wav_16k)
    elif sampleRate == 16000:
        wav_16k = wav
    else:
        raise Exception("Sample rate not meeting the expected possible sample rates: ", sampleRate)
    return wav_16k


# Load an audio file into 8khz format
def load_wav_8k_mono(fileName):
    contents = tf.io.read_file(fileName)  # Read file
    wav, sampleRate = tf.audio.decode_wav(contents, desired_channels=1)  # Decode audio
    wav = tf.squeeze(wav, axis=-1)  # Unpack unnecessary arrays
    wav_8k = []
    # Down-sample to 16khz if required
    if sampleRate == 48000:
        for el in wav[::6]:
            wav_8k.append(el)
        np.array(wav_8k)
    elif sampleRate == 16000:
        for el in wav[::3]:
            wav_8k.append(el)
        np.array(wav_8k)
    elif sampleRate == 8000:
        wav_8k = wav
    else:
        raise Exception("Sample rate not meeting the expected possible sample rates: ", sampleRate)
    return wav_8k


# Extract 500ms of 8khz audio clip that only contains the gunshot
def shortenWave(wave):
    # Check if wave is long enough (0.5s) for shorten to be applied
    if len(wave) < 4096:
        raise Exception("Wave length is smaller than required 4096 samples: ", len(wave))
    # Find max amplitude
    index = 0
    for i in range(len(wave)):
        if wave[i] >= wave[index]:
            index = i
    # Set the start at 100ms before the max amplitude
    if index < 800:
        index = 0
    else:
        index -= 800
    # Check if remaining samples are less than 4096 samples ~(0.5s)
    sampleAmount = 4096
    if len(wave) - index < 4096:
        sampleAmount = len(wave) - index
    # Create new wave consisting only of the gunshot
    newWave = wave[:sampleAmount + index][index:]
    return newWave


# Collect all file paths for Samsung edgy s7 with gun type
def collectPaths():
    folders = os.listdir("./data/Gunshot_Sounds/Samsung_Edge_S7")
    paths = []
    for i in range(len(folders)):
        if folders[i] == "__MACOSX": continue  # Skip macosx data, cant read it on windows
        filePaths = os.listdir("./data/Gunshot_Sounds/Samsung_Edge_S7/" + folders[i])
        for filePath in filePaths:
            paths.append(("./data/Gunshot_Sounds/Samsung_Edge_S7/" + folders[i] + '/' + filePath, folders[i]))
    return paths


# Get a wave with 16khz or 8khz sample rate from a .wav file
def getWave(fileName, sampleRate):
    # Load audio as waveform
    wave = []
    if sampleRate == 16000:
        wave = load_wav_16k_mono(fileName)
    elif sampleRate == 8000:
        wave = load_wav_8k_mono(fileName)
    else:
        raise Exception("Bad sample rate, only 8khz and 16khz is supported")
    return wave


# Transform a wave into a spectrogram
def getSpectrogram(wave):
    # Short time fourier transform the wave
    spectrogram = tf.signal.stft(wave, frame_length=512, frame_step=64, pad_end=True, window_fn=tf.signal.hann_window)
    # Turn the complex matrix into a real-valued array (Spectrogram)
    spectrogram = tf.abs(spectrogram)
    # Add a dimension to the spectrogram for the expected mono-channel audio
    spectrogram = tf.expand_dims(spectrogram, axis=2)
    return spectrogram


# Cut the wave to the same length and pad out with zeroes (Length is arbitrarily chosen for now)
def preProcessWave(wave, sampleRate):
    if sampleRate == 8000:
        wave = wave[:16000]
        zero_padding = tf.zeros([16000] - tf.shape(wave), dtype=tf.float32)
        wave = tf.concat([zero_padding, wave], 0)
    else:
        wave = wave[:32000]
        zero_padding = tf.zeros([32000] - tf.shape(wave), dtype=tf.float32)
        wave = tf.concat([zero_padding, wave], 0)
    return wave


# Process all spectrograms in Samsung folder
def processFolder(sampleRate, debug):
    paths = collectPaths()
    for i in range(len(paths)):
        label = paths[i][1]  # Name of the weapon in the folder
        path = paths[i][0]  # Path to file
        wave = getWave(path, sampleRate)  # Load file and get wave
        wave = preProcessWave(wave, sampleRate)  # Correct the dimensions of the wave
        spectrogram = getSpectrogram(wave)  # Obtain the spectrogram

        if debug:  # Check on the progress of the processing
            plotWave(wave, sampleRate)
            plotSpectrogram(spectrogram, wave, sampleRate)
            print(path)
            print("Wavelength:", len(wave))
            print("Current folder processed:", label, "\nSpectrogram shape:", spectrogram.shape, "\nIndex processed:",
                  i)


# Press the green button in the gutter to run the script.
if __name__ == '__main__':
    # Turn an entire folder into spectrograms (very slow)
    processFolder(8000, True)
