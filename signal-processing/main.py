import os
from scipy.io.wavfile import read
import numpy as np
from matplotlib import pyplot as plt
import tensorflow as tf

# import tensorflow_io as tfio

POS = './data/Gunshot_Sounds'
SAMSUNG_GLOCK45_FILE = '/Samsung_Edge_S7/Glock45_Samsung/SA_014A_S01.wav'
PARENT_DIR = './data/Gunshot_Sounds/Samsung_Edge_S7/Glock45_Samsung'
pos = tf.data.Dataset.list_files("./data/Gunshot_Sounds/Samsung_Edge_S7/Glock45_Samsung" + '\*.wav')
data = tf.data.Dataset.zip((pos, tf.data.Dataset.from_tensor_slices(tf.ones(len(pos)))))


# NEG = os.path.join('data', 'Parsed_Not_Capuchinbird_Clips')

# visual representation of each processing step
def demo():
    wave = load_wav_8k_mono(POS + SAMSUNG_GLOCK45_FILE)
    t = len(wave)
    plotWave(wave)
    wave = shortenWave(wave)  # Turns wave into 500ms
    print("Short wave:", wave)
    print("wave length:", len(wave))
    plotWave(wave)

    # numerically correct representation of spectrogram
    spectrogram = tf.signal.stft(wave, frame_length=512, frame_step=64, pad_end=True, window_fn=tf.signal.hann_window)
    spectrogram = tf.abs(spectrogram)
    plotSpectrogram(spectrogram, wave)
    # 2nd representation of spectrogram
    spectrogram = preprocess(wave)
    plotSpectrogram2(spectrogram)

def load_wav_8k_mono(fileName):
    samples = []
    contents = tf.io.read_file(fileName)
    wav, sampleRate = tf.audio.decode_wav(contents, desired_channels=1)  # Load audio
    wav = tf.squeeze(wav, axis=-1)  # Unpack arrays
    # Check for errors
    print("\nWav file", wav)
    if sampleRate != 48000:
        raise Exception("Sample rate not meeting the expected 48kHz requirement: ", sampleRate)
    # wav = tfio.audio.resample(wav, rate_in=44100, rate_out=16000)
    # Downsample from 48khz to 8khz for consistency
    for el in wav[::6]:
        samples.append(el)
    samples = np.array(samples, dtype="float32")
    print("\nSamples", samples)
    return samples


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


# Visualize the wave
def plotWave(wave):
    f = 8000
    t = len(wave) / f
    t = np.arange(0, t, step=1 / f)
    plt.plot(t, wave, linewidth=0.5)
    plt.xlabel('Seconds')
    plt.ylabel('Amplitude [A.U]')
    plt.show()


# Visualize the spectrogram
def plotSpectrogram(spectrogram, wave):
    spectrogram = tf.transpose(spectrogram)
    print("\n Spectrogram shape \n", spectrogram.shape)
    fd, td = spectrogram.shape
    t = np.arange(0, len(wave) / 8000, step=(len(wave) / 8000) / td)
    f = np.arange(0, 4000, 4000 / fd)
    plt.pcolormesh(t, f, spectrogram, shading='gouraud')
    plt.ylabel('Frequency [Hz]')
    plt.xlabel('Time [sec]')
    plt.show()


def plotSpectrogram2(spectrogram):
    plt.figure(figsize=(20, 30))
    plt.imshow(tf.transpose(spectrogram)[0])
    plt.show()


# Convert one wave into spectrogram, demo uses this
def preprocess(wave):
    spectrogram = tf.signal.stft(wave, frame_length=512, frame_step=64, pad_end=True, window_fn=tf.signal.hann_window)
    spectrogram = tf.abs(spectrogram)
    spectrogram = tf.expand_dims(spectrogram, axis=2)
    print(spectrogram, spectrogram.shape)
    return spectrogram

# Used to convert multiple waves into spectrograms successively
def preprocessMultiple(filepath_label_tuple, shorten):
    filepath, label = filepath_label_tuple
    print(filepath)
    wave = load_wav_8k_mono(filepath)
    if shorten:
        wave = shortenWave(wave)
    plotWave(wave)
    spectrogram = tf.signal.stft(wave, frame_length=512, frame_step=64, pad_end=True, window_fn=tf.signal.hann_window)
    spectrogram = tf.abs(spectrogram)
    spectrogram = tf.expand_dims(spectrogram, axis=2)
    return spectrogram, label





def collectPaths():
    # Collect all file paths for Samsung edgy s7 with gun type
    folders = os.listdir("./data/Gunshot_Sounds/Samsung_Edge_S7")
    paths = []
    for i in range(len(folders)):
        if folders[i] == "__MACOSX": continue  # Skip macosx data, cant read it on windows
        filePaths = os.listdir("./data/Gunshot_Sounds/Samsung_Edge_S7/" + folders[i])
        for filePath in filePaths:
            paths.append(("./data/Gunshot_Sounds/Samsung_Edge_S7/" + folders[i] + '/'+ filePath, folders[i]))
    return paths


# Press the green button in the gutter to run the script.
if __name__ == '__main__':
    # -- Uncomment to see the visual representation of each processing step
    DEMO = False
    if DEMO:
        demo()
    else:
        paths = collectPaths()
        print(paths)
        # Process all spectrograms in Samsung folder
        for i in range(len(paths)):
            spectrogram, label = preprocessMultiple(paths[i], True)
            plotSpectrogram2(spectrogram) # DEBUG

    # data.map(preprocess())
    # data = data.map(preprocess)
    # data = data.cache()