//
//  Frame.h
//  QVOD
//
//  Created by bigbug on 11-11-26.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#pragma once

#include "Global.h"
#include "../DependencyObject.h"
#include "FFmpegData.h"

class CFramePool;

class CFrame : public CDependencyObject
{
    friend class CFramePool;
public:

    int           m_nType;
    BOOL          m_bShow;
    int           m_nDuration;
    int           m_nWidth;
    int           m_nHeight;
    AVFrame*      m_pFrame;
    AVPixelFormat m_ePixFmt;

protected:
    CFrame();
    virtual ~CFrame();

    virtual void Free();
};

class CVideoFrame : public CFrame {
public:
    CVideoFrame();
    virtual ~CVideoFrame();

    int Resize(int nWidth, int nHeight, enum AVPixelFormat ePixFmt);
    int Alloc(int nWidth, int nHeight);
};

class CAudioFrame : public CFrame {
public:
    CAudioFrame();
    virtual ~CAudioFrame();

    int Alloc(int nbSamples, int sampleRate, AVSampleFormat sampleFormat, AVChannelLayout channelLayout);
};

