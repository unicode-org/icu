/*
******************************************************************************
*   Copyright (C) 1997-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
******************************************************************************
*   file name:  nfrlist.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
* Modification history
* Date        Name      Comments
* 10/11/2001  Doug      Ported from ICU4J
*/

#ifndef NFRLIST_H
#define NFRLIST_H

#include "unicode/utypes.h"
#include "unicode/uobject.h"
#include "cmemory.h"

#include "nfrule.h"

U_NAMESPACE_BEGIN

// unsafe class for internal use only.  assume memory allocations succeed, indexes are valid.
// should be a template, but we can't use them

class NFRuleList : public UObject {
protected:
    NFRule** fStuff;
    uint32_t fCount;
    uint32_t fCapacity;
public:
    NFRuleList(int capacity = 10) 
        : fStuff(capacity ? (NFRule**)uprv_malloc(capacity * sizeof(NFRule*)) : NULL)
        , fCount(0)
        , fCapacity(capacity) {};
    ~NFRuleList() {
        if (fStuff) {
            for(uint32_t i = 0; i < fCount; ++i) {
                delete fStuff[i];
            }
            uprv_free(fStuff);
        }
    }
    NFRule* operator[](uint32_t index) const { return fStuff[index]; }
    NFRule* remove(uint32_t index) {
        NFRule* result = fStuff[index];
        fCount -= 1;
        for (uint32_t i = index; i < fCount; ++i) { // assumes small arrays
            fStuff[i] = fStuff[i+1];
        }
        return result;
    }
    void add(NFRule* thing) {
        if (fCount == fCapacity) {
            fCapacity += 10;
            fStuff = (NFRule**)uprv_realloc(fStuff, fCapacity * sizeof(NFRule*)); // assume success
        }
        fStuff[fCount++] = thing;
    }
    uint32_t size() const { return fCount; }
    NFRule* last() const { return fCount > 0 ? fStuff[fCount-1] : NULL; }
    NFRule** release() {
        add(NULL); // ensure null termination
        NFRule** result = fStuff;
        fStuff = NULL;
        fCount = 0;
        fCapacity = 0;
        return result;
    }

    /**
     * ICU "poor man's RTTI", returns a UClassID for the actual class.
     *
     * @draft ICU 2.2
     */
    virtual inline UClassID getDynamicClassID() const { return getStaticClassID(); }

    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     *
     * @draft ICU 2.2
     */
    static inline UClassID getStaticClassID() { return (UClassID)&fgClassID; }

private:

    /**
     * The address of this static class variable serves as this class's ID
     * for ICU "poor man's RTTI".
     */
    static const char fgClassID;
};

U_NAMESPACE_END

// NFRLIST_H
#endif
