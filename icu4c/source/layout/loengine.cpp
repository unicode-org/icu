/*
 *
 * (C) Copyright IBM Corp. 1998-2004 - All Rights Reserved
 *
 */

#include "unicode/loengine.h"


/*
 * This file is needed to make sure that the
 * inline methods defined in loengine.h are
 * exported by the build...
 */

#ifndef U_HIDE_OBSOLETE_API
UOBJECT_DEFINE_RTTI_IMPLEMENTATION(ICULayoutEngine)
#endif
