import librosa
import os.path
import os
import argparse
import tensorflow as tf
import matplotlib.pyplot as plt
import numpy as np

# Parameters
SAMPLE_RATE = 16384
FRAME_SECONDS = 2.0

# Mel frequency cepstrum coefficients parameters
WINDOW_SIZE = 2048
NUMBER_MELS = 128
NUMBER_COEFFICIENTS = 20
MIN_FREQ = 1
MAX_FREQ = SAMPLE_RATE/2

def get_paths(path):
    if path is None:
        return []
    paths = []
    if (os.path.isfile(path)):
        if os.path.splitext(path)[1] == ".wav":
            paths.append(path)
    elif (os.path.isdir(path)):
        for dirpath, _, filenames in os.walk(path, topdown=False):
            for filename in filenames:
                filepath = os.path.join(dirpath, filename)
                if os.path.splitext(filepath)[1] == ".wav":
                    paths.append(filepath)
    return paths

def process(path):
    signal,_ = librosa.load(path, sr=SAMPLE_RATE, mono=True, duration=FRAME_SECONDS)
    if len(signal) < SAMPLE_RATE*FRAME_SECONDS:
        signal = np.concatenate((signal, [0] * int(SAMPLE_RATE*FRAME_SECONDS-len(signal))))
    assert len(signal) == SAMPLE_RATE*FRAME_SECONDS

    mfccs = librosa.feature.mfcc(y=signal, sr=SAMPLE_RATE, n_mfcc=NUMBER_COEFFICIENTS, 
                                    n_fft=WINDOW_SIZE,hop_length=WINDOW_SIZE//2,
                                    n_mels=NUMBER_MELS, fmin=MIN_FREQ, fmax=MAX_FREQ)
    
    #librosa.display.specshow(mfccs, x_axis='time')
    #plt.show() 
    print(".", end='', flush=True)

    return mfccs


def train(positive=None, negative=None, modelpath="model", epochs=1):
    positive = get_paths(positive)
    negative = get_paths(negative)

    print("Loading model ... ", end='', flush=True)
    try:
        model = tf.keras.models.load_model(modelpath)
    except OSError:
        raise ValueError("path to model is invalid (it is not a keras model): " + modelpath)
    print("Done!")

    data = []
    val = []

    if len(positive) > 0:
        print("Processing gunshot sound files ...", end='', flush=True)
        data.extend([process(path) for path in positive])
        val.extend([1.0] * len(positive))
        print(" Done!")

    if len(negative) > 0:
        print("Processing non-gunshot sound files ...", end='', flush=True)
        data.extend([process(path) for path in negative])
        val.extend([0.0] * len(negative))
        print(" Done!")

    hist = model.fit(tf.convert_to_tensor(data), tf.convert_to_tensor(val), epochs=epochs)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
                    prog = 'training_mfcc',
                    description = 'Trains tensorflow model with .wav file(s)')
    parser.add_argument('-p', '--positive', default=None, help="paths to gunshot(s) to train model on")
    parser.add_argument('-n', '--negative', default=None, help="paths to non-gunshot(s) to train model on")
    parser.add_argument('-m', '--model', default="model", help="path to saved model")
    parser.add_argument('-e', '--epochs', default=1, type=int, help="specifies amount of epochs to run model through")
    args = parser.parse_args()

    if args.positive is None and args.negative is None:
        raise ValueError("no paths to train on specified")
    if args.positive is not None and not os.path.exists(args.positive):
        raise ValueError("path to gunshots is invalid")
    if args.negative is not None and not os.path.exists(args.negative):
        raise ValueError("path to non-gunshots is invalid")
    if not os.path.exists(args.model):
        raise ValueError("path to saved model is invalid: " + args.model)

    train(positive=args.positive, negative=args.negative, modelpath=args.model, epochs=args.epochs)
