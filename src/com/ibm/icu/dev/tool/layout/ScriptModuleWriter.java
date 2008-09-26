/*
 *******************************************************************************
 * Copyright (C) 1998-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;


public class ScriptModuleWriter extends ModuleWriter
{
    public ScriptModuleWriter(ScriptData theScriptData, LanguageData theLanguageData)
    {
        super();
        
        scriptData = theScriptData;
        languageData = theLanguageData;
    }

    protected ScriptData scriptData;
    protected LanguageData languageData;
}