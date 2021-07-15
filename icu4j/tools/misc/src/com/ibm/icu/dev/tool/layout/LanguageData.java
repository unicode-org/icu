// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1998-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Apr 4, 2003
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

/**
 * This class holds the list of languages.
 * 
 * @author emader
 *
 */
public class LanguageData extends TagValueData
{
    public static class Record
    {
        private String tag;
        private String name;
        
        public Record(String tag, String name)
        {
            this.tag = tag;
            this.name = name;
        }
        
        public String tag()
        {
            return tag;
        }
        
        public String name()
        {
            return name;
        }
        
        public String label()
        {
            return TagUtilities.tagLabel(tag);
        }
        
        public String makeTag()
        {
            return TagUtilities.makeTag(tag);
        }
    }
        
    /*
     * This is temporary data until there is some resolution between
     * the OpenType language system tags and the ISO and RFC standards...
     */
    private Record[] languages =
    {
        new Record("",    "null"),
        new Record("ARA", "Arabic"),
        new Record("ASM", "Assamese"),
        new Record("BEN", "Bengali"),
        new Record("FAR", "Farsi"),
        new Record("GUJ", "Gujarati"),
        new Record("HIN", "Hindi"),
        new Record("IWR", "Hebrew"),
        new Record("JII", "Yiddish"),
        new Record("JAN", "Japanese"),
        new Record("KAN", "Kannada"),
        new Record("KOK", "Konkani"),
        new Record("KOR", "Korean"),
        new Record("KSH", "Kashmiri"),
        new Record("MAL", "Malayalam (Traditional)"),
        new Record("MAR", "Marathi"),
        new Record("MLR", "Malayalam (Reformed)"),
        new Record("MNI", "Manipuri"),
        new Record("ORI", "Oriya"),
        new Record("SAN", "Sanscrit"),
        new Record("SND", "Sindhi"),
        new Record("SNH", "Sinhalese"),
        new Record("SYR", "Syriac"),
        new Record("TAM", "Tamil"),
        new Record("TEL", "Telugu"),
        new Record("THA", "Thai"),
        new Record("URD", "Urdu"),
        new Record("ZHP", "Chinese (Phonetic)"),
        new Record("ZHS", "Chinese (Simplified)"),
        new Record("ZHT", "Chinese (Traditional)"),
        
        // languages added on 03/13/2008
        // TODO: need to deal with the fact that
        // these codes should be @draft, and the above
        // codes should be @final.
        new Record("AFK", "Afrikaans"),
        new Record("BEL", "Belarussian"),
        new Record("BGR", "Bulgarian"),
        new Record("CAT", "Catalan"),
        new Record("CHE", "Chechen"),
        new Record("COP", "Coptic"),
        new Record("CSY", "Czech"),
        new Record("DAN", "Danish"),
        new Record("DEU", "German"),
        new Record("DZN", "Dzongkha"),
        new Record("ELL", "Greek"),
        new Record("ENG", "English"),
        new Record("ESP", "Spanish"),
        new Record("ETI", "Estonian"),
        new Record("EUQ", "Basque"),
        new Record("FIN", "Finnish"),
      //new Record("FLE", "Flemish"), // Flemish has the same ISO 639-2 code as Dutch (NLD)
        new Record("FRA", "French"),
        new Record("GAE", "Gaelic"),
        new Record("HAU", "Hausa"),
        new Record("HRV", "Croation"),
        new Record("HUN", "Hungarian"),
        new Record("HYE", "Armenian"),
        new Record("IND", "Indonesian"),
        new Record("ITA", "Italian"),
        new Record("KHM", "Khmer"),
        new Record("MNG", "Mongolian"),
        new Record("MTS", "Maltese"),
        new Record("NEP", "Nepali"),
        new Record("NLD", "Dutch"),
        new Record("PAS", "Pashto"),
        new Record("PLK", "Polish"),
        new Record("PTG", "Portuguese"),
        new Record("ROM", "Romanian"),
        new Record("RUS", "Russian"),
        new Record("SKY", "Slovak"),
        new Record("SLV", "Slovenian"),
        new Record("SQI", "Albanian"),
        new Record("SRB", "Serbian"),
        new Record("SVE", "Swedish"),
        new Record("TIB", "Tibetan"),
        new Record("TRK", "Turkish"),
        new Record("WEL", "Welsh")
    };
    
    private int minLanguage = 0;
    private int maxLanguage = minLanguage + languages.length - 1;
    
    public int getMinValue()
    {
        return minLanguage;
    }
    
    public int getMaxValue()
    {
        return maxLanguage;
    }
    
    public String getTag(int value)
    {
        if (value < minLanguage || value > maxLanguage) {
            return null;
        }
        
        return languages[value - minLanguage].tag();
    }
    
    public String getTagLabel(int value)
    {
        if (value < minLanguage || value > maxLanguage) {
            return null;
        }
        
        return languages[value - minLanguage].label();
    }
    
    public String makeTag(int value)
    {
        if (value < minLanguage || value > maxLanguage) {
            return null;
        }
        
        return languages[value - minLanguage].makeTag();
    }
    
    public String getName(int value) {
        if (value < minLanguage || value > maxLanguage) {
            return "(UNKNOWN)";
        }
        
        return languages[value - minLanguage].name();
    }
}
