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


public class ScriptList
{
    static class LangSysRecord extends TaggedRecord
    {
        private Feature[] features;
        private int featureCount;
        
        public LangSysRecord(String theLanguageTag)
        {
            super(theLanguageTag);
            
            features = new Feature[10];
            featureCount = 0;
        }
        
        public void addFeature(Feature feature)
        {
            if (featureCount > features.length) {
                Feature[] newFeatures = new Feature[features.length + 5];
                
                System.arraycopy(features, 0, newFeatures, 0, features.length);
                features = newFeatures;
            }
            
            features[featureCount++] = feature;
        }
        
         public void writeLangSysRecord(OpenTypeTableWriter writer)
        {
            writer.writeData(0);      // lookupOrder (must be NULL)
            writer.writeData(0xFFFF); // reqFeatureIndex (0xFFFF means none)
            
            writer.writeData(featureCount);
            
            for (int i = 0; i < featureCount; i += 1) {
                writer.writeData(features[i].getFeatureIndex());
            }
        }
    }
    
    static class ScriptRecord extends TaggedRecord
    {
        private LangSysRecord[] langSysRecords;
        private int langSysCount;
        
        public ScriptRecord(String theScriptTag)
        {
            super(theScriptTag);
            langSysRecords = new LangSysRecord[10];
            langSysCount = 0;
        }
        
        public LangSysRecord findLangSysRecord(String languageTag)
        {
            for (int i = 0; i < langSysCount; i += 1) {
                LangSysRecord langSysRecord = langSysRecords[i];
                
                if (langSysRecord.getTag().equals(languageTag)) {
                    return langSysRecord;
                }
            }
            
            if (langSysCount >= langSysRecords.length) {
                LangSysRecord[] newLangSysRecords = new LangSysRecord[langSysCount + 5];
                
                System.arraycopy(langSysRecords, 0, newLangSysRecords, 0, langSysRecords.length);
                langSysRecords = newLangSysRecords;
            }
            
            LangSysRecord newLangSysRecord = new LangSysRecord(languageTag);
            langSysRecords[langSysCount] = newLangSysRecord;
            
            langSysCount += 1;
            return newLangSysRecord;
        }
        
        public void writeScriptRecord(OpenTypeTableWriter writer)
        {
            TaggedRecord.sort(langSysRecords, langSysCount);
            
            int scriptTableBase = writer.getOutputIndex();
            int firstLangSys = 0;
            
            writer.writeData(0); // default langSys offset (fixed later)
            
            if (langSysRecords[0].getTag().equals("(default)")) {
                firstLangSys = 1;
            }
            
            writer.writeData(langSysCount - firstLangSys);
            
            int langSysOffset = writer.getOutputIndex();
            
            for (int i = firstLangSys; i < langSysCount; i += 1) {
                writer.writeTag(langSysRecords[i].getTag());
                writer.writeData(0);
            }
            
            if (firstLangSys > 0) {
                System.out.print(" (default)");
                writer.fixOffset(scriptTableBase, scriptTableBase);
                langSysRecords[0].writeLangSysRecord(writer);
            }
            
            for (int i = firstLangSys; i < langSysCount; i += 1) {
                // fix the offset in the langSysRecordArray.
                // The "+2" skips over the tag and the "+3"
                // skips to the next langSysRecord entry
                writer.fixOffset(langSysOffset + 2, scriptTableBase);
                langSysOffset += 3;
                
                System.out.print(" '" + langSysRecords[i].getTag() + "'");
                langSysRecords[i].writeLangSysRecord(writer);
            }
        }
    }
    
    private ScriptRecord[] scriptRecords;
    private int scriptCount;
    
    public ScriptList()
    {
        scriptRecords = new ScriptRecord[10];
        scriptCount = 0;
    }
    
    private LangSysRecord findLangSysRecord(String scriptTag, String languageTag)
    {
        for (int i = 0; i < scriptCount; i += 1) {
            ScriptRecord scriptRecord = scriptRecords[i];
            
            if (scriptRecord.getTag().equals(scriptTag)) {
                 return scriptRecord.findLangSysRecord(languageTag);
            }
        }
        
        if (scriptCount >= scriptRecords.length) {
            ScriptRecord[] newScriptRecords = new ScriptRecord[scriptCount + 5];
            
            System.arraycopy(scriptRecords, 0, newScriptRecords, 0, scriptRecords.length);
            scriptRecords = newScriptRecords;
        }
        
        ScriptRecord newScriptRecord = new ScriptRecord(scriptTag);
        scriptRecords[scriptCount] = newScriptRecord;
        
        scriptCount += 1;
        return newScriptRecord.findLangSysRecord(languageTag);
    }
    
    public void addFeature(String scriptTag, String languageTag, Feature feature)
    {
        LangSysRecord langSysRecord = findLangSysRecord(scriptTag, languageTag);
        
        langSysRecord.addFeature(feature);
    }
    
    public void writeScriptList(OpenTypeTableWriter writer)
    {
        System.out.println("writing script list...");
        
        int scriptListBase = writer.getOutputIndex();
        
        TaggedRecord.sort(scriptRecords, scriptCount);
        writer.writeData(scriptCount);
        
        int scriptRecordOffset = writer.getOutputIndex();
        
        for (int i = 0; i < scriptCount; i += 1) {
            writer.writeTag(scriptRecords[i].getTag());
            writer.writeData(0);
        }
        
        for (int i = 0; i < scriptCount; i += 1) {
            // fix the offset in the scriptRecordArray.
            // The "+2" skips over the tag and the "+3"
            // skips to the next scriptRecord entry
            writer.fixOffset(scriptRecordOffset + 2, scriptListBase);
            scriptRecordOffset += 3;
            
            System.out.print("  script '" + scriptRecords[i].getTag() + "':");
            scriptRecords[i].writeScriptRecord(writer);
            System.out.println();
        }
    }
}
        
