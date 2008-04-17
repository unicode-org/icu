/********************************************************************
 * COPYRIGHT:
 * Copyright (C) 2008 IBM, Inc.   All Rights Reserved.
 *
 ********************************************************************/
#ifndef _STRSRCHPERF_H
#define _STRSRCHPERF_H

#include "unicode/uperf.h"
#include <stdlib.h>
#include <stdio.h>

class StringSearchPerfFunction : public UPerfFunction {
private:
    
public:
    virtual void call(UErrorCode* status);
    virtual long getOperationsPerIteration();
    virtual long getEventsPerIteration();
};

class StringSearchPerformanceTest : public UPerfTest {
private:
    
public:
    StringSearchPerformanceTest(int32_t argc, const char *argv[], UErrorCode &status);
    ~StringSearchPerformanceTest();
    virtual UPerfFunction* runIndexedTest(int32_t index, UBool exec, const char *&name, char *par = NULL);
};

#endif /* _STRSRCHPERF_H */
