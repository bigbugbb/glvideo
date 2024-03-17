//
//  PlayerConsts.h
//  QVOD
//
//  Created by bigbug on 11-11-12.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_SysConsts_h
#define QVOD_SysConsts_h

#include "Config.h"
#include "CallbackType.h"
#include "ParamType.h"

#define S_OK                 0  
#define E_FAIL              -1  
#define E_NOIMPL            -2 
#define E_OUTOFMEMORY       -3   
#define E_IO                -4 
#define E_BADSTREAM         -5
#define E_NOCODECS          -6
#define E_UNSUPPORTED       -7
#define E_BADPREVIEW        -8
#define E_RETRY             -9
#define E_HANDLED           -10
#define E_SHOWNEXT          -11

#define STATE_LOADED              (1)
#define STATE_WAITFORRESOURCES    (1 << 1)
#define STATE_IDLE                (1 << 2)
#define STATE_EXECUTE             (1 << 3)
#define STATE_PAUSE               (1 << 4)
#define STATE_INVALID             (1 << 5)
#define STATE_UNLOADED            (1 << 6)
#define STATE_NONE                (1 << 7)

#define REQUEST_OUTPUT_AUDIO            0
#define REQUEST_OUTPUT_VIDEO            1
#define REQUEST_INTERRUPT_AUDIO         2

const int EVENT_CREATE_AUDIO            = 0;
const int EVENT_CREATE_VIDEO            = 1;
const int EVENT_UPDATE_MEDIA_START_TIME = 2;
const int EVENT_UPDATE_VIDEO_TIMEBASE   = 3;
const int EVENT_UPDATE_AUDIO_TIMEBASE   = 4;
const int EVENT_UPDATE_VIDEO_FRAME_SIZE = 5;
const int EVENT_DELIVER_FRAME           = 6;
const int EVENT_FRAME_CAPTURED          = 7;
const int EVENT_OPEN_FINISHED           = 8;
const int EVENT_EXECUTE_FINISHED        = 9;
const int EVENT_PAUSE_FINISHED          = 10;
const int EVENT_CLOSE_FINISHED          = 11;
const int EVENT_WAIT_FOR_RESOURCES      = 12;
const int EVENT_ENCOUNTER_ERROR         = 13;
const int EVENT_AUDIO_EOS               = 14;
const int EVENT_VIDEO_EOS               = 15;
const int EVENT_AUDIO_ONLY              = 16;
const int EVENT_VIDEO_ONLY              = 17;
const int EVENT_DISCARD_VIDEO_PACKET    = 18;
const int EVENT_AUDIO_NEED_DATA         = 19;

#endif
