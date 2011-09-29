/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.tool.layout;


class Feature extends TaggedRecord
{
    private int[] lookupIndices;
    private int lookupCount;
    private int featureIndex;
    
    public Feature(String theFeatureTag)
    {
        super(theFeatureTag);
        
        lookupIndices = new int[10];
        lookupCount = 0;
        featureIndex = -1;
    }
    
    public void addLookup(int theLookupIndex)
    {
        if (lookupCount >= lookupIndices.length) {
            int[] newLookupIndices = new int[lookupIndices.length + 5];
            
            System.arraycopy(lookupIndices, 0, newLookupIndices, 0, lookupIndices.length);
            lookupIndices = newLookupIndices;
        }
        
        lookupIndices[lookupCount] = theLookupIndex;
        lookupCount += 1;
    }
    
    public void writeFeature(OpenTypeTableWriter writer)
    {
        writer.writeData(0);      // featureParams (must be NULL)
        
        writer.writeData(lookupCount);
        
        for (int i = 0; i < lookupCount; i += 1) {
            writer.writeData(lookupIndices[i]);
        }
    }
    
    public int getFeatureIndex()
    {
        return featureIndex;
    }
    
    public void setFeatureIndex(int index)
    {
        featureIndex = index;
    }
}