# Microsoft Developer Studio Project File - Name="layoutex" - Package Owner=<4>
# Microsoft Developer Studio Generated Build File, Format Version 6.00
# ** DO NOT EDIT **

# TARGTYPE "Win32 (x86) Dynamic-Link Library" 0x0102

CFG=layoutex - Win32 Debug
!MESSAGE This is not a valid makefile. To build this project using NMAKE,
!MESSAGE use the Export Makefile command and run
!MESSAGE 
!MESSAGE NMAKE /f "layoutex.mak".
!MESSAGE 
!MESSAGE You can specify a configuration when running NMAKE
!MESSAGE by defining the macro CFG on the command line. For example:
!MESSAGE 
!MESSAGE NMAKE /f "layoutex.mak" CFG="layoutex - Win32 Debug"
!MESSAGE 
!MESSAGE Possible choices for configuration are:
!MESSAGE 
!MESSAGE "layoutex - Win32 Release" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE "layoutex - Win32 Debug" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE "layoutex - Win64 Release" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE "layoutex - Win64 Debug" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE 

# Begin Project
# PROP AllowPerConfigDependencies 0
# PROP Scc_ProjName ""
# PROP Scc_LocalPath ""
CPP=cl.exe
MTL=midl.exe
RSC=rc.exe

!IF  "$(CFG)" == "layoutex - Win32 Release"

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
# ADD BASE CPP /nologo /MT /W3 /GX /O2 /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "LAYOUTEX_EXPORTS" /FD /c
# ADD CPP /nologo /MD /W3 /GX /O2 /I "..\..\include" /I "..\common" /D "NDEBUG" /D "WIN32" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "U_LAYOUTEX_IMPLEMENTATION" /FD /c
# ADD BASE MTL /nologo /D "NDEBUG" /mktyplib203 /win32
# ADD MTL /nologo /D "NDEBUG" /mktyplib203 /win32
# ADD BASE RSC /l 0x409 /d "NDEBUG"
# ADD RSC /l 0x409 /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /machine:I386
# ADD LINK32 ..\..\lib\icuuc.lib ..\..\lib\icule.lib /nologo /dll /machine:I386 /out:"..\..\bin\iculx30.dll" /implib:"..\..\lib\iculx.lib"
# SUBTRACT LINK32 /pdb:none

!ELSEIF  "$(CFG)" == "layoutex - Win32 Debug"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 1
# PROP BASE Output_Dir "Debug"
# PROP BASE Intermediate_Dir "Debug"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 1
# PROP Output_Dir "..\..\lib\"
# PROP Intermediate_Dir "Debug"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MTd /W3 /Gm /GX /ZI /Od /D "WIN32" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "LAYOUTEX_EXPORTS" /FD /GZ /c
# ADD CPP /nologo /MDd /W3 /Gm /GX /ZI /Od /I "..\..\include" /I "..\common" /D "_DEBUG" /D "WIN32" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "U_LAYOUTEX_IMPLEMENTATION" /FR /FD /GZ /c
# ADD BASE MTL /nologo /D "_DEBUG" /mktyplib203 /win32
# ADD MTL /nologo /D "_DEBUG" /mktyplib203 /win32
# ADD BASE RSC /l 0x409 /d "_DEBUG"
# ADD RSC /l 0x409 /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /debug /machine:I386 /pdbtype:sept
# ADD LINK32 ..\..\lib\icuucd.lib ..\..\lib\iculed.lib /nologo /dll /debug /machine:I386 /out:"..\..\bin\iculx30d.dll" /implib:"..\..\lib\iculxd.lib" /pdbtype:sept
# SUBTRACT LINK32 /pdb:none

!ELSEIF  "$(CFG)" == "layoutex - Win64 Release"

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
# ADD BASE CPP /nologo /MT /W3 /GX /O2 /D "WIN64" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "LAYOUTEX_EXPORTS" /FD /c
# ADD CPP /nologo /MT /W3 /GX /Zi /O2 /Op /I "..\..\include" /I "..\common" /D "NDEBUG" /D "WIN64" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "U_LAYOUTEX_IMPLEMENTATION" /D "_IA64_" /D "WIN32" /D "_AFX_NO_DAO_SUPPORT" /FD /QIA64_fmaopt /Zm600 /c
# ADD BASE MTL /nologo /D "NDEBUG" /mktyplib203 /win64
# ADD MTL /nologo /D "NDEBUG" /mktyplib203 /win64
# ADD BASE RSC /l 0x409 /d "NDEBUG"
# ADD RSC /l 0x409 /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /machine:IX86 /machine:IA64
# ADD LINK32 ..\..\lib\icuuc.lib ..\..\lib\icule.lib /nologo /dll /machine:IX86 /out:"..\..\bin\iculx30.dll" /implib:"..\..\lib\iculx.lib" /machine:IA64

!ELSEIF  "$(CFG)" == "layoutex - Win64 Debug"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 1
# PROP BASE Output_Dir "Debug"
# PROP BASE Intermediate_Dir "Debug"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 1
# PROP Output_Dir "..\..\lib\"
# PROP Intermediate_Dir "Debug"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MTd /W3 /Gm /GX /ZI /Od /D "WIN64" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "LAYOUTEX_EXPORTS" /FD /GZ /c
# ADD CPP /nologo /MTd /W3 /Gm /GX /Zi /Od /Op /I "..\..\include" /I "..\common" /D "_DEBUG" /D "WIN64" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "U_LAYOUTEX_IMPLEMENTATION" /D "_IA64_" /D "WIN32" /D "_AFX_NO_DAO_SUPPORT" /FR /FD /GZ /QIA64_fmaopt /Zm600 /c
# ADD BASE MTL /nologo /D "_DEBUG" /mktyplib203 /win64
# ADD MTL /nologo /D "_DEBUG" /mktyplib203 /win64
# ADD BASE RSC /l 0x409 /d "_DEBUG"
# ADD RSC /l 0x409 /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /debug /machine:IX86 /pdbtype:sept /machine:IA64
# ADD LINK32 ..\..\lib\icuucd.lib ..\..\lib\iculed.lib /nologo /dll /incremental:no /debug /machine:IX86 /out:"..\..\bin\iculx30d.dll" /implib:"..\..\lib\iculxd.lib" /pdbtype:sept /machine:IA64

!ENDIF 

# Begin Target

# Name "layoutex - Win32 Release"
# Name "layoutex - Win32 Debug"
# Name "layoutex - Win64 Release"
# Name "layoutex - Win64 Debug"
# Begin Group "Source Files"

# PROP Default_Filter "cpp;c;cxx;rc;def;r;odl;idl;hpj;bat"
# Begin Source File

SOURCE=.\LXUtilities.cpp
# End Source File
# Begin Source File

SOURCE=.\ParagraphLayout.cpp
# End Source File
# Begin Source File

SOURCE=.\RunArrays.cpp
# End Source File
# End Group
# Begin Group "Header Files"

# PROP Default_Filter "h;hpp;hxx;hm;inl"
# Begin Source File

SOURCE=.\LXUtilities.h
# End Source File
# Begin Source File

SOURCE=.\layout\ParagraphLayout.h

!IF  "$(CFG)" == "layoutex - Win32 Release"

# Begin Custom Build
InputPath=.\layout\ParagraphLayout.h

"..\..\include\layout\ParagraphLayout.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layoutex - Win32 Debug"

# Begin Custom Build
InputPath=.\layout\ParagraphLayout.h

"..\..\include\layout\ParagraphLayout.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layoutex - Win64 Release"

# Begin Custom Build
InputPath=.\layout\ParagraphLayout.h

"..\..\include\layout\ParagraphLayout.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layoutex - Win64 Debug"

# Begin Custom Build
InputPath=.\layout\ParagraphLayout.h

"..\..\include\layout\ParagraphLayout.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\layout\RunArrays.h

!IF  "$(CFG)" == "layoutex - Win32 Release"

# Begin Custom Build
InputPath=.\layout\RunArrays.h

"..\..\include\layout\RunArrays.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layoutex - Win32 Debug"

# Begin Custom Build
InputPath=.\layout\RunArrays.h

"..\..\include\layout\RunArrays.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layoutex - Win64 Release"

# Begin Custom Build
InputPath=.\layout\RunArrays.h

"..\..\include\layout\RunArrays.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ELSEIF  "$(CFG)" == "layoutex - Win64 Debug"

# Begin Custom Build
InputPath=.\layout\RunArrays.h

"..\..\include\layout\RunArrays.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy  $(InputPath)  ..\..\include\layout

# End Custom Build

!ENDIF 

# End Source File
# End Group
# Begin Group "Resource Files"

# PROP Default_Filter "ico;cur;bmp;dlg;rc2;rct;bin;rgs;gif;jpg;jpeg;jpe"
# Begin Source File

SOURCE=.\layoutex.rc
# ADD BASE RSC /l 0x409
# ADD RSC /l 0x409 /i "..\common"
# End Source File
# End Group
# End Target
# End Project
