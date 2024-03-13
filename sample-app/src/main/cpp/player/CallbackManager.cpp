//
//  CallbackManager.cpp
//  QVOD
//
//  Created by bigbug on 11-11-21.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#include <iostream>
#include "SysConsts.h"
#include "CallbackManager.h"
using namespace::std;


CCallbackManager::CCallbackManager()
{
    
}

CCallbackManager::~CCallbackManager()
{
    for (const auto& [nType, pCallback] : m_mapCallbacks) {
        if (pCallback) {
            delete pCallback;
        }
    }
    m_mapCallbacks.clear();
}

CCallbackManager* CCallbackManager::GetInstance()
{
    static CCallbackManager s_CallbackManager;
    
    return &s_CallbackManager;
}

int CCallbackManager::SetCallback(int nType, PCallback pfnCallback, void* pUserData, void* pReserved)
{
    CallbackData* pData = new CallbackData();

    pData->nCallbackType = nType;
    pData->pfnCallback   = pfnCallback;
    pData->pUserData     = pUserData;
    pData->pReserved     = pReserved;
    AssertValid(pfnCallback);

    if (m_mapCallbacks[nType]) {
        delete m_mapCallbacks[nType];
    }
    m_mapCallbacks[nType] = pData;
    
    return S_OK;
}

CallbackData* CCallbackManager::GetCallbackData(int nType)
{
    return m_mapCallbacks[nType];
}
