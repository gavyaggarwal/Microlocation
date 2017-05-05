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

#include "audio_common.h"



double now_us(void) {

    struct timespec res;
    clock_gettime(CLOCK_MONOTONIC, &res);
    return 1000000.0 * res.tv_sec + (double) res.tv_nsec / 1e3;

}


void fft(CArray& x)
{
    const size_t N = x.size();
    if (N <= 1) return;

    // divide
    CArray even = x[std::slice(0, N/2, 2)];
    CArray  odd = x[std::slice(1, N/2, 2)];

    // conquer
    fft(even);
    fft(odd);

    // combine
    for (size_t k = 0; k < N/2; ++k)
    {
        Complex t = std::polar(1.0, -2 * PI * k / N) * odd[k];
        x[k    ] = even[k] + t;
        x[k+N/2] = even[k] - t;
    }
}


void ConvertToSLSampleFormat(SLAndroidDataFormat_PCM_EX *pFormat,
                                    SampleFormat* pSampleInfo_) {

    assert(pFormat);
    memset(pFormat, 0, sizeof(*pFormat));

    pFormat->formatType = SL_DATAFORMAT_PCM;
    if( pSampleInfo_->channels_  <= 1 ) {
        pFormat->numChannels = 1;
        pFormat->channelMask = SL_SPEAKER_FRONT_CENTER;
    } else {
        pFormat->numChannels = 2;
        pFormat->channelMask = SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT;
    }
    pFormat->sampleRate  = pSampleInfo_->sampleRate_;

    pFormat->endianness  = SL_BYTEORDER_LITTLEENDIAN;
    pFormat->bitsPerSample = pSampleInfo_->pcmFormat_;
    pFormat->containerSize = pSampleInfo_->pcmFormat_;

    /*
     * fixup for android extended representations...
     */
    pFormat->representation = pSampleInfo_->representation_;
    switch (pFormat->representation) {
        case SL_ANDROID_PCM_REPRESENTATION_UNSIGNED_INT:
            pFormat->bitsPerSample =  SL_PCMSAMPLEFORMAT_FIXED_8;
            pFormat->containerSize = SL_PCMSAMPLEFORMAT_FIXED_8;
            pFormat->formatType = SL_ANDROID_DATAFORMAT_PCM_EX;
            break;
        case SL_ANDROID_PCM_REPRESENTATION_SIGNED_INT:
            pFormat->bitsPerSample =  SL_PCMSAMPLEFORMAT_FIXED_16; //supports 16, 24, and 32
            pFormat->containerSize = SL_PCMSAMPLEFORMAT_FIXED_16;
            pFormat->formatType = SL_ANDROID_DATAFORMAT_PCM_EX;
            break;
        case SL_ANDROID_PCM_REPRESENTATION_FLOAT:
            pFormat->bitsPerSample =  SL_PCMSAMPLEFORMAT_FIXED_32;
            pFormat->containerSize = SL_PCMSAMPLEFORMAT_FIXED_32;
            pFormat->formatType = SL_ANDROID_DATAFORMAT_PCM_EX;
            break;
        case 0:
            break;
        default:
            assert(0);
    }
}

