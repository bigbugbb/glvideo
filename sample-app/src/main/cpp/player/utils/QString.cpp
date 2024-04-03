//
//  String.cpp
//  QVOD
//
//  Created by bigbug on 11-11-12.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#include <iostream>

#include "QString.h"
#include <errno.h>

namespace ios_qvod_player
{

#ifdef WIN32 /* WIN32 */

//nothing to do

#else /* posix */


char* itoa(int value, char *str, int radix)
{
    static char r[1024] = {0};
	int  rem = 0;
	int  pos = 0;
	char ch  = '!';
    
	do {
		rem = value % radix;
		value /= radix;
        
		if (16 == radix) {
			if (rem >= 10 && rem <= 15) {
				switch( rem ) {
                    case 10: ch = 'a'; break;
                    case 11: ch = 'b'; break;
                    case 12: ch = 'c'; break;
                    case 13: ch = 'd'; break;
                    case 14: ch = 'e'; break;
                    case 15: ch = 'f'; break;
				}
			}
		}
        
		if ('!' == ch) {
			str[pos++] = (char)(rem + 0x30);
		} else {
			str[pos++] = ch;
		}
        
	} while(value != 0);
    
	str[pos] = '\0';
    
    /* reverse */
    int i = strlen(str) - 1;
    int j = 0;
    for (; i >= 0; i--, j++) {
        r[j] = str[i];
    }
    r[j] = '\0';
    
    /* copy r to str */
    memcpy(str, r, strlen(r));
    
	return r;
}


int memcpy_s(void *dest, size_t numberOfElements, const void *src, size_t count)
{
    if (numberOfElements >= count) {
        memcpy(dest, src, count);
        return 0;
    }
    
	return -1;
}

int strcpy_s(char *dest, size_t numberOfElements, const char *src)
{
    size_t len = strlen(src) + 1; /* include 0 */
    if (numberOfElements >= len) {
        strcpy(dest, src);
        return 0;
    }
    
    return -1;
}



#endif /* posix end */




int strupr(char *str)
{
#ifdef WIN32 /* WIN32 */
    
    _strupr(str);
    
#else /* posix */
    
    char *tmp = str;
    
    while (*tmp != '\0') {
        
        if (*tmp > 96 && *tmp < 123) {
            
            *tmp = *tmp - 32;
        }
        
        ++tmp;
    }
    
#endif /* posix end */
    
    return QVOD_OK;
}

int lstrlenW(LPCWSTR str)
{
#ifdef WIN32 /* WIN32 */
    
    return lstrlenW(str);
    
#else /* posix */
    
    return wcslen(str);
    
#endif /* posix end */
}

int lstrlen(LPCTSTR str)
{
#ifdef WIN32 /*** WIN32 ***/
    
    return lstrlen(str);
    
#else /*** posix ***/
    
#ifdef _UNICODE  /* _UNICODE  */
    
    return wcslen(str);
    
#else /* non _UNICODE  */
    
    return strlen(str);
    
#endif /* non _UNICODE  end */
    
#endif /*** posix end ***/
}
    
} /* end of namespace ios_qvod_player */


