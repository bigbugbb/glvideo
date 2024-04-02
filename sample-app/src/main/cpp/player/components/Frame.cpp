//
//  Frame.cpp
//  QVOD
//
//  Created by bigbug on 11-12-1.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#include <iostream>
#include "Frame.h"
#include "FFmpegData.h"

CFrame::CFrame()
{
    m_bShow     = FALSE;
    m_nDuration = 0;
    m_nWidth    = 0;
    m_nHeight   = 0;

    m_pFrame = av_frame_alloc();
    av_frame_unref(m_pFrame);
}

CFrame::~CFrame()
{
    Free();
}

CVideoFrame::CVideoFrame()
{
}

CVideoFrame::~CVideoFrame() noexcept
{
}

int CVideoFrame::Resize(int nWidth, int nHeight, enum AVPixelFormat ePixFmt)
{
    Log("CVideoFrame::OnFrameResize, width: %d, height: %d\n", nWidth, nHeight);
    int nResult = S_OK;

    m_ePixFmt = ePixFmt;

    Free();
    if ((nResult = Alloc(nWidth, nHeight)) != S_OK) {
        Log("CVideoFrame::Alloc failed\n");
        return nResult;
    }

    Log("CVideoFrame::OnFrameResize end");
    return S_OK;
}

int CVideoFrame::Alloc(int nWidth, int nHeight)
{
    if (av_image_alloc(const_cast<uint8_t **>(m_pFrame->data), m_pFrame->linesize, nWidth, nHeight, m_ePixFmt, 32) < 0) {
        return E_FAIL;
    }

    m_nWidth  = nWidth;
    m_nHeight = nHeight;
    m_pFrame->width  = nWidth;
    m_pFrame->height = nHeight;
    m_pFrame->format = m_ePixFmt;
    
    return S_OK;
}

void CVideoFrame::Free()
{
    if (m_pFrame->data[0]) {
        av_freep(&m_pFrame->data[0]);
    }

    av_frame_unref(m_pFrame);
    av_frame_free(&m_pFrame);
}

CAudioFrame::CAudioFrame()
{
}

CAudioFrame::~CAudioFrame() noexcept
{
}

int CAudioFrame::Alloc(int nbSamples, int sampleRate, AVSampleFormat sampleFormat, AVChannelLayout channelLayout)
{
    if (m_pFrame) {
        av_frame_free(&m_pFrame);
    }
    m_pFrame = av_frame_alloc();
    m_pFrame->sample_rate = sampleRate;
    m_pFrame->format = sampleFormat;
    m_pFrame->ch_layout = channelLayout;
    m_pFrame->nb_samples = nbSamples;
    // 分配buffer
    av_frame_get_buffer(m_pFrame,0);
    av_frame_make_writable(m_pFrame);
}

void CAudioFrame::Free()
{
    if (m_pFrame->data[0]) {
        av_freep(&m_pFrame->data[0]);
    }

    av_frame_unref(m_pFrame);
    av_frame_free(&m_pFrame);
}


