//
// Created by Administrator on 2024/3/14.
//

#ifndef QVOD_ParamType_h
#define QVOD_ParamType_h

// 用于获取或设置播放器属性
#define PLAYER_GET_STATE                   0  //获得播放器状态：
#define PLAYER_GET_MEDIA_DURATION          1  //获得当前播放的音/视频长度(秒）
#define PLAYER_GET_MEDIA_CURRENT_TIME      2  //获得当前播放的音/视频时间点（秒）
#define PLAYER_GET_MEDIA_BITRATE           3
#define PLAYER_GET_MEDIA_FORMAT_NAME       4
#define PLAYER_GET_AUDIO_FORMAT_ID         5  //获得音频流的format id
#define PLAYER_GET_AUDIO_CHANNEL_COUNT     6
#define PLAYER_GET_AUDIO_TRACK_COUNT       7  //获得当前播放的音频流中channel总数
#define PLAYER_GET_AUDIO_SAMPLE_FORMAT     8
#define PLAYER_GET_AUDIO_SAMPLE_RATE       9  //获得音频的sample rate
#define PLAYER_GET_AUDIO_CURRENT_TRACK    10  //获得当前的音频channel索引号
#define PLAYER_GET_VIDEO_WIDTH            11  //获得当前图像的宽度（像素）
#define PLAYER_GET_VIDEO_HEIGHT           12  //获得当前图像的高度（像素）
#define PLAYER_GET_VIDEO_FORMAT_ID        13  //获得视频流的format id
#define PLAYER_GET_VIDEO_FPS              14  //获得视频流的fps
#define PLAYER_SET_VIDEO_LOOP_FILTER      15

#endif // QVOD_ParamType_h
