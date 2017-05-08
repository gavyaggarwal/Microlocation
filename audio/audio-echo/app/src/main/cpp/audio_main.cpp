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
#include <cassert>
#include <cstring>
#include <jni.h>

#include <sys/types.h>
#include <SLES/OpenSLES.h>

#include "audio_common.h"
#include "audio_recorder.h"
#include "audio_player.h"

struct EchoAudioEngine {
    SLmilliHertz fastPathSampleRate_;
    uint32_t     fastPathFramesPerBuf_;
    uint16_t     sampleChannels_;
    uint16_t     bitsPerSample_;

    SLObjectItf  slEngineObj_;
    SLEngineItf  slEngineItf_;

    AudioRecorder  *recorder_;
    AudioPlayer    *player_;

    SharedData  *sharedData;
};
static EchoAudioEngine engine;

bool EngineService(void* ctx, uint32_t msg, void* data );

extern "C" {
JNIEXPORT void JNICALL
        Java_com_google_sample_echo_MainActivity_createSLEngine(JNIEnv *env, jclass, jint, jint);
JNIEXPORT void JNICALL
        Java_com_google_sample_echo_MainActivity_deleteSLEngine(JNIEnv *env, jclass type);
JNIEXPORT jboolean JNICALL
        Java_com_google_sample_echo_MainActivity_createSLBufferQueueAudioPlayer(JNIEnv *env, jclass);
JNIEXPORT void JNICALL
        Java_com_google_sample_echo_MainActivity_deleteSLBufferQueueAudioPlayer(JNIEnv *env, jclass type);

JNIEXPORT jboolean JNICALL
        Java_com_google_sample_echo_MainActivity_createAudioRecorder(JNIEnv *env, jclass type);
JNIEXPORT void JNICALL
        Java_com_google_sample_echo_MainActivity_deleteAudioRecorder(JNIEnv *env, jclass type);
JNIEXPORT void JNICALL
        Java_com_google_sample_echo_MainActivity_startPlay(JNIEnv *env, jclass type);
JNIEXPORT void JNICALL
        Java_com_google_sample_echo_MainActivity_stopPlay(JNIEnv *env, jclass type);

JNIEXPORT void JNICALL
        Java_com_google_sample_echo_MainActivity_playNoise(JNIEnv *env, jclass, jboolean, jobject);

}

JNIEXPORT void JNICALL
Java_com_google_sample_echo_MainActivity_createSLEngine(
        JNIEnv *env, jclass type, jint sampleRate, jint framesPerBuf) {
    SLresult result;
    memset(&engine, 0, sizeof(engine));

    engine.fastPathSampleRate_   = static_cast<SLmilliHertz>(sampleRate) * 1000;
    engine.fastPathFramesPerBuf_ = static_cast<uint32_t>(framesPerBuf);
    engine.sampleChannels_   = AUDIO_SAMPLE_CHANNELS;
    engine.bitsPerSample_    = SL_PCMSAMPLEFORMAT_FIXED_16;

    result = slCreateEngine(&engine.slEngineObj_, 0, NULL, 0, NULL, NULL);
    SLASSERT(result);

    result = (*engine.slEngineObj_)->Realize(engine.slEngineObj_, SL_BOOLEAN_FALSE);
    SLASSERT(result);

    result = (*engine.slEngineObj_)->GetInterface(engine.slEngineObj_, SL_IID_ENGINE, &engine.slEngineItf_);
    SLASSERT(result);

    // compute the RECOMMENDED fast audio buffer size:
    //   the lower latency required
    //     *) the smaller the buffer should be (adjust it here) AND
    //     *) the less buffering should be before starting player AFTER
    //        receiving the recordered buffer
    //   Adjust the bufSize here to fit your bill [before it busts]
    uint32_t bufSize = engine.fastPathFramesPerBuf_ * engine.sampleChannels_
                       * engine.bitsPerSample_;
    bufSize = (bufSize + 7) >> 3;            // bits --> byte


    engine.sharedData = (SharedData *) malloc(sizeof(SharedData));
    assert(engine.sharedData);
    engine.sharedData->play = false;
    engine.sharedData->waitingForSelf = false;
    engine.sharedData->waitingForOther = false;
    engine.sharedData->startTime = 0;
    engine.sharedData->endTime = 0;
    engine.sharedData->selfLatency = 0;
    engine.sharedData->bufferSize = bufSize;
    engine.sharedData->isEchoer = false;
}

JNIEXPORT jboolean JNICALL
Java_com_google_sample_echo_MainActivity_createSLBufferQueueAudioPlayer(JNIEnv *env, jclass type) {
    SampleFormat sampleFormat;
    memset(&sampleFormat, 0, sizeof(sampleFormat));
    sampleFormat.pcmFormat_ = (uint16_t)engine.bitsPerSample_;

    // SampleFormat.representation_ = SL_ANDROID_PCM_REPRESENTATION_SIGNED_INT;
    sampleFormat.channels_ = (uint16_t)engine.sampleChannels_;
    sampleFormat.sampleRate_ = engine.fastPathSampleRate_;

    engine.player_ = new AudioPlayer(&sampleFormat, engine.slEngineItf_, engine.sharedData);
    assert(engine.player_);
    if(engine.player_ == nullptr)
        return JNI_FALSE;

    engine.player_->RegisterCallback(EngineService, (void*)&engine);

    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_google_sample_echo_MainActivity_deleteSLBufferQueueAudioPlayer(JNIEnv *env, jclass type) {
    if(engine.player_) {
        delete engine.player_;
        engine.player_= nullptr;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_google_sample_echo_MainActivity_createAudioRecorder(JNIEnv *env, jclass type) {
    SampleFormat sampleFormat;
    memset(&sampleFormat, 0, sizeof(sampleFormat));
    sampleFormat.pcmFormat_ = static_cast<uint16_t>(engine.bitsPerSample_);

    // SampleFormat.representation_ = SL_ANDROID_PCM_REPRESENTATION_SIGNED_INT;
    sampleFormat.channels_ = engine.sampleChannels_;
    sampleFormat.sampleRate_ = engine.fastPathSampleRate_;
    engine.recorder_ = new AudioRecorder(&sampleFormat, engine.slEngineItf_, engine.sharedData);
    if(!engine.recorder_) {
        return JNI_FALSE;
    }
    engine.recorder_->RegisterCallback(EngineService, (void*)&engine);
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_google_sample_echo_MainActivity_deleteAudioRecorder(JNIEnv *env, jclass type) {
    if(engine.recorder_)
        delete engine.recorder_;

    engine.recorder_ = nullptr;
}

JNIEXPORT void JNICALL
Java_com_google_sample_echo_MainActivity_startPlay(JNIEnv *env, jclass type) {
    /*
     * start player: make it into waitForData state
     */
    if(SL_BOOLEAN_FALSE == engine.player_->Start()){
        LOGE("====%s failed", __FUNCTION__);
        return;
    }
    engine.recorder_->Start();
}

JNIEXPORT void JNICALL
Java_com_google_sample_echo_MainActivity_stopPlay(JNIEnv *env, jclass type) {
    engine.recorder_->Stop();
    engine.player_ ->Stop();

    delete engine.recorder_;
    delete engine.player_;
    engine.recorder_ = NULL;
    engine.player_ = NULL;
}

JNIEXPORT void JNICALL
Java_com_google_sample_echo_MainActivity_deleteSLEngine(JNIEnv *env, jclass type) {
    if (engine.slEngineObj_ != NULL) {
        (*engine.slEngineObj_)->Destroy(engine.slEngineObj_);
        engine.slEngineObj_ = NULL;
        engine.slEngineItf_ = NULL;
    }

    env->DeleteGlobalRef(engine.sharedData->clas);
    free(engine.sharedData);
}

JNIEXPORT void JNICALL
Java_com_google_sample_echo_MainActivity_playNoise(JNIEnv *env, jclass type, jboolean isEchoer, jobject classref) {
    env->GetJavaVM(&(engine.sharedData->jvm));
    engine.sharedData->clas = env->NewGlobalRef(classref);
    engine.sharedData->play = true;
    engine.sharedData->isEchoer = (bool) isEchoer;
}

/*
 * simple message passing for player/recorder to communicate with engine
 */
bool EngineService(void* ctx, uint32_t msg, void* data ) {
    assert(ctx == &engine);
    switch (msg) {
        case ENGINE_SERVICE_MSG_KICKSTART_PLAYER:
            engine.player_->PlayAudioBuffers(PLAY_KICKSTART_BUFFER_COUNT);
            // we only allow it to call once, so tell caller do not call
            // anymore
            return false;
//        case ENGINE_SERVICE_MSG_RETRIEVE_DUMP_BUFS:
//            *(static_cast<uint32_t*>(data)) = dbgEngineGetBufCount();
//            break;
        default:
            assert(false);
            return false;
    }

    return true;
}
