/*
 * @(#)$RCSFile$ $Revision: 1.1 $ $Date: 1999/08/16 21:51:06 $
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

struct DecompData {
    enum { MAX_CANONICAL = 21658 };
    enum { MAX_COMPAT = 11153 };
    enum { BASE = 0 };

    static const UChar offsets_index[];

    static const UChar offsets_values[];

    static const CompactShortArray* offsets;

    static const UChar contents[];

    static const UChar canonClass_index[];

    static const int8_t canonClass_values[];

    static CompactByteArray *canonClass;
};



