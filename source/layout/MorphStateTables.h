/*
 * @(#)MorphStateTables.h	1.4 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#ifndef __MORPHSTATETABLES_H
#define __MORPHSTATETABLES_H

#include "LETypes.h"
#include "LayoutTables.h"
#include "MorphTables.h"
#include "StateTables.h"

U_NAMESPACE_BEGIN

struct MorphStateTableHeader : MorphSubtableHeader
{
    StateTableHeader stHeader;
};

U_NAMESPACE_END
#endif
