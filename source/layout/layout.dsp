# Microsoft Developer Studio Project File - Name="layout" - Package Owner=<4>
# Microsoft Developer Studio Generated Build File, Format Version 6.00
# ** DO NOT EDIT **

# TARGTYPE "Win32 (x86) Dynamic-Link Library" 0x0102

CFG=layout - Win32 Debug
!MESSAGE This is not a valid makefile. To build this project using NMAKE,
!MESSAGE use the Export Makefile command and run
!MESSAGE 
!MESSAGE NMAKE /f "layout.mak".
!MESSAGE 
!MESSAGE You can specify a configuration when running NMAKE
!MESSAGE by defining the macro CFG on the command line. For example:
!MESSAGE 
!MESSAGE NMAKE /f "layout.mak" CFG="layout - Win32 Debug"
!MESSAGE 
!MESSAGE Possible choices for configuration are:
!MESSAGE 
!MESSAGE "layout - Win32 Release" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE "layout - Win32 Debug" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE 

# Begin Project
# PROP AllowPerConfigDependencies 0
# PROP Scc_ProjName ""
# PROP Scc_LocalPath ""
CPP=cl.exe
MTL=midl.exe
RSC=rc.exe

!IF  "$(CFG)" == "layout - Win32 Release"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 0
# PROP BASE Output_Dir "Release"
# PROP BASE Intermediate_Dir "Release"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 0
# PROP Output_Dir "..\..\lib\"
# PROP Intermediate_Dir "Release"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MT /W3 /GX /O2 /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "LAYOUT_EXPORTS" /YX /FD /c
# ADD CPP /nologo /MT /W3 /GX /O2 /I "..\..\common.\opentype .\aat" /I "..\common" /I "opentype" /I "aat" /I "..\layout" /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "LAYOUT_EXPORTS" /D "U_LAYOUT_IMPLEMENTATION" /YX /FD /c
# ADD BASE MTL /nologo /D "NDEBUG" /mktyplib203 /win32
# ADD MTL /nologo /D "NDEBUG" /mktyplib203 /win32
# ADD BASE RSC /l 0x409 /d "NDEBUG"
# ADD RSC /l 0x409 /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /machine:I386
# ADD LINK32 /nologo /dll /machine:I386 /out:"..\..\bin\icule17.dll"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 1
# PROP BASE Output_Dir "layout___Win32_Debug"
# PROP BASE Intermediate_Dir "layout___Win32_Debug"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 1
# PROP Output_Dir "..\..\lib\"
# PROP Intermediate_Dir "Debug"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MTd /W3 /Gm /GX /ZI /Od /D "WIN32" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "LAYOUT_EXPORTS" /YX /FD /GZ /c
# ADD CPP /nologo /MTd /W3 /Gm /GX /ZI /Od /I "..\common" /I "..\layout" /I "opentype" /I "aat" /D "WIN32" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "LAYOUT_EXPORTS" /D "U_LAYOUT_IMPLEMENTATION" /YX /FD /GZ /c
# ADD BASE MTL /nologo /D "_DEBUG" /mktyplib203 /win32
# ADD MTL /nologo /D "_DEBUG" /mktyplib203 /win32
# ADD BASE RSC /l 0x409 /d "_DEBUG"
# ADD RSC /l 0x409 /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /debug /machine:I386 /pdbtype:sept
# ADD LINK32 /nologo /dll /debug /machine:I386 /out:"..\..\bin\icule17d.dll" /pdbtype:sept

!ENDIF 

# Begin Target

# Name "layout - Win32 Release"
# Name "layout - Win32 Debug"
# Begin Group "Source Files"

# PROP Default_Filter "cpp;c;cxx;rc;def;r;odl;idl;hpj;bat"
# Begin Source File

SOURCE=.\opentype\AlternateSubstitutionSubtables.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\AnchorTables.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ArabicLayoutEngine.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\ArabicLigatureData.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\ArabicShaping.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\CDACLayout.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\CDACLigatureData.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\ClassDefinitionTables.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\aat\ContextualGlyphSubstitutionProcessor.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\ContextualSubstitutionSubtables.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\CoverageTables.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\DeviceTables.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\Features.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\GDEFMarkFilter.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\GlyphDefinitionTables.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\GlyphIterator.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\GlyphPositioningLookupProcessor.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\GlyphPositioningTables.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\GlyphSubstitutionLookupProcessor.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\GlyphSubstitutionTables.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\GXLayoutEngine.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\HebrewLigatureData.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\HebrewShaping.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\IndicClassTables.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\IndicLayoutEngine.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\aat\IndicRearrangementProcessor.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\IndicReordering.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\LayoutEngine.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\aat\LigatureSubstitutionProcessor.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\LigatureSubstitutionSubtables.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\LookupProcessor.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\Lookups.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\aat\LookupTables.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\MarkArrays.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\MarkToBasePositioningSubtables.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\MarkToLigaturePositioningSubtables.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\MarkToMarkPositioningSubtables.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\aat\MorphTables.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\MultipleSubstitutionSubtables.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\aat\NonContextualGlyphSubstitutionProcessor.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\OpenTypeLayoutEngine.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\OpenTypeUtilities.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\PairPositioningSubtables.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\ScriptAndLanguage.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\aat\SegmentArrayProcessor.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\aat\SegmentSingleProcessor.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\aat\SimpleArrayProcessor.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\SinglePositioningSubtables.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\SingleSubstitutionSubtables.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\aat\SingleTableProcessor.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\aat\StateTableProcessor.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\SubstitutionLookups.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\aat\SubtableProcessor.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ThaiLayoutEngine.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ThaiShaping.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\ThaiStateTables.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\aat\TrimmedArrayProcessor.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\opentype\ValueRecords.cpp

!IF  "$(CFG)" == "layout - Win32 Release"

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# PROP Intermediate_Dir "debug"

!ENDIF 

# End Source File
# End Group
# Begin Group "Header Files"

# PROP Default_Filter "h;hpp;hxx;hm;inl"
# Begin Source File

SOURCE=.\opentype\AlternateSubstitutionSubtables.h
# End Source File
# Begin Source File

SOURCE=.\opentype\AnchorTables.h
# End Source File
# Begin Source File

SOURCE=.\ArabicLayoutEngine.h
# End Source File
# Begin Source File

SOURCE=.\opentype\ArabicShaping.h
# End Source File
# Begin Source File

SOURCE=.\opentype\AttachmentPositioningSubtables.h
# End Source File
# Begin Source File

SOURCE=.\opentype\CDACLayout.h
# End Source File
# Begin Source File

SOURCE=.\opentype\ClassDefinitionTables.h
# End Source File
# Begin Source File

SOURCE=.\aat\ContextualGlyphInsertion.h
# End Source File
# Begin Source File

SOURCE=.\aat\ContextualGlyphSubstitution.h
# End Source File
# Begin Source File

SOURCE=.\aat\ContextualGlyphSubstitutionProcessor.h
# End Source File
# Begin Source File

SOURCE=.\opentype\ContextualSubstitutionSubtables.h
# End Source File
# Begin Source File

SOURCE=.\opentype\CoverageTables.h
# End Source File
# Begin Source File

SOURCE=.\opentype\DeviceTables.h
# End Source File
# Begin Source File

SOURCE=.\opentype\Features.h
# End Source File
# Begin Source File

SOURCE=.\opentype\GDEFMarkFilter.h
# End Source File
# Begin Source File

SOURCE=.\opentype\GlyphDefinitionTables.h
# End Source File
# Begin Source File

SOURCE=.\opentype\GlyphIterator.h
# End Source File
# Begin Source File

SOURCE=.\opentype\GlyphPositionAdjustments.h
# End Source File
# Begin Source File

SOURCE=.\opentype\GlyphPositioningLookupProcessor.h
# End Source File
# Begin Source File

SOURCE=.\opentype\GlyphPositioningTables.h
# End Source File
# Begin Source File

SOURCE=.\opentype\GlyphSubstitutionLookupProcessor.h
# End Source File
# Begin Source File

SOURCE=.\opentype\GlyphSubstitutionTables.h
# End Source File
# Begin Source File

SOURCE=.\GXLayoutEngine.h
# End Source File
# Begin Source File

SOURCE=.\opentype\HebrewShaping.h
# End Source File
# Begin Source File

SOURCE=.\opentype\HindiFeatureTags.h
# End Source File
# Begin Source File

SOURCE=.\IndicLayoutEngine.h
# End Source File
# Begin Source File

SOURCE=.\aat\IndicRearrangement.h
# End Source File
# Begin Source File

SOURCE=.\aat\IndicRearrangementProcessor.h
# End Source File
# Begin Source File

SOURCE=.\opentype\IndicReordering.h
# End Source File
# Begin Source File

SOURCE=.\LayoutEngine.h

!IF  "$(CFG)" == "layout - Win32 Release"

# Begin Custom Build
InputPath=.\LayoutEngine.h

"..\..\include\layout\LayoutEngine.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    LayoutEngine.h    ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# Begin Custom Build
InputPath=.\LayoutEngine.h

"..\..\include\layout\LayoutEngine.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    LayoutEngine.h    ..\..\include\layout

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\aat\LayoutTables.h
# End Source File
# Begin Source File

SOURCE=.\LEFontInstance.h

!IF  "$(CFG)" == "layout - Win32 Release"

# Begin Custom Build
InputPath=.\LEFontInstance.h

"..\..\include\layout\LEFontInstance.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    LEFontInstance.h    ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# Begin Custom Build
InputPath=.\LEFontInstance.h

"..\..\include\layout\LEFontInstance.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    LEFontInstance.h    ..\..\include\layout

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\LEGlyphFilter.h

!IF  "$(CFG)" == "layout - Win32 Release"

# Begin Custom Build
InputPath=.\LEGlyphFilter.h

"..\..\include\layout\LEGlyphFilter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    LEGlyphFilter.h    ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# Begin Custom Build
InputPath=.\LEGlyphFilter.h

"..\..\include\layout\LEGlyphFilter.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    LEGlyphFilter.h    ..\..\include\layout

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\LEScripts.h

!IF  "$(CFG)" == "layout - Win32 Release"

# Begin Custom Build
InputPath=.\LEScripts.h

"..\..\include\layout\LEScripts.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    LEScripts.h    ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# Begin Custom Build
InputPath=.\LEScripts.h

"..\..\include\layout\LEScripts.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    LEScripts.h    ..\..\include\layout

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\LESwaps.h

!IF  "$(CFG)" == "layout - Win32 Release"

# Begin Custom Build
InputPath=.\LESwaps.h

"..\..\include\layout\LESwaps.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    LESwaps.h    ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# Begin Custom Build
InputPath=.\LESwaps.h

"..\..\include\layout\LESwaps.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    LESwaps.h    ..\..\include\layout

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\LETypes.h

!IF  "$(CFG)" == "layout - Win32 Release"

# Begin Custom Build
InputPath=.\LETypes.h

"..\..\include\layout\LETypes.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    LETypes.h    ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layout - Win32 Debug"

# Begin Custom Build
InputPath=.\LETypes.h

"..\..\include\layout\LETypes.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    LETypes.h    ..\..\include\layout

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\aat\LigatureSubstitution.h
# End Source File
# Begin Source File

SOURCE=.\aat\LigatureSubstitutionProcessor.h
# End Source File
# Begin Source File

SOURCE=.\opentype\LigatureSubstitutionSubtables.h
# End Source File
# Begin Source File

SOURCE=.\opentype\LookupProcessor.h
# End Source File
# Begin Source File

SOURCE=.\opentype\Lookups.h
# End Source File
# Begin Source File

SOURCE=.\aat\LookupTables.h
# End Source File
# Begin Source File

SOURCE=.\opentype\MarkArrays.h
# End Source File
# Begin Source File

SOURCE=.\opentype\MarkToBasePositioningSubtables.h
# End Source File
# Begin Source File

SOURCE=.\opentype\MarkToLigaturePositioningSubtables.h
# End Source File
# Begin Source File

SOURCE=.\opentype\MarkToMarkPositioningSubtables.h
# End Source File
# Begin Source File

SOURCE=.\aat\MorphStateTables.h
# End Source File
# Begin Source File

SOURCE=.\aat\MorphTables.h
# End Source File
# Begin Source File

SOURCE=.\opentype\MultipleSubstitutionSubtables.h
# End Source File
# Begin Source File

SOURCE=.\aat\NonContextualGlyphSubstitution.h
# End Source File
# Begin Source File

SOURCE=.\aat\NonContextualGlyphSubstitutionProcessor.h
# End Source File
# Begin Source File

SOURCE=.\OpenTypeLayoutEngine.h
# End Source File
# Begin Source File

SOURCE=.\opentype\OpenTypeTables.h
# End Source File
# Begin Source File

SOURCE=.\opentype\OpenTypeUtilities.h
# End Source File
# Begin Source File

SOURCE=.\opentype\PairPositioningSubtables.h
# End Source File
# Begin Source File

SOURCE=.\opentype\ScriptAndLanguage.h
# End Source File
# Begin Source File

SOURCE=.\opentype\ScriptAndLanguageTags.h
# End Source File
# Begin Source File

SOURCE=.\aat\SegmentArrayProcessor.h
# End Source File
# Begin Source File

SOURCE=.\aat\SegmentSingleProcessor.h
# End Source File
# Begin Source File

SOURCE=.\aat\SimpleArrayProcessor.h
# End Source File
# Begin Source File

SOURCE=.\opentype\SinglePositioningSubtables.h
# End Source File
# Begin Source File

SOURCE=.\opentype\SingleSubstitutionSubtables.h
# End Source File
# Begin Source File

SOURCE=.\aat\SingleTableProcessor.h
# End Source File
# Begin Source File

SOURCE=.\aat\StateTableProcessor.h
# End Source File
# Begin Source File

SOURCE=.\aat\StateTables.h
# End Source File
# Begin Source File

SOURCE=.\opentype\SubstitutionLookups.h
# End Source File
# Begin Source File

SOURCE=.\aat\SubtableProcessor.h
# End Source File
# Begin Source File

SOURCE=.\ThaiLayoutEngine.h
# End Source File
# Begin Source File

SOURCE=.\ThaiShaping.h
# End Source File
# Begin Source File

SOURCE=.\aat\TrimmedArrayProcessor.h
# End Source File
# Begin Source File

SOURCE=.\opentype\ValueRecords.h
# End Source File
# End Group
# Begin Group "Resource Files"

# PROP Default_Filter "ico;cur;bmp;dlg;rc2;rct;bin;rgs;gif;jpg;jpeg;jpe"
# End Group
# End Target
# End Project
