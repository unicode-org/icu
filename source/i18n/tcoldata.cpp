/*
*******************************************************************************
* Copyright (C) 1997-1999, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/
//===============================================================================
//
// File tcoldata.cpp
//
// Internal file.  Implements TableCollationData, an internal class which is shared
// by TableCollation objects, and which contains all the invariant (and large)
// pieces of data.  Once created, TableCollationData objects do not change.
//
// Created by: Alan Liu
//
// Modification History:
//
//  Date        Name        Description
//  2/5/97      aliu        Creation.
//  3/5/97      aliu        Don't stream rule table in or out.
//===============================================================================

#include "ucmp32.h"
#include "tcoldata.h"
#include "tables.h"
#include "mutex.h"
#include "unicode/tblcoll.h"

//===============================================================================

CollationCache TableCollationData::fgCache;

//===============================================================================

TableCollationData::TableCollationData() 
    : isFrenchSec(FALSE),
      maxSecOrder(0),
      maxTerOrder(0),
      isRuleTableLoaded(FALSE),
      fBogus(FALSE)
{
    mapping = 0;
    contractTable = 0;
    expandTable = 0;
}

TableCollationData::~TableCollationData()
{
    ucmp32_close(mapping);
    delete contractTable;
    delete expandTable;
}

UBool
TableCollationData::isBogus() const
{
    return fBogus;
}

void TableCollationData::streamIn(FileStream* is)
{
    if (!T_FileStream_error(is))
    {
        // Stream in large objects
        char isNull;
        T_FileStream_read(is, &isNull, sizeof(isNull));
        if (isNull)
        {
            ucmp32_close(mapping);
            mapping = 0;
        }
        else
        {
            // Slight ugliness: We are a friend of TableCollation solely so
            // we can access the constant UNMAPPED here.  In fact, this code
            // path shouldn't really happen, because mapping should always != 0.
            if (mapping == 0) mapping = ucmp32_open(RuleBasedCollator::UNMAPPED);
            if (mapping->fBogus ){
                fBogus = TRUE;
                return;
            }
            ucmp32_streamIn(mapping, is);
            if (mapping->fBogus) {
                fBogus = TRUE;
                ucmp32_close(mapping);
                mapping = 0;
                return;
            }
        }

        T_FileStream_read(is, &isNull, sizeof(isNull));
        if (isNull)
        {
            delete contractTable;
            contractTable = 0;
        }
        else
        {
            if (contractTable == 0) contractTable = new VectorOfPToContractTable;
            if (contractTable->isBogus()) {
                fBogus = TRUE;
                return;
            }
            contractTable->streamIn(is);
            if (contractTable->isBogus()) {
                fBogus = TRUE;
                ucmp32_close(mapping);
                mapping = 0;
                delete contractTable;
                contractTable = 0;
                return;
            }
        }

        T_FileStream_read(is, &isNull, sizeof(isNull));
        if (isNull)
        {
            delete expandTable;
            expandTable = 0;
        }
        else
        {
            if (expandTable == 0) expandTable = new VectorOfPToExpandTable;
            if (expandTable->isBogus()) {
                fBogus = TRUE;
                return;
            }
            expandTable->streamIn(is);
            if (expandTable->isBogus()) {
                fBogus = TRUE;
                ucmp32_close(mapping);
                mapping = 0;
                delete contractTable;
                contractTable = 0;
                delete expandTable;
                expandTable = 0;
                return;
            }
        }

        // We don't stream in or out the rule table, in order to keep
        // binary files small.  We reconstruct rules on the fly.
        ruleTable.remove();
        isRuleTableLoaded = FALSE;

        // Stream in the small objects
        T_FileStream_read(is, &isFrenchSec, sizeof(isFrenchSec));
        T_FileStream_read(is, &maxSecOrder, sizeof(maxSecOrder));
        T_FileStream_read(is, &maxTerOrder, sizeof(maxTerOrder));
    }
}

void TableCollationData::streamOut(FileStream* os) const
{
    if (!T_FileStream_error(os))
    {
        // Stream out the large objects
        char isNull;
        isNull = (mapping == 0);
        T_FileStream_write(os, &isNull, sizeof(isNull));
        if (!isNull) ucmp32_streamOut(mapping, os);

        isNull = (contractTable == 0);
        T_FileStream_write(os, &isNull, sizeof(isNull));
        if (!isNull) contractTable->streamOut(os);

        isNull = (expandTable == 0);
        T_FileStream_write(os, &isNull, sizeof(isNull));
        if (!isNull) expandTable->streamOut(os);

        // We don't stream out the rule table, in order to keep
        // binary files small.  We reconstruct rules on the fly.

        // Stream out the small objects
        T_FileStream_write(os, &isFrenchSec, sizeof(isFrenchSec));
        T_FileStream_write(os, &maxSecOrder, sizeof(maxSecOrder));
        T_FileStream_write(os, &maxTerOrder, sizeof(maxTerOrder));
    }
}

void TableCollationData::streamIn(UMemoryStream* is)
{
    if (!uprv_mstrm_error(is))
    {
        // Stream in large objects
        char isNull;
        uprv_mstrm_read(is, &isNull, sizeof(isNull));
        if (isNull)
        {
            ucmp32_close(mapping);
            mapping = 0;
        }
        else
        {
            // Slight ugliness: We are a friend of TableCollation solely so
            // we can access the constant UNMAPPED here.  In fact, this code
            // path shouldn't really happen, because mapping should always != 0.
            if (mapping == 0) mapping = ucmp32_open(RuleBasedCollator::UNMAPPED);
            if (mapping->fBogus ){
                fBogus = TRUE;
                return;
            }
            ucmp32_streamMemIn(mapping, is);
            if (mapping->fBogus) {
                fBogus = TRUE;
                ucmp32_close(mapping);
                mapping = 0;
                return;
            }
        }

        uprv_mstrm_read(is, &isNull, sizeof(isNull));
        if (isNull)
        {
            delete contractTable;
            contractTable = 0;
        }
        else
        {
            if (contractTable == 0) contractTable = new VectorOfPToContractTable;
            if (contractTable->isBogus()) {
                fBogus = TRUE;
                return;
            }
            contractTable->streamIn(is);
            if (contractTable->isBogus()) {
                fBogus = TRUE;
                ucmp32_close(mapping);
                mapping = 0;
                delete contractTable;
                contractTable = 0;
                return;
            }
        }

        uprv_mstrm_read(is, &isNull, sizeof(isNull));
        if (isNull)
        {
            delete expandTable;
            expandTable = 0;
        }
        else
        {
            if (expandTable == 0) expandTable = new VectorOfPToExpandTable;
            if (expandTable->isBogus()) {
                fBogus = TRUE;
                return;
            }
            expandTable->streamIn(is);
            if (expandTable->isBogus()) {
                fBogus = TRUE;
                ucmp32_close(mapping);
                mapping = 0;
                delete contractTable;
                contractTable = 0;
                delete expandTable;
                expandTable = 0;
                return;
            }
        }

        // We don't stream in or out the rule table, in order to keep
        // binary files small.  We reconstruct rules on the fly.
        ruleTable.remove();
        isRuleTableLoaded = FALSE;

        // Stream in the small objects
        uprv_mstrm_read(is, &isFrenchSec, sizeof(isFrenchSec));
        uprv_mstrm_read(is, &maxSecOrder, sizeof(maxSecOrder));
        uprv_mstrm_read(is, &maxTerOrder, sizeof(maxTerOrder));
    }
}

void TableCollationData::streamOut(UMemoryStream* os) const
{
    if (!uprv_mstrm_error(os))
    {
        // Stream out the large objects
        char isNull;
        isNull = (mapping == 0);
        uprv_mstrm_write(os, (uint8_t *)&isNull, sizeof(isNull));
        if (!isNull) ucmp32_streamMemOut(mapping, os);

        isNull = (contractTable == 0);
        uprv_mstrm_write(os, (uint8_t *)&isNull, sizeof(isNull));
        if (!isNull) contractTable->streamOut(os);

        isNull = (expandTable == 0);
        uprv_mstrm_write(os, (uint8_t *)&isNull, sizeof(isNull));
        if (!isNull) expandTable->streamOut(os);

        // We don't stream out the rule table, in order to keep
        // binary files small.  We reconstruct rules on the fly.

        // Stream out the small objects
        uprv_mstrm_write(os, (uint8_t *)&isFrenchSec, sizeof(isFrenchSec));
        uprv_mstrm_write(os, (uint8_t *)&maxSecOrder, sizeof(maxSecOrder));
        uprv_mstrm_write(os, (uint8_t *)&maxTerOrder, sizeof(maxTerOrder));
    }
}

void TableCollationData::addToCache(const UnicodeString& key, TableCollationData* collation)
{
    Mutex lock;
    fgCache.Add(key, collation);
}

TableCollationData* TableCollationData::findInCache(const UnicodeString& key)
{
    Mutex lock;
    return fgCache.Find(key);
}

//eof


