# Microsoft Developer Studio Project File - Name="toolutil" - Package Owner=<4>
# Microsoft Developer Studio Generated Build File, Format Version 6.00
# ** DO NOT EDIT **

# TARGTYPE "Win32 (x86) Dynamic-Link Library" 0x0102

CFG=toolutil - Win32 Debug
!MESSAGE This is not a valid makefile. To build this project using NMAKE,
!MESSAGE use the Export Makefile command and run
!MESSAGE 
!MESSAGE NMAKE /f "toolutil.mak".
!MESSAGE 
!MESSAGE You can specify a configuration when running NMAKE
!MESSAGE by defining the macro CFG on the command line. For example:
!MESSAGE 
!MESSAGE NMAKE /f "toolutil.mak" CFG="toolutil - Win32 Debug"
!MESSAGE 
!MESSAGE Possible choices for configuration are:
!MESSAGE 
!MESSAGE "toolutil - Win32 Release" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE "toolutil - Win32 Debug" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE "toolutil - Win64 Release" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE "toolutil - Win64 Debug" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE 

# Begin Project
# PROP AllowPerConfigDependencies 0
# PROP Scc_ProjName ""
# PROP Scc_LocalPath ""
CPP=cl.exe
MTL=midl.exe
RSC=rc.exe

!IF  "$(CFG)" == "toolutil - Win32 Release"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 0
# PROP BASE Output_Dir "Release"
# PROP BASE Intermediate_Dir "Release"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 0
# PROP Output_Dir "..\..\..\lib"
# PROP Intermediate_Dir "Release"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MT /W3 /GX /O2 /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "TOOLUTIL_EXPORTS" /FD /c
# ADD CPP /nologo /G6 /MD /W3 /GX /O2 /I "..\..\common" /D "NDEBUG" /D "WIN32" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /FD /c
# SUBTRACT CPP /YX
# ADD BASE MTL /nologo /D "NDEBUG" /mktyplib203 /win32
# ADD MTL /nologo /D "NDEBUG" /mktyplib203 /win32
# ADD BASE RSC /l 0x409 /d "NDEBUG"
# ADD RSC /l 0x409 /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /machine:I386
# ADD LINK32 kernel32.lib /nologo /dll /machine:I386 /out:"..\..\..\bin\icutu30.dll" /implib:"..\..\..\lib/icutu.lib" /libpath:"..\..\..\lib"
# SUBTRACT LINK32 /pdb:none

!ELSEIF  "$(CFG)" == "toolutil - Win32 Debug"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 1
# PROP BASE Output_Dir "Debug"
# PROP BASE Intermediate_Dir "Debug"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 1
# PROP Output_Dir "..\..\..\lib"
# PROP Intermediate_Dir "Debug"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MTd /W3 /Gm /GX /ZI /Od /D "WIN32" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "TOOLUTIL_EXPORTS" /FD /GZ /c
# ADD CPP /nologo /G6 /MDd /W3 /Gm /GX /ZI /Od /I "..\..\common" /D "_DEBUG" /D "WIN32" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /FD /GZ /c
# SUBTRACT CPP /YX
# ADD BASE MTL /nologo /D "_DEBUG" /mktyplib203 /win32
# ADD MTL /nologo /D "_DEBUG" /mktyplib203 /win32
# ADD BASE RSC /l 0x409 /d "_DEBUG"
# ADD RSC /l 0x409 /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /debug /machine:I386 /pdbtype:sept
# ADD LINK32 kernel32.lib /nologo /dll /debug /machine:I386 /out:"..\..\..\bin\icutu30d.dll" /implib:"..\..\..\lib/icutud.lib" /pdbtype:sept /libpath:"..\..\..\lib"
# SUBTRACT LINK32 /pdb:none

!ELSEIF  "$(CFG)" == "toolutil - Win64 Release"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 0
# PROP BASE Output_Dir "Release"
# PROP BASE Intermediate_Dir "Release"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 0
# PROP Output_Dir "..\..\..\lib"
# PROP Intermediate_Dir "Release"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MT /W3 /GX /O2 /D "WIN64" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "TOOLUTIL_EXPORTS" /FD /c
# ADD CPP /nologo /MD /W3 /GX /Zi /O2 /Op /I "..\..\common" /D "NDEBUG" /D "WIN64" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "_IA64_" /D "WIN32" /D "_AFX_NO_DAO_SUPPORT" /FD /QIA64_fmaopt /Zm600 /c
# SUBTRACT CPP /YX
# ADD BASE MTL /nologo /D "NDEBUG" /mktyplib203 /win64
# ADD MTL /nologo /D "NDEBUG" /mktyplib203 /win64
# ADD BASE RSC /l 0x409 /d "NDEBUG"
# ADD RSC /l 0x409 /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /machine:IX86 /machine:IA64
# ADD LINK32 kernel32.lib /nologo /dll /machine:IX86 /out:"..\..\..\bin\icutu30.dll" /implib:"..\..\..\lib/icutu.lib" /libpath:"..\..\..\lib" /machine:IA64

!ELSEIF  "$(CFG)" == "toolutil - Win64 Debug"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 1
# PROP BASE Output_Dir "Debug"
# PROP BASE Intermediate_Dir "Debug"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 1
# PROP Output_Dir "..\..\..\lib"
# PROP Intermediate_Dir "Debug"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MTd /W3 /Gm /GX /ZI /Od /D "WIN64" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "TOOLUTIL_EXPORTS" /FD /GZ /c
# ADD CPP /nologo /MDd /W3 /Gm /GX /Zi /Od /Op /I "..\..\common" /D "_DEBUG" /D "WIN64" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "_IA64_" /D "WIN32" /D "_AFX_NO_DAO_SUPPORT" /FD /GZ /QIA64_fmaopt /Zm600 /c
# SUBTRACT CPP /YX
# ADD BASE MTL /nologo /D "_DEBUG" /mktyplib203 /win64
# ADD MTL /nologo /D "_DEBUG" /mktyplib203 /win64
# ADD BASE RSC /l 0x409 /d "_DEBUG"
# ADD RSC /l 0x409 /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /debug /machine:IX86 /pdbtype:sept /machine:IA64
# ADD LINK32 kernel32.lib /nologo /dll /incremental:no /debug /machine:IX86 /out:"..\..\..\bin\icutu30d.dll" /implib:"..\..\..\lib/icutud.lib" /pdbtype:sept /libpath:"..\..\..\lib" /machine:IA64

!ENDIF 

# Begin Target

# Name "toolutil - Win32 Release"
# Name "toolutil - Win32 Debug"
# Name "toolutil - Win64 Release"
# Name "toolutil - Win64 Debug"
# Begin Group "Source Files"

# PROP Default_Filter "cpp;c;cxx;rc;def;r;odl;idl;hpj;bat"
# Begin Source File

SOURCE=.\toolutil.c
# ADD CPP /Ze
# End Source File
# Begin Source File

SOURCE=.\ucbuf.c
# End Source File
# Begin Source File

SOURCE=.\ucm.c
# End Source File
# Begin Source File

SOURCE=.\ucmpwrit.c
# End Source File
# Begin Source File

SOURCE=.\ucmstate.c
# End Source File
# Begin Source File

SOURCE=.\unewdata.c
# End Source File
# Begin Source File

SOURCE=.\uoptions.c
# End Source File
# Begin Source File

SOURCE=.\uparse.c
# End Source File
# Begin Source File

SOURCE=.\uperf.cpp
# End Source File
# End Group
# Begin Group "Header Files"

# PROP Default_Filter "h;hpp;hxx;hm;inl"
# Begin Source File

SOURCE=.\toolutil.h
# End Source File
# Begin Source File

SOURCE=.\ucbuf.h
# End Source File
# Begin Source File

SOURCE=.\ucm.h
# End Source File
# Begin Source File

SOURCE=.\ucmpwrit.h
# End Source File
# Begin Source File

SOURCE=.\unewdata.h
# End Source File
# Begin Source File

SOURCE=.\uoptions.h
# End Source File
# Begin Source File

SOURCE=.\uparse.h
# End Source File
# Begin Source File

SOURCE=.\uperf.h
# End Source File
# Begin Source File

SOURCE=.\utimer.h
# End Source File
# End Group
# Begin Group "Resource Files"

# PROP Default_Filter "ico;cur;bmp;dlg;rc2;rct;bin;rgs;gif;jpg;jpeg;jpe"
# End Group
# End Target
# End Project
