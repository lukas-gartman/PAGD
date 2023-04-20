## Requirements
numpy, matplotlib, json, scipy, and tensorflow are required.
```
pip install numpy matplotlib json skipy tensorflow 
```
## How to use
### Plotting
**plotWave()** - Display a wave using getWave()

**plotSpectrogram()** - Display a spectrogram with correctly labeled axis by supplying a spectrogram, wave and its sampleRate

**plotSpectrogramSimple()** - Display a spectrogram by supplying a spectrogram
### Helper functions
**load_wav_16k_mono()** - Accepts 48khz and 16khz .wav files and loads them as a wave with 16k sampleRate

**load_wav_8k_mono()** - Accepts 48khz, 16khz, 8khz .wav files and loads them as a wave with 8k sampleRate

**shortenWave()** - Used for optimization, it truncates a supplied wave to only contain the gunshot in a 250ms window 

**collectPaths()** - Get all filepaths of a folder containing sub-folders with audio samples

### Core functionality
**processFolder()** - Generate trainingdata and output them to a predefined folder as JSON files containing an array of Tensorflow spectrograms of shape (64,65,1)
Possible sample rates: 8000, 16000

**trainModel()** - Use a folder with positive and a folder with negative training data to train a ML model

(Use tensorflow library to load all files in batches : Not implemented)

### Training a model 
0. Make sure you have the following folder hierarchy before proceeding to the next step
`- main.py
- data
    - Gunshot_Sounds
        - deviceTypes (Several folders)
            - WeaponTypes (Several folders)
                - .wav files
- trainingDataPos8khz
    - Positive training data in .json files
- trainingDataNeg8khz
    - Negative training data in .json files   `
_(replace 8 with 16 if you wish you train on data with 16khz samplerate)_
1. Process the positive and negative audio you wish to use with **processFolder()**
2. train the model with a specified model with **trainModel()**
