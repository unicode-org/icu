/*
 *
 * (C) Copyright IBM Corp. 1998-2004 - All Rights Reserved
 *
 */

#ifndef __CANONSHAPING_H
#define __CANONSHAPING_H

#include "LETypes.h"

U_NAMESPACE_BEGIN

class CanonShaping /* not : public UObject because all members are static */
{
public:
    static const le_uint8 glyphSubstitutionTable[];
};

U_NAMESPACE_END
#endif
