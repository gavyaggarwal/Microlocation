/*
 * Copyright 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


#ifndef NATIVE_AUDIO_AUDIO_COMMON_H
#define NATIVE_AUDIO_AUDIO_COMMON_H


#include <complex>
#include <math.h>
#include <iostream>
#include <valarray>
#include <time.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

#include "android_debug.h"
#include "debug_utils.h"
#include "buf_manager.h"
#include "../../../../../../../../../../Android/sdk/ndk-bundle/platforms/android-21/arch-x86/usr/include/jni.h"

/*
 * Audio Sample Controls...
 */
#define AUDIO_SAMPLE_CHANNELS               1

/*
 * Sample Buffer Controls...
 */
#define PLAY_KICKSTART_BUFFER_COUNT         3
#define DEVICE_SHADOW_BUFFER_QUEUE_LEN      4

#define NUM_RECORDING_BUFS 4
const double PI = 3.141592653589793238460;

typedef std::complex<double> Complex;
typedef std::valarray<Complex> CArray;


struct SharedData {
    bool        play;
    bool        waitingForSelf;
    bool        waitingForOther;
    double      startTime;
    double      endTime;
    double      selfLatency;
    uint32_t    bufferSize;
    bool        isEchoer;
    JavaVM*     jvm;
    jobject     clas;
};

struct SampleFormat {
    uint32_t   sampleRate_;
    uint16_t   channels_;
    uint16_t   pcmFormat_;          //8 bit, 16 bit, 24 bit ...
    uint32_t   representation_;     //android extensions
};




extern double now_us(void);

extern void ConvertToSLSampleFormat(SLAndroidDataFormat_PCM_EX *pFormat,
                                    SampleFormat* format);


#define SLASSERT(x)   do {\
    assert(SL_RESULT_SUCCESS == (x));\
    (void) (x);\
    } while (0)

/*
 * Interface for player and recorder to communicate with engine
 */
#define ENGINE_SERVICE_MSG_KICKSTART_PLAYER    1
#define ENGINE_SERVICE_MSG_RETRIEVE_DUMP_BUFS  2
typedef bool (*ENGINE_CALLBACK)(void* pCTX, uint32_t msg, void* pData);





template <class NumberFormat, size_t DFT_Length>
class SlidingDFT
{
private:
    /// Are the frequency domain values valid? (i.e. have at elast DFT_Length data
    /// points been seen?)
    bool data_valid = false;

    /// Time domain samples are stored in this circular buffer.
    NumberFormat x[DFT_Length] = { 0 };

    /// Index of the next item in the buffer to be used. Equivalently, the number
    /// of samples that have been seen so far modulo DFT_Length.
    size_t x_index = 0;

    /// Twiddle factors for the update algorithm
    std::complex<NumberFormat> twiddle[DFT_Length];

    /// Frequency domain values (unwindowed!)
    std::complex<NumberFormat> S[DFT_Length];

public:
    /// Frequency domain values (windowed)
    std::complex<NumberFormat> dft[DFT_Length];

    /// A damping factor introduced into the recursive DFT algorithm to guarantee
    /// stability.
    NumberFormat damping_factor = std::nexttoward((NumberFormat)1, (NumberFormat)0);

    /// Constructor
    SlidingDFT()
    {
        const std::complex<NumberFormat> j(0.0, 1.0);
        const NumberFormat N = DFT_Length;

        // Compute the twiddle factors, and zero the x and S arrays
        for (size_t k = 0; k < DFT_Length; k++) {
            NumberFormat factor = (NumberFormat)(2.0 * M_PI) * k / N;
            this->twiddle[k] = std::exp(j * factor);
            this->S[k] = 0;
            this->x[k] = 0;
        }
    }

    /// Determine whether the output data is valid
    bool is_data_valid()
    {
        return this->data_valid;
    }

    /// Update the calculation with a new sample
    /// Returns true if the data are valid (because enough samples have been
    /// presented), or false if the data are invalid.
    bool update(NumberFormat new_x)
    {
        // Update the storage of the time domain values
        const NumberFormat old_x = this->x[this->x_index];
        this->x[this->x_index] = new_x;

        // Update the DFT
        const NumberFormat r = this->damping_factor;
        const NumberFormat r_to_N = pow(r, (NumberFormat)DFT_Length);
        for (size_t k = 0; k < DFT_Length; k++) {
            this->S[k] = this->twiddle[k] * (r * this->S[k] - r_to_N * old_x + new_x);
        }

        // Apply the Hanning window
        this->dft[0] = (NumberFormat)0.5*this->S[0] - (NumberFormat)0.25*(this->S[DFT_Length - 1] + this->S[1]);
        for (size_t k = 1; k < (DFT_Length - 1); k++) {
            this->dft[k] = (NumberFormat)0.5*this->S[k] - (NumberFormat)0.25*(this->S[k - 1] + this->S[k + 1]);
        }
        this->dft[DFT_Length - 1] = (NumberFormat)0.5*this->S[DFT_Length - 1] - (NumberFormat)0.25*(this->S[DFT_Length - 2] + this->S[0]);

        // Increment the counter
        this->x_index++;
        if (this->x_index >= DFT_Length) {
            this->data_valid = true;
            this->x_index = 0;
        }

        // Done.
        return this->data_valid;
    }
};

/*
 * flag to enable file dumping
 */
//#define ENABLE_LOG  1

#endif //NATIVE_AUDIO_AUDIO_COMMON_H
