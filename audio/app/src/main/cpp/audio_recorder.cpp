#include <cstring>
#include <cstdlib>
#include "audio_recorder.h"
/*
 * bqRecorderCallback(): called for every buffer is full;
 *                       pass directly to handler
 */
void bqRecorderCallback(SLAndroidSimpleBufferQueueItf bq, void *rec) {
    double now = now_us();
    (static_cast<AudioRecorder *>(rec))->ProcessSLCallback(bq, now);
}

void AudioRecorder::ProcessSLCallback(SLAndroidSimpleBufferQueueItf bq, double time) {
#ifdef ENABLE_LOG
    recLog_->logTime();
#endif
    assert(bq == recBufQueueItf_);
    uint8_t *buffer = buffers[currentBuffer];

//    LOGW("Gavy Says Recording in %s", __FUNCTION__);

    int samples = sharedData->bufferSize / 2;
    int16_t *arr = (int16_t *) buffer;

//    double start = now_us();
//    double khzVal = std::abs(data[107]);
//    for (int i = 0; i < samples; ++i) {
//        sdft->update(arr[i]);
//    }
//    double khzVal = std::abs(sdft->dft[14]);
//
//    LOGW("Gavy Says Max FFT (%f, %f) in %s", khzVal, now_us() - start, __FUNCTION__);


    for (int i = 0; i < samples; ++i) {
        sdft->update(arr[i]);
        double val = std::abs(sdft->dft[14]);
//        int16_t val = arr[i];
//        val = abs((int) val);


        if (val < 150) {
            continue;
        }

        double delta = ((double) (samples - i)) / 48000.0 * 1000000.0;
        double endTime = time - delta;

        if (!sharedData->isEchoer) {
            if (sharedData->waitingForSelf) {
                sharedData->endTime = endTime;
                sharedData->waitingForSelf = false;
                sharedData->selfLatency = sharedData->endTime - sharedData->startTime;
                LOGW("Gavy Says Self Latency (%f), %f in %s", sharedData->selfLatency, delta, __FUNCTION__);
                break;
            } else if (sharedData->waitingForOther and endTime > sharedData->endTime + sharedData->selfLatency + 50000) {
                sharedData->waitingForOther = false;
                double otherLatency = endTime - sharedData->startTime;
                double totalLatency = otherLatency - sharedData->selfLatency;

                SendResult(totalLatency);

                break;
            }
        } else {
            if (sharedData->waitingForSelf) {
                sharedData->endTime = endTime;
                sharedData->waitingForSelf = false;
                double currentLatency = sharedData->endTime - sharedData->startTime;
                double totalLatency = sharedData->selfLatency + currentLatency;

                SendResult(totalLatency);

                break;
            } else if (endTime > sharedData->startTime + 500000) {
                // Assuming enough time has passed since last play
                sharedData->startTime = endTime;
                sharedData->play = true;
                break;
            }
        }
    }


    SLresult result = (*bq)->Enqueue(bq, buffer, sharedData->bufferSize);
    SLASSERT(result);

    currentBuffer %= currentBuffer + 1;


    if(++audioBufCount == PLAY_KICKSTART_BUFFER_COUNT && callback_) {
        callback_(ctx_, ENGINE_SERVICE_MSG_KICKSTART_PLAYER, NULL);
    }

    // should leave the device to sleep to save power if no buffers
//    if(devShadowQueue_->size() == 0) {
//        (*recItf_)->SetRecordState(recItf_, SL_RECORDSTATE_STOPPED);
//    }
}

AudioRecorder::AudioRecorder(SampleFormat *sampleFormat, SLEngineItf slEngine, SharedData *sd) :
        callback_(nullptr)
{
    SLresult result;
    sampleInfo_ = *sampleFormat;
    SLAndroidDataFormat_PCM_EX format_pcm;
    ConvertToSLSampleFormat(&format_pcm, &sampleInfo_);

    // configure audio source
    SLDataLocator_IODevice loc_dev = {SL_DATALOCATOR_IODEVICE,
                                      SL_IODEVICE_AUDIOINPUT,
                                      SL_DEFAULTDEVICEID_AUDIOINPUT,
                                      NULL };
    SLDataSource audioSrc = {&loc_dev, NULL };

    // configure audio sink
    SLDataLocator_AndroidSimpleBufferQueue loc_bq = {
            SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,
            DEVICE_SHADOW_BUFFER_QUEUE_LEN };

    SLDataSink audioSnk = {&loc_bq, &format_pcm};

    // create audio recorder
    // (requires the RECORD_AUDIO permission)
    const SLInterfaceID id[2] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE,
                                 SL_IID_ANDROIDCONFIGURATION };
    const SLboolean req[2] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};
    result = (*slEngine)->CreateAudioRecorder(slEngine,
                                              &recObjectItf_,
                                              &audioSrc,
                                              &audioSnk,
                                              sizeof(id)/sizeof(id[0]),
                                              id, req);
    SLASSERT(result);

    // Configure the voice recognition preset which has no
    // signal processing for lower latency.
    SLAndroidConfigurationItf inputConfig;
    result = (*recObjectItf_)->GetInterface(recObjectItf_,
                                            SL_IID_ANDROIDCONFIGURATION,
                                            &inputConfig);
    if (SL_RESULT_SUCCESS == result) {
        SLuint32 presetValue = SL_ANDROID_RECORDING_PRESET_VOICE_RECOGNITION;
        (*inputConfig)->SetConfiguration(inputConfig,
                                         SL_ANDROID_KEY_RECORDING_PRESET,
                                         &presetValue,
                                         sizeof(SLuint32));
    }
    result = (*recObjectItf_)->Realize(recObjectItf_, SL_BOOLEAN_FALSE);
    SLASSERT(result);
    result = (*recObjectItf_)->GetInterface(recObjectItf_,
                    SL_IID_RECORD, &recItf_);
    SLASSERT(result);

    result = (*recObjectItf_)->GetInterface(recObjectItf_,
                    SL_IID_ANDROIDSIMPLEBUFFERQUEUE, &recBufQueueItf_);
    SLASSERT(result);

    result = (*recBufQueueItf_)->RegisterCallback(recBufQueueItf_,
                    bqRecorderCallback, this);
    SLASSERT(result);

    sharedData = sd;

    LOGW("Gavy Says Initializing Record Buffers (%d) in %s", sharedData->bufferSize, __FUNCTION__);

    buffers = new uint8_t*[NUM_RECORDING_BUFS];
    for (int i = 0; i < NUM_RECORDING_BUFS; ++i) {
        buffers[i] = new uint8_t[sharedData->bufferSize];
        assert(buffers[i]);
    }
    assert(buffers);

    sdft = new SlidingDFT<float, 32>();
    assert(sdft);

#ifdef ENABLE_LOG
    std::string name = "rec";
    recLog_ = new AndroidLog(name);
#endif
}

SLboolean AudioRecorder::Start(void) {
    audioBufCount = 0;

    SLresult result;
    // in case already recording, stop recording and clear buffer queue
    result = (*recItf_)->SetRecordState(recItf_, SL_RECORDSTATE_STOPPED);
    SLASSERT(result);
    result = (*recBufQueueItf_)->Clear(recBufQueueItf_);
    SLASSERT(result);

    for(int i =0; i < NUM_RECORDING_BUFS; i++ ) {
        result = (*recBufQueueItf_)->Enqueue(recBufQueueItf_, buffers[i], sharedData->bufferSize);
        SLASSERT(result);
    }

    result = (*recItf_)->SetRecordState(recItf_, SL_RECORDSTATE_RECORDING);
    SLASSERT(result);

    currentBuffer = 0;

    return (result == SL_RESULT_SUCCESS? SL_BOOLEAN_TRUE:SL_BOOLEAN_FALSE);
}

SLboolean  AudioRecorder::Stop(void) {
    // in case already recording, stop recording and clear buffer queue
    SLuint32 curState;

    SLresult result = (*recItf_)->GetRecordState(recItf_, &curState);
    SLASSERT(result);
    if( curState == SL_RECORDSTATE_STOPPED) {
        return SL_BOOLEAN_TRUE;
    }
    result = (*recItf_)->SetRecordState(recItf_, SL_RECORDSTATE_STOPPED);
    SLASSERT(result);
    result = (*recBufQueueItf_)->Clear(recBufQueueItf_);
    SLASSERT(result);


#ifdef ENABLE_LOG
    recLog_->flush();
#endif

    return SL_BOOLEAN_TRUE;
}

AudioRecorder::~AudioRecorder() {
    // destroy audio recorder object, and invalidate all associated interfaces
    if (recObjectItf_ != NULL) {
        (*recObjectItf_)->Destroy(recObjectItf_);
    }

    if(buffers) {
        for (int i = 0; i < NUM_RECORDING_BUFS; ++i) {
            if (buffers[i]) {
                delete[] buffers[i];
            }
        }
        delete[] buffers;
    }

    delete sdft;
#ifdef  ENABLE_LOG
    if(recLog_) {
        delete recLog_;
    }
#endif
}

void AudioRecorder::RegisterCallback(ENGINE_CALLBACK cb, void *ctx) {
    callback_ = cb;
    ctx_ = ctx;
}

void AudioRecorder::SendResult(double latency) {
    double distance = latency * 0.0003436;

    LOGW("Gavy Says Distance = %f, Latency = %f (Echo Mode = %d) in %s", distance, latency, sharedData->isEchoer, __FUNCTION__);

    JNIEnv* env;
    JavaVMAttachArgs args;
    args.version = JNI_VERSION_1_6; // choose your JNI version
    args.name = NULL; // you might want to give the java thread a name
    args.group = NULL; // you might want to assign the java thread to a ThreadGroup
    sharedData->jvm->AttachCurrentThread(&env, &args);


    jclass jc = env->GetObjectClass(sharedData->clas);
    jmethodID mid = env->GetMethodID(jc, "showDistance","(DD)V");
    env->CallVoidMethod(sharedData->clas, mid, distance, latency);
}