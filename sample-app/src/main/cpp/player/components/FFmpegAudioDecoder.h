//
//  FFmpegAudioDecoder.h
//  QVOD
//
//  Created by bigbug on 11-11-14.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#pragma once

#include "../SamplePool.h"
#include "../MediaObject.h"
#include "CompInterfaces.h"

// This class is not mandatory. For better performance, we can certainly put 
// all processes into audio renderer and output the decoded PCM at the rate 
// controlled by audio unit callback. I seperate the decoding process just 
// for simplicity and better maintenance. And it makes the whole system symmetrical either.

class CFFmpegAudioDecoder : public CMediaObject,
                            public CThread,
                            public IFFmpegAudioDecoder
{
public:
    CFFmpegAudioDecoder(const GUID& guid, IDependency* pDepend, int* pResult);
    virtual ~CFFmpegAudioDecoder();

    // IFFmpegAudioDecoder
    virtual int SetParameter(int nParam, void* pValue);
    virtual int GetParameter(int nParam, void* pValue);
    
protected:
    // CMediaObject
    int Load();
    int WaitForResources(BOOL bWait);
    int Idle();
    int Execute();
    int Pause();
    int BeginFlush();
    int EndFlush();
    int Invalid();
    int Unload();
    int SetEOS();
    
    int OnReceive(CMediaSample& sample);
    
    virtual THREAD_RETURN ThreadProc();
    int Decode(AVPacket* pPacket, AVCodecContext* pCodecCtx, const CMediaSample& sampleIn);

    int Resample(AVCodecContext* pCodec, CAudioFrame& audioFrame, AVFrame* pSrcFrame);
    void InitOutFrame(int64_t dst_nb_samples);
    
    CEvent          m_sync;

    ISamplePool*    m_pFramePool;
    ISamplePool*    m_pAudioPool;
    CMediaObject*   m_pRenderer;

    AVFrame*        m_pFrame;
    AudioInfo*      m_pAudio;

    SwrContext*     m_pSwrContext;
};

