/*
 * @(#)$RCSFile$ $Revision: 1.1 $ $Date: 1999/08/16 21:51:05 $
 *
 * (C) Copyright IBM Corp. 1997-1998 - All Rights Reserved
 *
 * The program is provided 'as is' without any warranty express or
 * implied, including the warranty of non-infringement and the implied
 * warranties of merchantibility and fitness for a particular purpose.
 * IBM will not be liable for any damages suffered by you as a result
 * of using the Program. In no event will IBM be liable for any
 * special, indirect or consequential damages or lost profits even if
 * IBM has been advised of the possibility of their occurrence. IBM
 * will not be liable for any third party claims against you.
 *
 * This class is MACHINE GENERATED.  Run NormalizerBuilder to regenerate.
 */


#include "utypes.h"
#include "ucmp8.h"
#include "ucmp16.h"

struct ComposeData {
    enum { BASE_COUNT = 805 };
    enum { COMBINING_COUNT = 59 };
    enum { MAX_COMPAT = 4215 };
    enum { MAX_CANONICAL = 4882 };
    enum { MAX_COMPOSED = 0xFB4E };
    enum { MAX_INDEX = 8704 };
    enum { INITIAL_JAMO_INDEX = 8705 };
    enum { MEDIAL_JAMO_INDEX = 8706 };
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

    static const UChar lookup_index[];

    static const UChar lookup_values[];

    static const CompactShortArray* lookup;

    static const UChar actions_index[];

    static const UChar actions_values[];

    static const CompactShortArray* actions;

    static const UChar actionIndex[];

    static const UChar replace[];

    static const int32_t typeMask[];
};
