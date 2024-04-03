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

class CFramePool : public CSamplePool
{
public:
    CFramePool();
    virtual ~CFramePool();

    int Flush();
    int Reset();
protected:
    CFrame  m_Frames[FRAME_POOL_SIZE];
};

