#include <cstdlib>
#include "audio_player.h"

/*
 * Called by OpenSL SimpleBufferQueue for every audio buffer played
 * directly pass thru to our handler.
 * The regularity of this callback from openSL/Android System affects
 * playback continuity. If it does not callback in the regular time
 * slot, you are under big pressure for audio processing[here we do
 * not do any filtering/mixing]. Callback from fast audio path are
 * much more regular than other audio paths by my observation. If it
 * very regular, you could buffer much less audio samples between
 * recorder and player, hence lower latency.
 */
void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *ctx) {
    (static_cast<AudioPlayer *>(ctx))->ProcessSLCallback(bq);
}

void AudioPlayer::ProcessSLCallback(SLAndroidSimpleBufferQueueItf bq) {
#ifdef ENABLE_LOG
    logFile_->logTime();
#endif

//    LOGW("Gavy Says Playing in %s", __FUNCTION__);

    uint8_t *outputBuffer = blankBuffer;


    if (sharedData->isEchoer == false) {
        if (sharedData->play) {
            outputBuffer = soundBuffer;
            sharedData->play = false;
            sharedData->waitingForSelf = true;
            sharedData->waitingForOther = true;

            //LOGW("Gavy Says Audio Sent (%f) in %s", now_us(), __FUNCTION__);
            sharedData->startTime = now_us();
        }
    } else {
        double now = now_us();
        if (sharedData->play and now > sharedData->startTime + 50000) {
            outputBuffer = soundBuffer;
            sharedData->play = false;
            sharedData->waitingForSelf = true;
            sharedData->selfLatency = now - sharedData->startTime;
            LOGW("Gavy Says Echo Process Latency (%f) in %s", sharedData->selfLatency, __FUNCTION__);

            //LOGW("Gavy Says Audio Sent (%f) in %s", now_us(), __FUNCTION__);
            sharedData->startTime = now;
        }
    }

    (*bq)->Enqueue(bq, outputBuffer, sharedData->bufferSize);
}

AudioPlayer::AudioPlayer(SampleFormat *sampleFormat, SLEngineItf slEngine, SharedData *sd) :
    callback_(nullptr)
{
    SLresult result;
    assert(sampleFormat);
    sampleInfo_ = *sampleFormat;

    result = (*slEngine)->CreateOutputMix(slEngine, &outputMixObjectItf_,
                                          0, NULL, NULL);
    SLASSERT(result);

    // realize the output mix
    result = (*outputMixObjectItf_)->Realize(outputMixObjectItf_, SL_BOOLEAN_FALSE);
    SLASSERT(result);

    // configure audio source
    SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {
            SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,
            DEVICE_SHADOW_BUFFER_QUEUE_LEN };

    SLAndroidDataFormat_PCM_EX format_pcm;
    ConvertToSLSampleFormat(&format_pcm, &sampleInfo_);
    SLDataSource audioSrc = {&loc_bufq, &format_pcm};

    // configure audio sink
    SLDataLocator_OutputMix loc_outmix = {SL_DATALOCATOR_OUTPUTMIX, outputMixObjectItf_};
    SLDataSink audioSnk = {&loc_outmix, NULL};
    /*
     * create fast path audio player: SL_IID_BUFFERQUEUE and SL_IID_VOLUME interfaces ok,
     * NO others!
     */
    SLInterfaceID  ids[2] = { SL_IID_BUFFERQUEUE, SL_IID_VOLUME};
    SLboolean      req[2] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};
    result = (*slEngine)->CreateAudioPlayer(slEngine, &playerObjectItf_, &audioSrc, &audioSnk,
                                            sizeof(ids)/sizeof(ids[0]), ids, req);
    SLASSERT(result);

    // realize the player
    result = (*playerObjectItf_)->Realize(playerObjectItf_, SL_BOOLEAN_FALSE);
    SLASSERT(result);

    // get the play interface
    result = (*playerObjectItf_)->GetInterface(playerObjectItf_, SL_IID_PLAY, &playItf_);
    SLASSERT(result);

    // get the buffer queue interface
    result = (*playerObjectItf_)->GetInterface(playerObjectItf_, SL_IID_BUFFERQUEUE,
                                             &playBufferQueueItf_);
    SLASSERT(result);

    // register callback on the buffer queue
    result = (*playBufferQueueItf_)->RegisterCallback(playBufferQueueItf_, bqPlayerCallback, this);
    SLASSERT(result);

    result = (*playItf_)->SetPlayState(playItf_, SL_PLAYSTATE_STOPPED);
    SLASSERT(result);

    sharedData = sd;
    soundBuffer = (uint8_t *) malloc(sharedData->bufferSize);
    blankBuffer = (uint8_t *) malloc(sharedData->bufferSize);

    LOGW("Gavy Says Initializing Play Buffers (%d) in %s", sharedData->bufferSize, __FUNCTION__);

    int samples = sharedData->bufferSize / 2;
    int16_t *sound = (int16_t *) soundBuffer;
    int16_t *blank = (int16_t *) blankBuffer;
    if (sharedData->isEchoer) {
        for (int i = 0; i < samples; ++i) {
            sound[i] = (int16_t) (cos(2 * PI * i / 48000 * ECHO_MODE_FREQUENCY) * 32767);
            blank[i] = 0;
        }
    } else {
        for (int i = 0; i < samples; ++i) {
            sound[i] = (int16_t) (cos(2 * PI * i / 48000 * MAIN_MODE_FREQUENCY) * 32767);
            blank[i] = 0;
        }
    }

#ifdef  ENABLE_LOG
    std::string name = "play";
    logFile_ = new AndroidLog(name);
#endif
}

AudioPlayer::~AudioPlayer() {

    // destroy buffer queue audio player object, and invalidate all associated interfaces
    if (playerObjectItf_ != NULL) {
        (*playerObjectItf_)->Destroy(playerObjectItf_);
    }

    // destroy output mix object, and invalidate all associated interfaces
    if (outputMixObjectItf_) {
        (*outputMixObjectItf_)->Destroy(outputMixObjectItf_);
    }

    free(soundBuffer);
    free(blankBuffer);
}

SLresult AudioPlayer::Start(void) {
    SLuint32   state;
    SLresult  result = (*playItf_)->GetPlayState(playItf_, &state);
    if (result != SL_RESULT_SUCCESS) {
        return SL_BOOLEAN_FALSE;
    }
    if(state == SL_PLAYSTATE_PLAYING) {
        return SL_BOOLEAN_TRUE;
    }

    result = (*playItf_)->SetPlayState(playItf_, SL_PLAYSTATE_STOPPED);
    SLASSERT(result);

    result = (*playItf_)->SetPlayState(playItf_, SL_PLAYSTATE_PLAYING);
    SLASSERT(result);

    return SL_BOOLEAN_TRUE;
}

void AudioPlayer::Stop(void) {
    SLuint32   state;

    SLresult   result = (*playItf_)->GetPlayState(playItf_, &state);
    SLASSERT(result);

    if(state == SL_PLAYSTATE_STOPPED)
        return;

    result = (*playItf_)->SetPlayState(playItf_, SL_PLAYSTATE_STOPPED);
    SLASSERT(result);

#ifdef ENABLE_LOG
    if (logFile_) {
        delete logFile_;
        logFile_ = nullptr;
    }
#endif
}

void AudioPlayer::PlayAudioBuffers(int32_t count) {
    if(!count) {
        return;
    }

    SLresult result = (*playBufferQueueItf_)->Enqueue(playBufferQueueItf_, blankBuffer, sharedData->bufferSize);
    SLASSERT(result);
}

void AudioPlayer::RegisterCallback(ENGINE_CALLBACK cb, void *ctx) {
    callback_ = cb;
    ctx_ = ctx;
}