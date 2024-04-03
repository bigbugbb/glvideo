//
//  Pools.h
//  QvodPlayer
//
//  Created by bigbug on 12-2-17.
//  Copyright (c) 2012年 qvod. All rights reserved.
//

#pragma once

#include "Frame.h"
#include "../SamplePool.h"

class CPacketPool : public CSamplePool
{
public:
    int Flush();
};

const int FRAME_POOL_SIZE = 6;

class CVideoFramePool : public CSamplePool
{
public:
    CVideoFramePool();
    virtual ~CVideoFramePool();
    
    int Flush();
    int Reset();
protected:
    CVideoFrame m_Frames[FRAME_POOL_SIZE];
};

class CAudioFramePool : public CSamplePool
{
public:
    CAudioFramePool();
    virtual ~CAudioFramePool();
    
    int Flush();
    int Reset();
    
protected:
    CAudioFrame m_Frames[FRAME_POOL_SIZE];
};

