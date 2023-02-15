# import scipy as scipy

# from scipy.io.wavfile import read
# from scipy.signal import stft
import matplotlib.pyplot as plt
import numpy as np
# This is a sample Python script.

# Press Shift+F10 to execute it or replace it with your code.
# Press Double Shift to search everywhere for classes, files, tool windows, actions, and settings.



def print_hi():
    # Demo
    soundFile = read("./Audio/SA_075B_S04.wav") # You can import any wav file
    samples = soundFile[1]  # 48k samples * ~2s due to 48khz sample rate
    print(samples)
    buffer = []

    # Downsample from 48khz to 8khz
    for el in samples[::6]:  # Sample every 48khz/8khz'th element
        if (el[0] != el[1]):  # The same amplitudes appears twice and are identical
            print("NONMATCH")
        buffer.append(el[0])  # Only keep one of the two identical amplitudes
    bufferArray = np.array(buffer) # Turn list into array for type match in spectrogram

    # nperseg is frame size which we perform fourier transform on
    # noverlap is amount of samples we jump between frame transformations
    # higher nperseg = higher frequency resolution, but lower time resolution
    f, t, Sxx = scipy.signal.spectrogram(bufferArray, fs=8000, nperseg=256, noverlap=128)
    plt.pcolormesh(t, f, Sxx, shading='gouraud')
    plt.ylabel('Frequency [Hz]')
    plt.xlabel('Time [sec]')
    plt.show()

    # If you want several spectrograms side-by-side, check documentation and see bottom of block comment
    # for an example

    # Messy Research nedanför ifall du är intresserad
    """soundFile = read("./Audio/SA_075B_S04.wav")
    soundFile2 = read("./Audio/SA_014A_S01.wav")
    samples = soundFile[1] # 48k samples * ~2s
    samples2 = soundFile2[1]


    print(samples)
    buffer = []
    buffer16k = []
    buffer2 = []
    for el in samples[::6]:  # Sample every 48khz/8khz'th element
        if (el[0] != el[1]):  # The same amplitudes appears twice
            print("NONMATCH")
        buffer.append(el[0])  # Only keep one of the two identical amplitudes
    for el in samples2[::6]:  # Sample every 48khz/8khz'th element
        if (el[0] != el[1]):  # The same amplitudes appears twice
            print("NONMATCH")
        buffer2.append(el[0])  # Only keep one of the two identical amplitudes
    for el in samples[::3]: # Sample every 48khz/8khz'th element
        if(el[0] != el[1]): # The same amplitudes appears twice
            print("NONMATCH")
        buffer16k.append(el[0]) # Only keep one of the two identical amplitudes
    print(buffer)
    bufferSize = len(buffer)
    print("Signal length")
    print(bufferSize)

    # Visualize waveform
    l = np.arange(0,bufferSize/8000, step=1/8000)
    #plt.plot(l,buffer,linewidth=0.5)
    #plt.xlabel("Seconds")
    #plt.ylabel("Amplitude")
    #plt.show()
    # Window signal
    taperedSignal = scipy.signal.convolve(buffer,np.hanning(8000)) # Convolve with hanning window of equal sampling
    print("Tapered length")
    print(len(taperedSignal))
    #plt.plot(np.arange(0,len(taperedSignal)/8000,step=1/8000),taperedSignal,linewidth=0.5)
    #plt.xlabel("Seconds")
    #plt.ylabel("Amplitude")
    #plt.show()
    # Frequency spectrum
    bufferFT = fft(buffer)

    hanningFT = fft(taperedSignal)

    #fig,ax = plt.subplots()
    #ax.plot(np.linspace(0,8000/2,len(bufferFT)//2), 2.0/len(bufferFT) * np.abs(bufferFT[:len(bufferFT)//2]))
    #plt.xlabel("Hz")
    #plt.ylabel("AU")
    #plt.show() # bufferFT

    #fig, ax = plt.subplots()
    #ax.plot(np.linspace(0, 8000 / 2, len(hanningFT) // 2), 2.0 / len(hanningFT) * np.abs(hanningFT[:len(hanningFT) // 2]))
    #plt.show() # hanningFT

    # Window size is the amount of samples we apply windowing to
    # Frame size is the number of samples we consider in each signal
    # Frame size and window size usually coincide
    # Hop size H tells us how many samples we slide to the right with the window
    # Testing frame size of 2^10 = 1024 which is a frame duration of 128ms
    # Also overlap is a 4th of that of frame size (256)
    # Larger frame size makes freq resolution larger, smaller makes time resolution bigger. I think frequency resolution
    # of higher importance to us since time resolution is more important when you need to know better WHEN something happened
    # We use the hanning window as opposed to a rectangular function since rectangular functions introduce discontinuties
    bufferSTFT = stft(buffer, fs=8000.0, window="hann", nperseg=1024, noverlap=256)
    freqBins = len(bufferSTFT[0]) # Frequency bins = Framesize/2 + 1
    nFrames = len(bufferSTFT[1]) # Number of frames = (samples - Framesize)/hopsize + 1
    # Calculating the spectrogram
    y_scale = np.abs(bufferSTFT[2])**2
    #print(y_scale)
    fs = 10e3
    N = 1e5
    amp = 2 * np.sqrt(2)
    noise_power = 0.01 * fs / 2
    time = np.arange(N) / float(fs)
    mod = 500 * np.cos(2 * np.pi * 0.25 * time)
    carrier = amp * np.sin(2 * np.pi * 3e3 * time + mod)
    noise = np.random.default_rng().normal(scale=np.sqrt(noise_power), size=time.shape)
    noise *= np.exp(-time / 5)
    x = carrier + noise
    print("Buffer")
    print(buffer)
    print("X")
    print(x)
    bufferArray = np.array(buffer)
    buffer16kArray = np.array(buffer16k)
    buffer2Array = np.array(buffer2)
    print("bufferArray")
    print(bufferArray)
    #f, t, Sxx = scipy.signal.spectrogram(x, fs)
    f, t, Sxx = scipy.signal.spectrogram(bufferArray, fs=8000,nperseg=256,noverlap=128)
    plt.subplot(1, 2, 1)
    plt.pcolormesh(t, f, Sxx, shading='gouraud')
    plt.ylabel('Frequency [Hz]')
    plt.xlabel('Time [sec]')
    #f, t, Sxx = scipy.signal.spectrogram(buffer16kArray, fs=16000, nperseg=512, noverlap=128) 16khz
    f, t, Sxx = scipy.signal.spectrogram(buffer2Array, fs=8000, nperseg=256, noverlap=128) #sound sample 2
    plt.subplot(1, 2, 2)
    plt.pcolormesh(t, f, Sxx, shading='gouraud')
    plt.ylabel('Frequency [Hz]')
    plt.xlabel('Time [sec]')
    plt.show()"""



print_hi()