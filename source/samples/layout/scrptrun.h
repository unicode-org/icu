/*
 *******************************************************************************
 *
 *   Copyright (C) 1999-2001, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  scrptrun.h
 *
 *   created on: 10/17/2001
 *   created by: Eric R. Mader
 */

#ifndef __SCRPTRUN_H
#define __SCRPTRUN_H

#include "unicode/utypes.h"
#include "unicode/uscript.h"

struct ScriptRecord
{
    UChar32 startChar;
    UChar32 endChar;
    UScriptCode scriptCode;
};

class ScriptRun
{
public:
    ScriptRun();

    ScriptRun(const UChar chars[], int32_t length);

    ScriptRun(const UChar chars[], int32_t start, int32_t length);

    void reset();

    void reset(int32_t start, int32_t count);

    void reset(const UChar chars[], int32_t start, int32_t length);

    int32_t getScriptStart();

    int32_t getScriptEnd();

    UScriptCode getScriptCode();

    UBool next();

private:

    static UBool sameScript(int32_t scriptOne, int32_t scriptTwo);

    int32_t charStart;
    int32_t charLimit;
    const UChar *charArray;

    int32_t scriptStart;
    int32_t scriptEnd;
    UScriptCode scriptCode;
};

inline ScriptRun::ScriptRun()
{
    reset(NULL, 0, 0);
}

inline ScriptRun::ScriptRun(const UChar chars[], int32_t length)
{
    reset(chars, 0, length);
}

inline ScriptRun::ScriptRun(const UChar chars[], int32_t start, int32_t length)
{
    reset(chars, start, length);
}

inline int32_t ScriptRun::getScriptStart()
{
    return scriptStart;
}

inline int32_t ScriptRun::getScriptEnd()
{
    return scriptEnd;
}

inline UScriptCode ScriptRun::getScriptCode()
{
    return scriptCode;
}

inline void ScriptRun::reset()
{
    scriptStart = charStart;
    scriptEnd   = charStart;
    scriptCode  = USCRIPT_INVALID_CODE;
}

inline void ScriptRun::reset(int32_t start, int32_t length)
{
    charStart = start;
    charLimit = start + length;

    reset();
}

inline void ScriptRun::reset(const UChar chars[], int32_t start, int32_t length)
{
    charArray = chars;

    reset(start, length);
}


#endif
