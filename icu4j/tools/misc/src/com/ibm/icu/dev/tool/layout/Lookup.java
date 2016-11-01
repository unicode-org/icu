/*
 *******************************************************************************
 * Copyright (C) 1998-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Dec 3, 2003
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;


public /*abstract*/ class Lookup
{
    private int lookupType;
    private int lookupFlags;
    private LookupSubtable[] subtables;
    private int subtableCount;
    
    // Lookup flags
    public final static int LF_ReservedBit          = 0x0001;
    public final static int LF_IgnoreBaseGlyphs     = 0x0002;
    public final static int LF_IgnoreLigatures      = 0x0004;
    public final static int LF_IgnoreMarks          = 0x0008;
    public final static int LF_ReservedMask         = 0x00F0;
    public final static int LF_MarkAttachTypeMask   = 0xFF00;
    public final static int LF_MarkAttachTypeShift  = 8;
    
    // GSUB lookup types
    public final static int GSST_Single          = 1;
    public final static int GSST_Multiple        = 2;
    public final static int GSST_Alternate       = 3;
    public final static int GSST_Ligature        = 4;
    public final static int GSST_Context         = 5;
    public final static int GSST_ChainingContext = 6;

    // GPOS lookup types
    public final static int GPST_Single          = 1;
    public final static int GPST_Pair            = 2;
    public final static int GPST_Cursive         = 3;
    public final static int GPST_MarkToBase      = 4;
    public final static int GPST_MarkToLigature  = 5;
    public final static int GPST_MarkToMark      = 6;
    public final static int GPST_Context         = 7;
    public final static int GPST_ChainingContext = 8;
    
    public Lookup(int theLookupType, int theLookupFlags)
    {
        lookupType = theLookupType;
        lookupFlags = theLookupFlags;
        
        subtables = new LookupSubtable[10];
        subtableCount = 0;
    }
    
    public void addSubtable(LookupSubtable subtable)
    {
        if (subtableCount >= subtables.length) {
            LookupSubtable[] newSubtables = new LookupSubtable[subtables.length + 5];
            
            System.arraycopy(subtables, 0, newSubtables, 0, subtables.length);
            subtables = newSubtables;
        }
        
        subtables[subtableCount] = subtable;
        subtableCount += 1;
    }
    
    public void writeLookup(OpenTypeTableWriter writer)
    {
        int lookupBase = writer.getOutputIndex();
        
        writer.writeData(lookupType);
        writer.writeData(lookupFlags);
        writer.writeData(subtableCount);
        
        int subtableOffset = writer.getOutputIndex();
        
        for (int i = 0; i < subtableCount; i += 1) {
            writer.writeData(0);
        }
        
        for (int i = 0; i < subtableCount; i += 1) {
            writer.fixOffset(subtableOffset++, lookupBase);
            subtables[i].writeLookupSubtable(writer);
        }
    }
}