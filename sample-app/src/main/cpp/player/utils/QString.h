//
//  QString.h
//  QVOD
//
//  Created by bigbug on 11-11-12.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#pragma once


#include "BaseTypes.h"

namespace ios_qvod_player
{

#ifdef WIN32 /***************** WIN32 *****************/


#include <string.h>
#include <stdlib.h>
#include <tchar.h>

/* TCHAR */
typedef LPVOID				LPVOID;
typedef LPCVOID 			LPCVOID;
typedef LPCSTR				LPCSTR;
typedef LPCTSTR				LPCTSTR;
typedef LPTSTR				LPTSTR;
typedef LPSTR				LPSTR;
typedef LPCWSTR				LPCWSTR;


#else /***************** posix *****************/


#include <string.h>
#include <stdlib.h>
#include <wchar.h>
#include <ctype.h>

typedef const wchar_t*		LPCWSTR;
    
#ifdef _UNICODE  /* _UNICODE  */

typedef wchar_t 			TCHAR;
typedef void*				LPVOID;
typedef const void* 		LPCVOID;
typedef const char*			LPCSTR;
typedef const wchar_t*		LPCTSTR;
typedef wchar_t*			LPTSTR;
typedef char*				LPSTR;

#else /* non _UNICODE  */

typedef char 				TCHAR;
typedef void*				LPVOID;
typedef const void* 		LPCVOID;
typedef const char*			LPCSTR;
typedef const char*			LPCTSTR;
typedef char*				LPTSTR;
typedef char*				LPSTR;

#endif /* non _UNICODE  end */

char* itoa(int value, char* str, int radix);
int memcpy_s(void *dest, size_t numberOfElements, const void *src, size_t count);
int strcpy_s(char *dest, size_t numberOfElements, const char *src);

#define sprintf_s snprintf /* need modify */

#endif /***************** posix end *****************/

int strupr(char *str);
int lstrlenW(LPCWSTR str);
int lstrlen(LPCTSTR str);

} /* end of namespace ios_qvod_player */

