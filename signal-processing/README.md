## Requirements
numpy, matplotlib, json, scipy, and tensorflow are required.
```
pip install numpy matplotlib json skipy tensorflow 
```
## How to use
### Plotting
**plotWave()** - Display a wave using getWave()

**plotSpectrogram()** - Display a spectrogram with correctly labeled axis by supplying a spectrogram, wave and its sampleRate

plotSpectrogramSimple() - Display a spectrogram by supplying a spectrogram
### Helper functions
**load_wav_16k_mono()** - Accepts 48khz and 16khz .wav files and loads them as a wave with 16k sampleRate

**load_wav_8k_mono()** - Accepts 48khz, 16khz, 8khz .wav files and loads them as a wave with 8k sampleRate

**shortenWave()** - Used for optimization, it truncates a supplied wave to only contain the gunshot in a 500ms window (TO DO)

**collectPaths()** - Get all filepaths of a folder containing sub-folders with audio samples
### Core functionality
**processFolder()** - Generate trainingdata and output them to a predefined folder
Possible sample rates: 8000, 16000

**trainModel()** - Use a folder with positive and a folder with negative training data to train a trainModel

Change batchSize to increase/decrease the number of .json files loaded per iteration of training, make sure you have enough ram to load the number of training data files = batchSize
### Training a model 
0. Make sure you have the following folder hierarchy before proceeding to the next step
`- main.py
- data
    - audioSourceType1 #can have several here
        - deviceType1 #can have several here
            - WeaponType1
                - .wav files
- trainingDataPos8khz
    - Positive training data in .json files
- trainingDataNeg8khz
    - Negative training data in .json files   `
_(replace 8 with 16 if you wish you train on data with 16khz samplerate)_
1. Process the positive and negative audio you wish to use with **processFolder()**
2. train the model with a specified model with **trainModel()**
