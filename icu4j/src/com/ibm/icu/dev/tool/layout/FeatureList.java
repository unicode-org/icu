/*
 *******************************************************************************
 * Copyright (C) 1998-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Dec 3, 2003
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/layout/FeatureList.java,v $
 * $Date: 2003/12/09 01:18:11 $
 * $Revision: 1.1 $
 * 
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

import java.io.*;
import java.lang.*;
import java.util.*;

public class FeatureList
{
    
    static class FeatureRecord extends TaggedRecord
    {
        private int[] lookupIndices;
        private int lookupCount;
        
        public FeatureRecord(String theFeatureTag)
        {
            super(theFeatureTag);
            
            lookupIndices = new int[10];
            lookupCount = 0;
        }
        
        public void addLookup(int theLookupIndex)
        {
            if (lookupCount > lookupIndices.length) {
                int[] newLookupIndices = new int[lookupIndices.length + 5];
                
                System.arraycopy(lookupIndices, 0, newLookupIndices, 0, lookupIndices.length);
                lookupIndices = newLookupIndices;
            }
            
            lookupIndices[lookupCount] = theLookupIndex;
            lookupCount += 1;
        }
        
        public void writeFeatureRecord(OpenTypeTableWriter writer)
        {
            writer.writeData(0);      // featureParams (must be NULL)
            
            writer.writeData(lookupCount);
            
            for (int i = 0; i < lookupCount; i += 1) {
                writer.writeData(lookupIndices[i]);
            }
        }
    }
    
    private FeatureRecord[] featureRecords;
    private int featureCount;
    
    public FeatureList()
    {
        featureRecords = new FeatureRecord[10];
        featureCount = 0;
    }
        
    private FeatureRecord findFeatureRecord(String featureTag)
    {
        for (int i = 0; i < featureCount; i += 1) {
            FeatureRecord featureRecord = featureRecords[i];
            
            if (featureRecord.getTag().equals(featureTag)) {
                 return featureRecord;
            }
        }
        
        if (featureCount >= featureRecords.length) {
            FeatureRecord[] newFeatureRecords = new FeatureRecord[featureCount + 5];
            
            System.arraycopy(featureRecords, 0, newFeatureRecords, 0, featureRecords.length);
            featureRecords = newFeatureRecords;
        }
        
        FeatureRecord newFeatureRecord = new FeatureRecord(featureTag);
        featureRecords[featureCount] = newFeatureRecord;
        
        featureCount += 1;
        return newFeatureRecord;
    }
    
    public void addLookup(String featureTag, int lookupIndex)
    {
        FeatureRecord featureRecord = findFeatureRecord(featureTag);
            
        featureRecord.addLookup(lookupIndex);
    }
    
    public void finalizeFeatureList()
    {
        TaggedRecord.sort(featureRecords, featureCount);
    }
    
    public int getFeatureIndex(String featureTag)
    {
        return TaggedRecord.search(featureRecords, featureCount, featureTag);
    }
    
    public void writeFeaturetList(OpenTypeTableWriter writer)
    {
        System.out.print("writing feature list...");
        
        int featureListBase = writer.getOutputIndex();
        
        writer.writeData(featureCount);
        
        int featureRecordOffset = writer.getOutputIndex();
        
        for (int i = 0; i < featureCount; i += 1) {
            String tag = featureRecords[i].getTag();
            
            System.out.print(" '" + tag + "'");
            writer.writeTag(tag);
            writer.writeData(0);
        }
        
        for (int i = 0; i < featureCount; i += 1) {
            // fix the offset in the featureRecordArray.
            // The "+2" skips over the tag and the "+3"
            // skips to the next featureRecord entry
            writer.fixOffset(featureRecordOffset + 2, featureListBase);
            featureRecordOffset += 3;
            
            featureRecords[i].writeFeatureRecord(writer);
        }
        
        System.out.println();
    }
}