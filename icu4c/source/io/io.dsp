# Microsoft Developer Studio Project File - Name="io" - Package Owner=<4>
# Microsoft Developer Studio Generated Build File, Format Version 6.00
# ** DO NOT EDIT **

# TARGTYPE "Win32 (x86) Dynamic-Link Library" 0x0102

CFG=io - Win32 Debug
!MESSAGE This is not a valid makefile. To build this project using NMAKE,
!MESSAGE use the Export Makefile command and run
!MESSAGE 
!MESSAGE NMAKE /f "io.mak".
!MESSAGE 
!MESSAGE You can specify a configuration when running NMAKE
!MESSAGE by defining the macro CFG on the command line. For example:
!MESSAGE 
!MESSAGE NMAKE /f "io.mak" CFG="io - Win32 Debug"
!MESSAGE 
!MESSAGE Possible choices for configuration are:
!MESSAGE 
!MESSAGE "io - Win32 Release" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE "io - Win32 Debug" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE "io - Win64 Release" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE "io - Win64 Debug" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE 

# Begin Project
# PROP AllowPerConfigDependencies 0
# PROP Scc_ProjName ""
# PROP Scc_LocalPath ""
CPP=cl.exe
MTL=midl.exe
RSC=rc.exe

!IF  "$(CFG)" == "io - Win32 Release"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 0
# PROP BASE Output_Dir "Release"
# PROP BASE Intermediate_Dir "Release"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 0
# PROP Output_Dir "..\..\lib"
# PROP Intermediate_Dir "Release"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MT /W3 /GX /O2 /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "IO_EXPORTS" /FD /c
# ADD CPP /nologo /G6 /MD /Za /W3 /GX /O2 /Ob2 /I "..\..\include" /I "..\common" /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "IO_EXPORTS" /D "U_IO_IMPLEMENTATION" /FD /GF /c
# ADD BASE MTL /nologo /D "NDEBUG" /mktyplib203 /win32
# ADD MTL /nologo /D "NDEBUG" /mktyplib203 /win32
# ADD BASE RSC /l 0x409 /d "NDEBUG"
# ADD RSC /l 0x409 /i "..\common" /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /machine:I386
# ADD LINK32 icuuc.lib icuin.lib /nologo /dll /machine:I386 /out:"..\..\bin\icuio30.dll" /implib:"..\..\lib/icuio.lib" /libpath:"..\..\lib\\"
# SUBTRACT LINK32 /pdb:none

!ELSEIF  "$(CFG)" == "io - Win32 Debug"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 1
# PROP BASE Output_Dir "Debug"
# PROP BASE Intermediate_Dir "Debug"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 1
# PROP Output_Dir "..\..\lib"
# PROP Intermediate_Dir "Debug"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MTd /W3 /Gm /GX /ZI /Od /D "WIN32" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "IO_EXPORTS" /FD /GZ /c
# ADD CPP /nologo /G6 /MDd /Za /W3 /Gm /GX /ZI /Od /I "..\..\include" /I "..\common" /D "WIN32" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "IO_EXPORTS" /D "U_IO_IMPLEMENTATION" /FR /FD /GF /GZ /c
# ADD BASE MTL /nologo /D "_DEBUG" /mktyplib203 /win32
# ADD MTL /nologo /D "_DEBUG" /mktyplib203 /win32
# ADD BASE RSC /l 0x409 /d "_DEBUG"
# ADD RSC /l 0x409 /i "..\common" /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /debug /machine:I386 /pdbtype:sept
# ADD LINK32 icuucd.lib icuind.lib /nologo /dll /debug /machine:I386 /out:"..\..\bin\icuio30d.dll" /implib:"..\..\lib\icuiod.lib" /pdbtype:sept /libpath:"debug" /libpath:"..\..\lib\\"
# SUBTRACT LINK32 /pdb:none

!ELSEIF  "$(CFG)" == "io - Win64 Release"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 0
# PROP BASE Output_Dir "Release"
# PROP BASE Intermediate_Dir "Release"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 0
# PROP Output_Dir "..\..\lib"
# PROP Intermediate_Dir "Release"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MT /W3 /GX /O2 /D "WIN64" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "IO_EXPORTS" /FD /c
# ADD CPP /nologo /MD /Za /W3 /GX /Zi /O2 /I "..\..\include" /I "..\common" /D "WIN64" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "IO_EXPORTS" /D "U_IO_IMPLEMENTATION" /D "_IA64_" /D "WIN32" /D "_AFX_NO_DAO_SUPPORT" /FD /GF /QIA64_fmaopt /Zm600 /c
# ADD BASE MTL /nologo /D "NDEBUG" /mktyplib203 /win64
# ADD MTL /nologo /D "NDEBUG" /mktyplib203 /win64
# ADD BASE RSC /l 0x409 /d "NDEBUG"
# ADD RSC /l 0x409 /i "..\common" /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /machine:IX86 /machine:IA64
# ADD LINK32 icuuc.lib icuin.lib /nologo /dll /machine:IX86 /out:"..\..\bin\icuio30.dll" /implib:"..\..\lib/icuio.lib" /libpath:"..\..\lib\\" /machine:IA64

!ELSEIF  "$(CFG)" == "io - Win64 Debug"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 1
# PROP BASE Output_Dir "Debug"
# PROP BASE Intermediate_Dir "Debug"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 1
# PROP Output_Dir "..\..\lib"
# PROP Intermediate_Dir "Debug"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MTd /W3 /Gm /GX /ZI /Od /D "WIN64" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "IO_EXPORTS" /FD /GZ /c
# ADD CPP /nologo /MDd /Za /W3 /Gm /GX /Zi /Od /I "..\..\include" /I "..\common" /D "WIN64" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "IO_EXPORTS" /D "U_IO_IMPLEMENTATION" /D "_IA64_" /D "WIN32" /D "_AFX_NO_DAO_SUPPORT" /FR /FD /GF /GZ /QIA64_fmaopt /Zm600 /c
# ADD BASE MTL /nologo /D "_DEBUG" /mktyplib203 /win64
# ADD MTL /nologo /D "_DEBUG" /mktyplib203 /win64
# ADD BASE RSC /l 0x409 /d "_DEBUG"
# ADD RSC /l 0x409 /i "..\common" /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /debug /machine:IX86 /pdbtype:sept /machine:IA64
# ADD LINK32 icuucd.lib icuind.lib /nologo /dll /incremental:no /debug /machine:IX86 /out:"..\..\bin\icuio30d.dll" /implib:"..\..\lib\icuiod.lib" /pdbtype:sept /libpath:"debug" /libpath:"..\..\lib\\" /machine:IA64

!ENDIF 

# Begin Target

# Name "io - Win32 Release"
# Name "io - Win32 Debug"
# Name "io - Win64 Release"
# Name "io - Win64 Debug"
# Begin Group "Source Files"

# PROP Default_Filter "cpp;c;cxx;rc;def;r;odl;idl;hpj;bat"
# Begin Source File

SOURCE=.\locbund.c
# End Source File
# Begin Source File

SOURCE=.\sprintf.c
# End Source File
# Begin Source File

SOURCE=.\sscanf.c
# End Source File
# Begin Source File

SOURCE=.\ufile.c
# End Source File
# Begin Source File

SOURCE=.\ufmt_cmn.c
# End Source File
# Begin Source File

SOURCE=.\uprintf.c
# End Source File
# Begin Source File

SOURCE=.\uprntf_p.c
# End Source File
# Begin Source File

SOURCE=.\uscanf.c
# End Source File
# Begin Source File

SOURCE=.\uscanf_p.c
# End Source File
# Begin Source File

SOURCE=.\ustdio.c
# End Source File
# Begin Source File

SOURCE=.\ustream.cpp
# ADD CPP /Ze
# End Source File
# End Group
# Begin Group "Header Files"

# PROP Default_Filter "h;hpp;hxx;hm;inl"
# Begin Source File

SOURCE=.\locbund.h
# End Source File
# Begin Source File

SOURCE=.\sprintf.h
# End Source File
# Begin Source File

SOURCE=.\ufile.h
# End Source File
# Begin Source File

SOURCE=.\ufmt_cmn.h
# End Source File
# Begin Source File

SOURCE=.\uprintf.h
# End Source File
# Begin Source File

SOURCE=.\uprntf_p.h
# End Source File
# Begin Source File

SOURCE=.\uscanf.h
# End Source File
# Begin Source File

SOURCE=.\uscanf_p.h
# End Source File
# Begin Source File

SOURCE=.\unicode\ustdio.h

!IF  "$(CFG)" == "io - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\ustdio.h

"..\..\include\unicode\ustdio.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "io - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\ustdio.h

"..\..\include\unicode\ustdio.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "io - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\ustdio.h

"..\..\include\unicode\ustdio.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "io - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\ustdio.h

"..\..\include\unicode\ustdio.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\unicode\ustream.h

!IF  "$(CFG)" == "io - Win32 Release"

# Begin Custom Build
InputPath=.\unicode\ustream.h

"..\..\include\unicode\ustream.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "io - Win32 Debug"

# Begin Custom Build
InputPath=.\unicode\ustream.h

"..\..\include\unicode\ustream.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "io - Win64 Release"

# Begin Custom Build
InputPath=.\unicode\ustream.h

"..\..\include\unicode\ustream.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ELSEIF  "$(CFG)" == "io - Win64 Debug"

# Begin Custom Build
InputPath=.\unicode\ustream.h

"..\..\include\unicode\ustream.h" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy    $(InputPath)    ..\..\include\unicode

# End Custom Build

!ENDIF 

# End Source File
# End Group
# Begin Group "Resource Files"

# PROP Default_Filter "ico;cur;bmp;dlg;rc2;rct;bin;rgs;gif;jpg;jpeg;jpe"
# Begin Source File

SOURCE=.\io.rc
# End Source File
# End Group
# End Target
# End Project
