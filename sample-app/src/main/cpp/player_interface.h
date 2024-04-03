//
//  PlayerInterface.h
//  QVOD
//
//  Created by bigbug on 11-11-21.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#include "player/Config.h"
#include "player/CallbackType.h"

// Android中向Java层发送的消息号
#define ON_OPENED					1
#define ON_CLOSED					2
#define ON_COMPLETION				3
#define ON_BEGIN_BUFFERING			4
#define ON_END_BUFFERING			5
#define ON_BUFFERING				6
#define ON_VIDEO_SIZE_CHANGED		7
#define ON_NOTIFY_SEEK_POSITION		8
#define ON_NOTIFY_READ_INDEX		9
#define ON_ERROR       				10

// 外部请求的事件
#define REQUEST_OUTPUT_AUDIO        0
#define REQUEST_OUTPUT_VIDEO        1
#define REQUEST_INTERRUPT_AUDIO     2
    
// 外部用到的错误码
#define S_OK                 0
#define E_FAIL              -1
#define E_NOIMPL            -2
#define E_OUTOFMEMORY       -3
#define E_IO                -4
#define E_BADSTREAM         -5
#define E_NOCODECS          -6
#define E_UNSUPPORTED       -7
#define E_BADPREVIEW        -8
    
// 外部可能用到的播放器状态码
#define STATE_LOADED        (1)
#define STATE_EXECUTE       (1 << 3)
#define STATE_PAUSE         (1 << 4)
#define STATE_INVALID       (1 << 5)
#define STATE_UNLOADED      (1 << 6)
#define STATE_NONE          (1 << 7)

typedef int (*PCallback)(int nType, void* pUserData, void* pReserved);

typedef struct _FRAMEINFO
{
    int    nWidth;
    int    nHeight;
    int    nStride;
    int    nFormat;
    void*  pContent;
} FRAMEINFO;
    
int CreatePlayer(const char* szPath);
int DestroyPlayer();
int Open(const char* pszURL, double lfOffset, int nRemote);
int Close();
int Play();
int Pause();
int Seek(double lfTime);
int CaptureFrame();
int SetParameter(int nParam, void* pValue);
int GetParameter(int nParam, void* pValue);
int SetCallback(int nType, PCallback pfnCallback, void* pUserData, void* pReserved);
int SendRequest(int nType, int nParam1, int nParam2, void* pUserData, void* pReserved);

#ifdef __cplusplus
}
#endif

