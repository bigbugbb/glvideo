//
//  Pools.cpp
//  QvodPlayer
//
//  Created by bigbug on 12-2-17.
//  Copyright (c) 2012年 qvod. All rights reserved.
//

#include <iostream>
#include "Pools.h"
#include "FFmpegData.h"
#include "../MediaObject.h"

///////////////////////////////////////////////////////////////

int CPacketPool::Flush()
{
    CMediaSample sample;

    while (GetUnused(sample) == S_OK) {
        AVPacket* pPacket = *(AVPacket**)sample.m_pBuf;
        av_packet_unref(pPacket);
        Recycle(sample);
    }
    AssertValid(Size() == 0);

    return S_OK;
}

///////////////////////////////////////////////////////////////

CFramePool::CFramePool() : CSamplePool()
{
    POOL_PROPERTIES request, actual;
    request.nSize  = sizeof(AVFrame*);
    request.nCount = FRAME_POOL_SIZE;
    CSamplePool::SetProperties(&request, &actual);

    CMediaSample sample;
    for (int i = 0; GetEmpty(sample) == S_OK; ++i) {
        sample.m_Type = SAMPLE_FRAME;
        Commit(sample);
    }
    Flush();
}

CFramePool::~CFramePool()
{
}

int CFramePool::Flush()
{
    CMediaSample sample;

    while (GetUnused(sample) == S_OK) {
        AVFrame* pFrame = *(AVFrame**)sample.m_pBuf;
        av_frame_unref(pFrame);
        Recycle(sample);
    }
    AssertValid(Size() == 0);

    return S_OK;
}

