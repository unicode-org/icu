/*
 *******************************************************************************
 * Copyright (C) 1998-2002, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/layout/ScriptNameBuilder.java,v $
 * $Date: 2003/01/14 19:05:23 $
 * $Revision: 1.1 $
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

public class ScriptNameBuilder
{
    /*
     * This tool builds the script related header files and data tables needed by
     * the ICU LayoutEngine. By using the ICU4J interfaces to access the script
     * information, we can be sure that the data written by this tool is in synch
     * with ICU.
     */
    public static void main(String[] args)
    {
        ScriptData scriptData = new ScriptData();
        ScriptModuleWriter scriptModuleWriter = new ScriptModuleWriter(scriptData);
        
        scriptModuleWriter.writeScriptHeader("LEScripts.h");
        
        ScriptTagModuleWriter scriptTagModuleWriter = new ScriptTagModuleWriter(scriptData);
        
        scriptTagModuleWriter.writeScriptTags("ScriptAndLanguageTags");
        
        ScriptRunModuleWriter scriptRunModuleWriter = new ScriptRunModuleWriter(scriptData);
        
        scriptRunModuleWriter.writeScriptRuns("ScriptRunData.cpp");
    }
}