//
//  Global.cpp
//  QVOD
//
//  Created by bigbug on 11-11-21.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#include <iostream>
#include "Global.h"

void* align_malloc(size_t size, size_t align) // align should be smaller than 255
{
    char* data = (char*)malloc(size + align);
    int offset = align - (uintptr_t)data % align;
    *(data + offset - 1) = offset;
    
    return data + offset;
}

void align_free(void* ptr)
{
    char* data = (char*)ptr;
    int offset = *(data - 1);
    
    free(data - offset);
}
