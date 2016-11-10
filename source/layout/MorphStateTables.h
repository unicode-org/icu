/*
 *
 * Copyright (C) 2016 and later: Unicode, Inc. and others. License & terms of use: http://www.unicode.org/copyright.html
 *
 */

#ifndef __MORPHSTATETABLES_H
#define __MORPHSTATETABLES_H

/**
 * \file
 * \internal
 */

#include "LETypes.h"
#include "LayoutTables.h"
#include "MorphTables.h"
#include "StateTables.h"

U_NAMESPACE_BEGIN

struct MorphStateTableHeader : MorphSubtableHeader
{
    StateTableHeader stHeader;
};

struct MorphStateTableHeader2 : MorphSubtableHeader2
{
    StateTableHeader2 stHeader;
};

U_NAMESPACE_END
#endif
