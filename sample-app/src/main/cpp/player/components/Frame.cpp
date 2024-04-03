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
    m_nType     = 0;
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

int CFrame::Resize(int nWidth, int nHeight, enum AVPixelFormat ePixFmt)
{
    Log("CFrame::OnFrameResize, width: %d, height: %d\n", nWidth, nHeight);
    int nResult = S_OK;

    m_ePixFmt = ePixFmt;

    Free();
    if ((nResult = Alloc(nWidth, nHeight)) != S_OK) {
        Log("CFrame::Alloc failed\n");
        return nResult;
    }

    Log("CFrame::OnFrameResize end");
    return S_OK;
}

int CFrame::Alloc(int nWidth, int nHeight)
{
    AssertValid(nWidth > 0 && nHeight > 0);
//    int nSize = avpicture_get_size(m_ePixFmt, nWidth, nHeight);
//
//    if ((m_pFrame = av_frame_alloc()) == NULL) {
//        return E_OUTOFMEMORY;
//    }
//    if ((m_pData = (BYTE*)align_malloc(nSize, nAlign)) == NULL) {
//        av_free(m_pFrame); m_pFrame = NULL;
//        return E_OUTOFMEMORY;
//    }
//
//    if (avpicture_fill((AVPicture*)m_pFrame, m_pData, m_ePixFmt, nWidth, nHeight) < 0) {
//        return E_FAIL;
//    }
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

void CFrame::Free()
{
    if (m_pFrame->data[0]) {
        av_freep(m_pFrame->data[0]);
    }

    av_frame_unref(m_pFrame);
}

