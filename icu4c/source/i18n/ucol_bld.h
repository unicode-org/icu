#ifndef UCOL_BLD_H
#define UCOL_BLD_H
#include <stdio.h>
#include <stdlib.h>

#include "ucol_imp.h"
#include "ucol_tok.h"
#include "ucol_elm.h"

#include "uhash.h"
#include "ucmp16.h"
#include "umutex.h"
#include "cpputils.h"

#include "unicode/ustring.h"
#include "unicode/unistr.h"
#include "unicode/normlzr.h"


const InverseTableHeader *ucol_initInverseUCA(UErrorCode *status);
UCATableHeader *ucol_assembleTailoringTable(UColTokenParser *src, UErrorCode *status);

#endif