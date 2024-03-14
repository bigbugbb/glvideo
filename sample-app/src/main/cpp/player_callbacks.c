//
// Created by Administrator on 2024/3/14.
//

#include "player_callbacks.h"

#include "ffmpeg_player.h"
#include "player_interface.h"

int onPlayerCallback(int nType, void* pUserData, void* pReserved) {
    LOGD("onPlayerCallback %d", nType);
    switch (nType) {
        case CALLBACK_CREATE_AUDIO_SERVICE:
            break;
        case CALLBACK_CREATE_VIDEO_SERVICE:
            break;
        case CALLBACK_UPDATE_PICTURE_SIZE:
            break;
        case CALLBACK_DELIVER_FRAME:
            break;
        case CALLBACK_PLAYBACK_FINISHED:
            break;
        case CALLBACK_ERROR:
            break;
        case CALLBACK_BEGIN_BUFFERING:
            break;
        case CALLBACK_ON_BUFFERING:
            break;
        case CALLBACK_END_BUFFERING:
            break;
        case CALLBACK_SEEK_POSITION:
            break;
        case CALLBACK_READ_INDEX:
            break;
        case CALLBACK_GET_DOWNLOAD_SPEED:
            break;
        case CALLBACK_OPEN_FINISHED:
            break;
        case CALLBACK_CLOSE_FINISHED:
            break;
        case CALLBACK_FRAME_CAPTURED:
            break;
        case CALLBACK_CHECK_DEVICE:
            break;
    }
}



