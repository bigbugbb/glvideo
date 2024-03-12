//
//  FFmpegCallbacks.cpp
//  QVOD
//
//  Created by bigbug on 11-11-16.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#include <iostream>
#include "../Utils.h"
#include "../Components.h"
#include "../QvodPlayer.h"
#include "../BufferingManager.h"
#include "../CallbackManager.h"
#include "CompInterfaces.h"
#include "FFmpegCallbacks.h"
using namespace::ios_qvod_player;

extern CCallbackManager* g_CallbackManager;

static int g_nInterrupt;
static CLock g_csInterrupt;

int avio_interrupt_cb() // 通过url_set_interrupt_cb传给ffmpeg的回调函数，用于终止网络数据读写
{
    CAutoLock cObjectLock(&g_csInterrupt);
    return g_nInterrupt;
}

void interrupt_avio()
{
    CAutoLock cObjectLock(&g_csInterrupt);
    g_nInterrupt = 1;
}

void maintain_avio()
{
    CAutoLock cObjectLock(&g_csInterrupt);
    g_nInterrupt = 0;
}
