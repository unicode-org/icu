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

SOURCE=.\AlternateSubstitutionSubtables.cpp
# End Source File
# Begin Source File

SOURCE=.\AnchorTables.cpp
# End Source File
# Begin Source File

SOURCE=.\ArabicLayoutEngine.cpp
# End Source File
# Begin Source File

SOURCE=.\ArabicLigatureData.cpp
# End Source File
# Begin Source File

SOURCE=.\ArabicShaping.cpp
# End Source File
# Begin Source File

SOURCE=.\ClassDefinitionTables.cpp
# End Source File
# Begin Source File

SOURCE=.\ContextualGlyphSubstitutionProcessor.cpp
# End Source File
# Begin Source File

SOURCE=.\ContextualSubstitutionSubtables.cpp
# End Source File
# Begin Source File

SOURCE=.\CoverageTables.cpp
# End Source File
# Begin Source File

SOURCE=.\DeviceTables.cpp
# End Source File
# Begin Source File

SOURCE=.\Features.cpp
# End Source File
# Begin Source File

SOURCE=.\GDEFMarkFilter.cpp
# End Source File
# Begin Source File

SOURCE=.\GlyphDefinitionTables.cpp
# End Source File
# Begin Source File

SOURCE=.\GlyphIterator.cpp
# End Source File
# Begin Source File

SOURCE=.\GlyphPositioningLookupProcessor.cpp
# End Source File
# Begin Source File

SOURCE=.\GlyphPositioningTables.cpp
# End Source File
# Begin Source File

SOURCE=.\GlyphSubstitutionLookupProcessor.cpp
# End Source File
# Begin Source File

SOURCE=.\GlyphSubstitutionTables.cpp
# End Source File
# Begin Source File

SOURCE=.\GXLayoutEngine.cpp
# End Source File
# Begin Source File

SOURCE=.\HebrewLigatureData.cpp
# End Source File
# Begin Source File

SOURCE=.\HebrewShaping.cpp
# End Source File
# Begin Source File

SOURCE=.\IndicClassTables.cpp
# End Source File
# Begin Source File

SOURCE=.\IndicLayoutEngine.cpp
# End Source File
# Begin Source File

SOURCE=.\IndicRearrangementProcessor.cpp
# End Source File
# Begin Source File

SOURCE=.\IndicReordering.cpp
# End Source File
# Begin Source File

SOURCE=.\LayoutEngine.cpp
# End Source File
# Begin Source File

SOURCE=.\LigatureSubstitutionProcessor.cpp
# End Source File
# Begin Source File

SOURCE=.\LigatureSubstitutionSubtables.cpp
# End Source File
# Begin Source File

SOURCE=.\LookupProcessor.cpp
# End Source File
# Begin Source File

SOURCE=.\Lookups.cpp
# End Source File
# Begin Source File

SOURCE=.\LookupTables.cpp
# End Source File
# Begin Source File

SOURCE=.\MarkArrays.cpp
# End Source File
# Begin Source File

SOURCE=.\MarkToBasePositioningSubtables.cpp
# End Source File
# Begin Source File

SOURCE=.\MarkToLigaturePositioningSubtables.cpp
# End Source File
# Begin Source File

SOURCE=.\MarkToMarkPositioningSubtables.cpp
# End Source File
# Begin Source File

SOURCE=.\MorphTables.cpp
# End Source File
# Begin Source File

SOURCE=.\MultipleSubstitutionSubtables.cpp
# End Source File
# Begin Source File

SOURCE=.\NonContextualGlyphSubstitutionProcessor.cpp
# End Source File
# Begin Source File

SOURCE=.\OpenTypeLayoutEngine.cpp
# End Source File
# Begin Source File

SOURCE=.\OpenTypeUtilities.cpp
# End Source File
# Begin Source File

SOURCE=.\PairPositioningSubtables.cpp
# End Source File
# Begin Source File

SOURCE=.\ScriptAndLanguage.cpp
# End Source File
# Begin Source File

SOURCE=.\SegmentArrayProcessor.cpp
# End Source File
# Begin Source File

SOURCE=.\SegmentSingleProcessor.cpp
# End Source File
# Begin Source File

SOURCE=.\SimpleArrayProcessor.cpp
# End Source File
# Begin Source File

SOURCE=.\SinglePositioningSubtables.cpp
# End Source File
# Begin Source File

SOURCE=.\SingleSubstitutionSubtables.cpp
# End Source File
# Begin Source File

SOURCE=.\SingleTableProcessor.cpp
# End Source File
# Begin Source File

SOURCE=.\StateTableProcessor.cpp
# End Source File
# Begin Source File

SOURCE=.\SubstitutionLookups.cpp
# End Source File
# Begin Source File

SOURCE=.\SubtableProcessor.cpp
# End Source File
# Begin Source File

SOURCE=.\ThaiLayoutEngine.cpp
# End Source File
# Begin Source File

SOURCE=.\ThaiShaping.cpp
# End Source File
# Begin Source File

SOURCE=.\ThaiStateTables.cpp
# End Source File
# Begin Source File

SOURCE=.\TrimmedArrayProcessor.cpp
# End Source File
# Begin Source File

SOURCE=.\ValueRecords.cpp
# End Source File
# End Group
# Begin Group "Header Files"

# PROP Default_Filter "h;hpp;hxx;hm;inl"
# Begin Source File

SOURCE=.\AlternateSubstitutionSubtables.h
# End Source File
# Begin Source File

SOURCE=.\AnchorTables.h
# End Source File
# Begin Source File

SOURCE=.\ArabicLayoutEngine.h
# End Source File
# Begin Source File

SOURCE=.\ArabicShaping.h
# End Source File
# Begin Source File

SOURCE=.\AttachmentPositioningSubtables.h
# End Source File
# Begin Source File

SOURCE=.\ClassDefinitionTables.h
# End Source File
# Begin Source File

SOURCE=.\ContextualGlyphInsertion.h
# End Source File
# Begin Source File

SOURCE=.\ContextualGlyphSubstitution.h
# End Source File
# Begin Source File

SOURCE=.\ContextualGlyphSubstitutionProcessor.h
# End Source File
# Begin Source File

SOURCE=.\ContextualSubstitutionSubtables.h
# End Source File
# Begin Source File

SOURCE=.\CoverageTables.h
# End Source File
# Begin Source File

SOURCE=.\DeviceTables.h
# End Source File
# Begin Source File

SOURCE=.\Features.h
# End Source File
# Begin Source File

SOURCE=.\GDEFMarkFilter.h
# End Source File
# Begin Source File

SOURCE=.\GlyphDefinitionTables.h
# End Source File
# Begin Source File

SOURCE=.\GlyphIterator.h
# End Source File
# Begin Source File

SOURCE=.\GlyphPositionAdjustments.h
# End Source File
# Begin Source File

SOURCE=.\GlyphPositioningLookupProcessor.h
# End Source File
# Begin Source File

SOURCE=.\GlyphPositioningTables.h
# End Source File
# Begin Source File

SOURCE=.\GlyphSubstitutionLookupProcessor.h
# End Source File
# Begin Source File

SOURCE=.\GlyphSubstitutionTables.h
# End Source File
# Begin Source File

SOURCE=.\GXLayoutEngine.h
# End Source File
# Begin Source File

SOURCE=.\HebrewShaping.h
# End Source File
# Begin Source File

SOURCE=.\HindiFeatureTags.h
# End Source File
# Begin Source File

SOURCE=.\IndicLayoutEngine.h
# End Source File
# Begin Source File

SOURCE=.\IndicRearrangement.h
# End Source File
# Begin Source File

SOURCE=.\IndicRearrangementProcessor.h
# End Source File
# Begin Source File

SOURCE=.\IndicReordering.h
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

SOURCE=.\LayoutTables.h
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

SOURCE=.\LigatureSubstitution.h
# End Source File
# Begin Source File

SOURCE=.\LigatureSubstitutionProcessor.h
# End Source File
# Begin Source File

SOURCE=.\LigatureSubstitutionSubtables.h
# End Source File
# Begin Source File

SOURCE=.\LookupProcessor.h
# End Source File
# Begin Source File

SOURCE=.\Lookups.h
# End Source File
# Begin Source File

SOURCE=.\LookupTables.h
# End Source File
# Begin Source File

SOURCE=.\MarkArrays.h
# End Source File
# Begin Source File

SOURCE=.\MarkToBasePositioningSubtables.h
# End Source File
# Begin Source File

SOURCE=.\MarkToLigaturePositioningSubtables.h
# End Source File
# Begin Source File

SOURCE=.\MarkToMarkPositioningSubtables.h
# End Source File
# Begin Source File

SOURCE=.\MorphStateTables.h
# End Source File
# Begin Source File

SOURCE=.\MorphTables.h
# End Source File
# Begin Source File

SOURCE=.\MultipleSubstitutionSubtables.h
# End Source File
# Begin Source File

SOURCE=.\NonContextualGlyphSubstitution.h
# End Source File
# Begin Source File

SOURCE=.\NonContextualGlyphSubstitutionProcessor.h
# End Source File
# Begin Source File

SOURCE=.\OpenTypeLayoutEngine.h
# End Source File
# Begin Source File

SOURCE=.\OpenTypeTables.h
# End Source File
# Begin Source File

SOURCE=.\OpenTypeUtilities.h
# End Source File
# Begin Source File

SOURCE=.\PairPositioningSubtables.h
# End Source File
# Begin Source File

SOURCE=.\ScriptAndLanguage.h
# End Source File
# Begin Source File

SOURCE=.\ScriptAndLanguageTags.h
# End Source File
# Begin Source File

SOURCE=.\SegmentArrayProcessor.h
# End Source File
# Begin Source File

SOURCE=.\SegmentSingleProcessor.h
# End Source File
# Begin Source File

SOURCE=.\SimpleArrayProcessor.h
# End Source File
# Begin Source File

SOURCE=.\SinglePositioningSubtables.h
# End Source File
# Begin Source File

SOURCE=.\SingleSubstitutionSubtables.h
# End Source File
# Begin Source File

SOURCE=.\SingleTableProcessor.h
# End Source File
# Begin Source File

SOURCE=.\StateTableProcessor.h
# End Source File
# Begin Source File

SOURCE=.\StateTables.h
# End Source File
# Begin Source File

SOURCE=.\SubstitutionLookups.h
# End Source File
# Begin Source File

SOURCE=.\SubtableProcessor.h
# End Source File
# Begin Source File

SOURCE=.\ThaiLayoutEngine.h
# End Source File
# Begin Source File

SOURCE=.\ThaiShaping.h
# End Source File
# Begin Source File

SOURCE=.\TrimmedArrayProcessor.h
# End Source File
# Begin Source File

SOURCE=.\ValueRecords.h
# End Source File
# End Group
# Begin Group "Resource Files"

# PROP Default_Filter "ico;cur;bmp;dlg;rc2;rct;bin;rgs;gif;jpg;jpeg;jpe"
# End Group
# End Target
# End Project
