//
//  FFmpegData.h
//  QVOD
//
//  Created by bigbug on 11-11-15.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_FFmpegData_h
#define QVOD_FFmpegData_h

#ifdef __cplusplus
extern "C" {
#endif
    
#include "libavformat/avformat.h"
#include "libavformat/url.h"
#include "libswscale/swscale.h"
#include "libavutil/mathematics.h"
#include "libavutil/imgutils.h"
#include "libavcodec/avcodec.h"
#include "libavdevice/avdevice.h"
    
#ifdef __cplusplus
}
#endif


#define MAX_AUDIO_TRACKS    8
#define AVCODEC_MAX_AUDIO_FRAME_SIZE    192000

struct FormatInfo
{
    AVFormatContext* pFormatContext;
    
    AVCodecContext*  pVideoContext;
    AVCodecContext*  pAudioContext[MAX_AUDIO_TRACKS];
    AVCodecContext*	 pSubtitleContext;
    
    const AVCodec*   pVideoCodec;
    const AVCodec*   pAudioCodec[MAX_AUDIO_TRACKS];
    const AVCodec*   pSubtitleCodec;

    int              nVideoStreamIdx;
    int              nAudioStreamIdx;
    int              nSubtitleStreamIdx;
    
    bool             bDecodeAudio;
    bool             bDecodeVideo;
};

struct VideoInfo
{
    int         nWidth;
    int         nHeight;
    int         nFormatID;
    int         nBitrate;
    int64_t     llFormatDuration;
    int64_t     llDuration;
    double      lfFPS;
    double      lfTimebase;
};

typedef enum AVSampleFormat SampleFormat;

struct AudioInfo
{
    int          nSampleRate;
    int          nFormatID;
    int          nChannelsPerFrame;
    int          nFramesPerPacket;
    int          nBitrate;
    int          nCurTrack;
    int          nTrackCount;
    SampleFormat nSampleFormat;
    double       lfTimebase;
    AVRational   avrTimebase;
};

#endif
