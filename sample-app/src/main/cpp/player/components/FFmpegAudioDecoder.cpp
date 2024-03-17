//
//  FFmpegAudioDecoder.cpp
//  QVOD
//
//  Created by bigbug on 11-11-14.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#include <iostream>
#include "GUIDs.h"
#include "Global.h"
#include "FFmpegData.h"
#include "FFmpegAudioDecoder.h"

#ifdef LOG_AUDIO_PCM
    extern std::string strPathPCM;
    #define LOG_PCM(buf, size) \
    { \
        FILE* fp = fopen(strPathPCM.c_str(), "a+"); \
        fwrite(buf, 1, size, fp); \
        fclose(fp); \
    }
#else 
    #define LOG_PCM(buf, size)
#endif

CFFmpegAudioDecoder::CFFmpegAudioDecoder(const GUID& guid, IDependency* pDepend, int* pResult)
    : CMediaObject(guid, pDepend), m_pPcmPool(NULL)
{
    m_pAudio = NULL;
    m_pFrame = av_frame_alloc();
}

CFFmpegAudioDecoder::~CFFmpegAudioDecoder()
{
    av_frame_free(&m_pFrame);
}

// IFFmpegAudioDecoder
int CFFmpegAudioDecoder::SetParameter(int nParam, void* pValue)
{
    AssertValid(pValue);
    
    switch (nParam) {
    default:
        break;
    }
    
    return S_OK;
}

int CFFmpegAudioDecoder::GetParameter(int nParam, void* pValue)
{
    AssertValid(pValue);
    
    switch (nParam) {
    default:
        break;
    }
    
    return S_OK;
}

int CFFmpegAudioDecoder::Load()
{
    Log("CFFmpegAudioDecoder::Load\n");
    CMediaObject* pDemuxer = NULL;
    
    for (int i = 0; i < m_vecInObjs.size(); ++i) {
        const GUID& guid = m_vecInObjs[i]->GetGUID();
        if (!memcmp(&guid, &GUID_DEMUXER, sizeof(GUID))) {
            pDemuxer = m_vecInObjs[i];
        }
    }
    if (!pDemuxer) {
        return E_FAIL;
    }
    pDemuxer->GetSamplePool(GetGUID(), &m_pAudioPool);
    if (!m_pAudioPool) {
        return E_FAIL;
    }
    
    for (int i = 0; i < m_vecOutObjs.size(); ++i) {
        const GUID& guid = m_vecOutObjs[i]->GetGUID();
        if (!memcmp(&guid, &GUID_AUDIO_RENDERER, sizeof(GUID))) {
            m_pRenderer = m_vecOutObjs[i];
        }
    }
    if (!m_pRenderer) {
        return E_FAIL;
    }
    m_pRenderer->GetSamplePool(GetGUID(), &m_pPcmPool);
    if (!m_pPcmPool) {
        return E_FAIL;
    }
        
    Create();
    m_sync.Signal();
    
    CMediaObject::Load();
    return S_OK;
}

int CFFmpegAudioDecoder::WaitForResources(BOOL bWait)
{
    Log("CFFmpegAudioDecoder::WaitForResources\n");
    CMediaObject::WaitForResources(bWait);
    
    return S_OK;
}

int CFFmpegAudioDecoder::Idle()
{
    Log("CFFmpegAudioDecoder::Idle\n");
    Start();
    
    CMediaObject::Idle();
    return S_OK;
}

int CFFmpegAudioDecoder::Execute()
{
    Log("CFFmpegAudioDecoder::Execute\n");
    
    CMediaObject::Execute();
    return S_OK;
}

int CFFmpegAudioDecoder::Pause()
{
    Log("CFFmpegAudioDecoder::Pause\n");
    
    CMediaObject::Pause();
    return S_OK;
}

int CFFmpegAudioDecoder::BeginFlush()
{
    Log("CFFmpegAudioDecoder::BeginFlush\n");
    CMediaObject::BeginFlush();
    
    m_sync.Wait();
    
    return S_OK;
}

int CFFmpegAudioDecoder::EndFlush()
{
    Log("CFFmpegAudioDecoder::EndFlush\n");
    m_sync.Signal();
    
    return CMediaObject::EndFlush();
}

int CFFmpegAudioDecoder::Invalid()
{
    Log("CFFmpegAudioDecoder::Invalid\n");
    CMediaObject::Invalid();
    
    Close();
    
    return S_OK;
}

int CFFmpegAudioDecoder::Unload()
{
    Log("CFFmpegAudioDecoder::Unload\n");
    Close();
    
    m_pRenderer  = NULL;
    m_pPcmPool   = NULL;
    m_pAudioPool = NULL;
    
    CMediaObject::Unload();
    return S_OK;
}

int CFFmpegAudioDecoder::SetEOS()
{
    if (m_bEOS) {
        return S_OK;
    }
    
    return CMediaObject::SetEOS();
}

THREAD_RETURN CFFmpegAudioDecoder::ThreadProc()
{
    int nWait = 0;

    while (m_bRun) {
        m_sync.Wait();
        
        NotifyEvent(EVENT_AUDIO_NEED_DATA, !m_pPcmPool->GetSize(), 0, NULL);
        
        if (Receive(m_pAudioPool) == E_RETRY) {
            nWait = 20;
        } else {
            nWait = 0;
        }
        
        m_sync.Signal();
        
        if (m_vecInObjs[0]->IsEOS() && !m_pAudioPool->GetSize()) {
            SetEOS();
        }
        
        Sleep(nWait);
    }
    
    return 0;
}

int CFFmpegAudioDecoder::OnReceive(CMediaSample& sample)
{
    AssertValid(sample.m_nSize == sizeof(AVPacket*));
    AVPacket* pPacket = *(AVPacket**)sample.m_pBuf;
    AVCodecContext* pCodecCtx = (AVCodecContext*)sample.m_pExten;
    m_pAudio = (AudioInfo*)sample.m_pSpecs;
    
    int nResult = Decode(pPacket, pCodecCtx, sample);
    if (nResult != E_RETRY) {
        av_packet_unref(pPacket);
        av_packet_free(&pPacket);
    }
    
    return nResult;
}

inline
int CFFmpegAudioDecoder::Decode(AVPacket* pPacket, AVCodecContext* pCodecCtx, const CMediaSample& sampleIn)
{
    if (sampleIn.m_bIgnore) {
        return S_OK;
    }
    
    CMediaSample mediaSample;
    if (m_pPcmPool->GetEmpty(mediaSample) != S_OK) {
        return E_RETRY;
    }

    if (avcodec_send_packet(pCodecCtx, pPacket) != 0) {
        return E_FAIL;
    }

    while (avcodec_receive_frame(pCodecCtx, m_pFrame) == 0) {
        int nChannels = m_pFrame->ch_layout.nb_channels;
        int nSamples = m_pFrame->nb_samples;

        if (av_sample_fmt_is_planar(pCodecCtx->sample_fmt)) {
            for (int i = 0; i < nChannels; ++i) {
                float* data = (float*)m_pFrame->data[i];
                // TODO: Process your data here...
            }
        } else { // This means the data is interleaved
            // Determine the sample format
            if (m_pFrame->format == AV_SAMPLE_FMT_S16) { // 16-bit signed integers
                int16_t *samples = (int16_t *)m_pFrame->data[0]; // All channels are in data[0]
                for (int i = 0; i < nSamples; i++) {
                    for (int ch = 0; ch < nChannels; ch++) {
                        int16_t sample = samples[i * nChannels + ch];
                        // TODO: Process the sample for channel 'ch' here...
                    }
                }
            } else if (m_pFrame->format == AV_SAMPLE_FMT_FLT) { // Single-precision floating-point
                float *samples = (float *)m_pFrame->data[0];
                for (int i = 0; i < nSamples; i++) {
                    for (int ch = 0; ch < nChannels; ch++) {
                        float sample = samples[i * nChannels + ch];
                        // TODO: Process the sample for channel 'ch' here...
                    }
                }
            }
            // Add more conditions for other sample formats as needed
        }
    }

    mediaSample.m_llTimestamp = sampleIn.m_llTimestamp;
    mediaSample.m_llSyncPoint = sampleIn.m_llSyncPoint;

    m_pPcmPool->Commit(mediaSample);

    return S_OK;
}







