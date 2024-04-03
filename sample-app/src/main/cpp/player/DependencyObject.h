//
//  DependencyObject.h
//  QvodPlayer
//
//  Created by bigbug on 12-2-25.
//  Copyright (c) 2012年 qvod. All rights reserved.
//

#pragma once

#include "BaseObject.h"
#include "Utils.h"

struct IDependency
{
    virtual int ReceiveEvent(void* pSender, int nEvent, DWORD dwParam1, DWORD dwParam2, void* pUserData) = 0;
};

class CDependencyObject : public CBaseObject
{
public:
    CDependencyObject();
    CDependencyObject(IDependency* pDepend);
    
    void SetDependency(IDependency* pDepend);
    
protected:
    virtual int NotifyEvent(int nEvent, DWORD dwParam1, DWORD dwParam2, void* pUserData);
    virtual int InterceptEvent(int nEvent, DWORD dwParam1, DWORD dwParam2, void* pUserData);
    
    IDependency*  m_pDepend;
};

