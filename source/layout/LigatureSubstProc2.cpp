/*
 *
 * Copyright (C) 2016 and later: Unicode, Inc. and others.
 * License & terms of use: http://www.unicode.org/copyright.html
 *
 */

#include "LETypes.h"
#include "MorphTables.h"
#include "StateTables.h"
#include "MorphStateTables.h"
#include "SubtableProcessor2.h"
#include "StateTableProcessor2.h"
#include "LigatureSubstProc2.h"
#include "LEGlyphStorage.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN

#define ExtendedComplement(m) ((le_int32) (~((le_uint32) (m))))
#define SignBit(m) ((ExtendedComplement(m) >> 1) & (le_int32)(m))
#define SignExtend(v,m) (((v) & SignBit(m))? ((v) | ExtendedComplement(m)): (v))

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(LigatureSubstitutionProcessor2)

LigatureSubstitutionProcessor2::LigatureSubstitutionProcessor2(const LEReferenceTo<MorphSubtableHeader2> &morphSubtableHeader, LEErrorCode &success)
  : StateTableProcessor2(morphSubtableHeader, success), 
  ligActionOffset(0),
  componentOffset(0), ligatureOffset(0),  entryTable(), ligatureSubstitutionHeader(morphSubtableHeader, success)
{
    if (LE_FAILURE(success)) return;

    ligActionOffset = SWAPL(ligatureSubstitutionHeader->ligActionOffset);
    componentOffset = SWAPL(ligatureSubstitutionHeader->componentOffset);
    ligatureOffset = SWAPL(ligatureSubstitutionHeader->ligatureOffset);

    entryTable = LEReferenceToArrayOf<LigatureSubstitutionStateEntry2>(stHeader, success, entryTableOffset, LE_UNBOUNDED_ARRAY);
}

LigatureSubstitutionProcessor2::~LigatureSubstitutionProcessor2()
{
}

void LigatureSubstitutionProcessor2::beginStateTable()
{
    m = -1;
}

le_uint16 LigatureSubstitutionProcessor2::processStateEntry(LEGlyphStorage &glyphStorage, le_int32 &currGlyph, EntryTableIndex2 index, LEErrorCode &success)
{
    const LigatureSubstitutionStateEntry2 *entry = entryTable.getAlias(index, success);
    if(LE_FAILURE(success)) return 0;

    le_uint16 nextStateIndex = SWAPW(entry->nextStateIndex);
    le_uint16 flags = SWAPW(entry->entryFlags);
    le_uint16 ligActionIndex = SWAPW(entry->ligActionIndex);
    
    if (flags & lsfSetComponent) {
        if (++m >= nComponents) {
            m = 0;
        }
        componentStack[m] = currGlyph;
    } else if ( m == -1) {
        // bad font- skip this glyph.
        //LE_DEBUG_BAD_FONT("m==-1 (componentCount went negative)")
        currGlyph+= dir;
        return nextStateIndex;
    }

    ByteOffset actionOffset = flags & lsfPerformAction;

    if (actionOffset != 0) {
        LEReferenceTo<LigatureActionEntry> ap(stHeader, success, ligActionOffset); // byte offset
        ap.addObject(ligActionIndex - 1, success);  // index offset ( one before the actual start, because we will pre-increment)
        LEReferenceToArrayOf<TTGlyphID> ligatureTable(stHeader, success, ligatureOffset, LE_UNBOUNDED_ARRAY);
        LigatureActionEntry action;
        le_int32 offset, i = 0;
        le_int32 stack[nComponents];
        le_int16 mm = -1;

        LEReferenceToArrayOf<le_uint16> componentTable(stHeader, success, componentOffset, LE_UNBOUNDED_ARRAY);
        if(LE_FAILURE(success)) {
          currGlyph+= dir;
          return nextStateIndex; // get out! bad font
        }
        
        do {
            le_int32 componentGlyph = componentStack[m--]; // pop off

            ap.addObject(success);
            action = SWAPL(*ap.getAlias());

            if (m < 0) {
                m = nComponents - 1;
            }

            offset = action & lafComponentOffsetMask;
            if (offset != 0) {
                if(componentGlyph > glyphStorage.getGlyphCount() || componentGlyph < 0) {
                  LE_DEBUG_BAD_FONT("preposterous componentGlyph");
                  currGlyph+= dir;
                  return nextStateIndex; // get out! bad font
                }
                i += SWAPW(componentTable(LE_GET_GLYPH(glyphStorage[componentGlyph]) + (SignExtend(offset, lafComponentOffsetMask)),success));
                       
                if (action & (lafLast | lafStore))  {
                  TTGlyphID ligatureGlyph = SWAPW(ligatureTable(i,success));
                    glyphStorage[componentGlyph] = LE_SET_GLYPH(glyphStorage[componentGlyph], ligatureGlyph);
                    if(mm==nComponents) {
                      LE_DEBUG_BAD_FONT("exceeded nComponents");
                      mm--; // don't overrun the stack.
                    }
                    stack[++mm] = componentGlyph;
                    i = 0;
                } else {
                    glyphStorage[componentGlyph] = LE_SET_GLYPH(glyphStorage[componentGlyph], 0xFFFF);
                }
            }
#if LE_ASSERT_BAD_FONT
            if(m<0) {
              LE_DEBUG_BAD_FONT("m<0")
            }
#endif
        } while (!(action & lafLast) && (m>=0) ); // stop if last bit is set, or if run out of items

        while (mm >= 0) {
            if (++m >= nComponents) {
                m = 0;
            }

            componentStack[m] = stack[mm--];
        }
    }

    if (!(flags & lsfDontAdvance)) {
        currGlyph += dir;
    }

    return nextStateIndex;
}

void LigatureSubstitutionProcessor2::endStateTable()
{
}

U_NAMESPACE_END
