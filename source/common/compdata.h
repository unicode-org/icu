/*
************************************************************************
*   Copyright (c) 1997-2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
************************************************************************
* > THIS FILE WAS MACHINE GENERATED <
* >       DO NOT EDIT BY HAND       <
* >      RUN TOOL TO REGENERATE     <
* Tool: com.ibm.text.Normalizer
* Creation date: Tue Jul 25 11:14:48 PDT 2000
*/
#include "unicode/utypes.h"
#include "ucmp8.h"
#include "ucmp16.h"

struct ComposeData {
    enum { BASE_COUNT = 748 };
    enum { COMBINING_COUNT = 55 };
    enum { MAX_COMPAT = 4341 };
    enum { MAX_CANONICAL = 5199 };
    enum { MAX_COMPOSED = 0xFB1D };
    enum { MAX_INDEX = 8982 };
    enum { INITIAL_JAMO_INDEX = 8983 };
    enum { MEDIAL_JAMO_INDEX = 8984 };
    enum { MAX_BASES = 1024 };
    enum { MAX_COMBINE = 64 };
    enum { TYPE_MASK = 0x7 };
    enum { INDEX_SHIFT = 3 };
    enum { IGNORE = 0 };
    enum { BASE = 1 };
    enum { NON_COMPOSING_COMBINING = 2 };
    enum { COMBINING = 3 };
    enum { INITIAL_JAMO = 4 };
    enum { MEDIAL_JAMO = 5 };
    enum { FINAL_JAMO = 6 };
    enum { HANGUL = 7 };

    static const uint16_t lookup_index[];

    static const uint16_t lookup_values[];

    static const CompactShortArray* lookup;

    static const uint16_t actions_index[];

    static const uint16_t actions_values[];

    static const CompactShortArray* actions;

    static const uint16_t actionIndex[];

    static const uint16_t replace[];

    static const int32_t typeBit[];
};
