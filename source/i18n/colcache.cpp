/*
*******************************************************************************
* Copyright (C) 1997-1999, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/
//===============================================================================
//
// File colcache.cpp
//
// CollationCache implements a persistent in-memory cache for
// TableCollationData objects.  The goal of CollationCache is to improve
// the memory footprint of a process which may have multiple threads
// loading up the same TableCollation object.  Performance improvement is
// strictly a secondary goal.
//
// Created by: Alan Liu
//
// Modification History:
//
//  Date        Name        Description
//  2/11/97     aliu        Creation.
//  2/12/97     aliu        Modified to work with TableCollationData.
//
//===============================================================================

#include "colcache.h"
#include "tcoldata.h"
#include "hash.h"

//--------------------------------------------------------------------------------
// CollationCache implementation
//--------------------------------------------------------------------------------

static void U_CALLCONV deleteTCD(void* TCD)
{
  delete (TableCollationData*)TCD;
}

CollationCache::CollationCache() : fHashtable()
{
  fHashtable.setValueDeleter(deleteTCD);
}

//eof
