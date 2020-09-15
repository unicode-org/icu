// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
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


public class FeatureList
{
    
    private Feature[] features;
    private int featureCount;
    
    public FeatureList()
    {
        features = new Feature[10];
        featureCount = 0;
    }
        
    public void addFeature(Feature feature)
    {
        if (featureCount >= features.length) {
            Feature[] newFeatures = new Feature[features.length + 5];
            
            System.arraycopy(features, 0, newFeatures, 0, features.length);
            features = newFeatures;
        }
        
        features[featureCount++] = feature;
    }
    
    public void finalizeFeatureList()
    {
        TaggedRecord.sort(features, featureCount);
        
        for (int i = 0; i < featureCount; i += 1) {
            features[i].setFeatureIndex(i);
        }
    }
    
    public void writeFeaturetList(OpenTypeTableWriter writer)
    {
        System.out.print("writing feature list...");
        
        int featureListBase = writer.getOutputIndex();
        
        writer.writeData(featureCount);
        
        int featureRecordOffset = writer.getOutputIndex();
        
        for (int i = 0; i < featureCount; i += 1) {
            String tag = features[i].getTag();
            
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
            
            features[i].writeFeature(writer);
        }
        
        System.out.println();
    }
}
