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
    request.nSize  = sizeof(DWORD);
    request.nCount = FRAME_POOL_SIZE;
    CSamplePool::SetProperties(&request, &actual);

    CMediaSample sample;
    for (int i = 0; GetEmpty(sample) == S_OK; ++i) {
        sample.m_Type   = SAMPLE_FRAME;
        sample.m_pExten = &m_Frames[i];
        Commit(sample);
    }
    Flush();
}

CFramePool::~CFramePool()
{
}

int CFramePool::Flush()
{
    CSamplePool::Flush();

    for (int i = 0; i < FRAME_POOL_SIZE; ++i) {
        m_Frames[i].m_nType     = 0;
        m_Frames[i].m_bShow     = FALSE;
        m_Frames[i].m_nDuration = 0;
    }

    return S_OK;
}

int CFramePool::Reset()
{
    CFramePool::Flush();

    for (int i = 0; i < FRAME_POOL_SIZE; ++i) {
        m_Frames[i].m_nWidth  = 0;
        m_Frames[i].m_nHeight = 0;
    }

    return S_OK;
}
