/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* 2002-09-20 aliu Created.
*/
#ifndef __PERFTIMER_H__
#define __PERFTIMER_H__

#include "unicode/utypes.h"

// Derived from Ram's perftime.h

//----------------------------------------------------------------------
// Win32

#if defined(WIN32) || defined(_WIN32) || defined(WIN64) || defined(_WIN64)

#include <windows.h>

class Timer {
    LARGE_INTEGER tstart, tend;
public:
    Timer() {}
    inline void start() {
        QueryPerformanceCounter(&tstart);
    }
    inline double stop() {
        QueryPerformanceCounter(&tend);
        LARGE_INTEGER freq;
        int result = QueryPerformanceFrequency(&freq);
        return ((double)(tend.QuadPart - tstart.QuadPart))/((double)freq.QuadPart);
    }
};
    
//----------------------------------------------------------------------
// UNIX

#else

#include <sys/time.h> 

class Timer {
    struct timeval tstart, tend;
    struct timezone tz;
public:
    Timer() {}
    inline void start() {
        gettimeofday(&tstart, &tz);
    }
    inline double stop() {
        gettimeofday(&tend, &tz);
        double t1, t2;
        t1 = (double)tstart.tv_sec + (double)tstart.tv_usec*1e-6;
        t2 = (double)tend.tv_sec + (double)tend.tv_usec*1e-6;
        return t2-t1;
    }
};

#endif
#endif
