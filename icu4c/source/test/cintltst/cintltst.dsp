# Microsoft Developer Studio Project File - Name="cintltst" - Package Owner=<4>
# Microsoft Developer Studio Generated Build File, Format Version 6.00
# ** DO NOT EDIT **

# TARGTYPE "Win32 (x86) Console Application" 0x0103

CFG=cintltst - Win32 Debug
!MESSAGE This is not a valid makefile. To build this project using NMAKE,
!MESSAGE use the Export Makefile command and run
!MESSAGE 
!MESSAGE NMAKE /f "cintltst.mak".
!MESSAGE 
!MESSAGE You can specify a configuration when running NMAKE
!MESSAGE by defining the macro CFG on the command line. For example:
!MESSAGE 
!MESSAGE NMAKE /f "cintltst.mak" CFG="cintltst - Win32 Debug"
!MESSAGE 
!MESSAGE Possible choices for configuration are:
!MESSAGE 
!MESSAGE "cintltst - Win32 Release" (based on "Win32 (x86) Console Application")
!MESSAGE "cintltst - Win32 Debug" (based on "Win32 (x86) Console Application")
!MESSAGE 

# Begin Project
# PROP AllowPerConfigDependencies 0
# PROP Scc_ProjName ""
# PROP Scc_LocalPath ""
CPP=cl.exe
RSC=rc.exe

!IF  "$(CFG)" == "cintltst - Win32 Release"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 0
# PROP BASE Output_Dir "Release"
# PROP BASE Intermediate_Dir "Release"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 0
# PROP Output_Dir "Release"
# PROP Intermediate_Dir "Release"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /W3 /GX /O2 /D "WIN32" /D "NDEBUG" /D "_CONSOLE" /D "_MBCS" /YX /FD /c
# ADD CPP /nologo /MD /W3 /GX /I "..\..\..\include" /I "..\..\tools\ctestfw" /I "..\..\common" /D "WIN32" /D "NDEBUG" /D "_CONSOLE" /D "_MBCS" /YX /FD /c
# ADD BASE RSC /l 0x409 /d "NDEBUG"
# ADD RSC /l 0x409 /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:console /machine:I386
# ADD LINK32 ..\..\..\lib\release\icuuc.lib ..\..\..\lib\release\icui18n.lib ..\..\..\lib\release\ctestfw.lib kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:console /machine:I386

!ELSEIF  "$(CFG)" == "cintltst - Win32 Debug"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 1
# PROP BASE Output_Dir "Debug"
# PROP BASE Intermediate_Dir "Debug"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 1
# PROP Output_Dir "Debug"
# PROP Intermediate_Dir "Debug"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /W3 /Gm /GX /ZI /Od /D "WIN32" /D "_DEBUG" /D "_CONSOLE" /D "_MBCS" /YX /FD /GZ /c
# ADD CPP /nologo /MDd /W3 /Gm /GX /ZI /Od /I "..\..\..\include" /I "..\..\tools\ctestfw" /I "..\..\common" /D "WIN32" /D "_DEBUG" /D "_CONSOLE" /D "_MBCS" /YX /FD /GZ /c
# ADD BASE RSC /l 0x409 /d "_DEBUG"
# ADD RSC /l 0x409 /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:console /debug /machine:I386 /pdbtype:sept
# ADD LINK32 ..\..\..\lib\debug\icuuc.lib ..\..\..\lib\debug\icui18n.lib ..\..\..\lib\debug\ctestfw.lib kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:console /debug /machine:I386 /pdbtype:sept
# SUBTRACT LINK32 /profile

!ENDIF 

# Begin Target

# Name "cintltst - Win32 Release"
# Name "cintltst - Win32 Debug"
# Begin Group "Source Files"

# PROP Default_Filter "cpp;c;cxx;rc;def;r;odl;idl;hpj;bat"
# Begin Source File

SOURCE=.\callcoll.c
# End Source File
# Begin Source File

SOURCE=.\calltest.c
# End Source File
# Begin Source File

SOURCE=.\capitst.c
# End Source File
# Begin Source File

SOURCE=.\cbiapts.c
# End Source File
# Begin Source File

SOURCE=.\cbididat.c
# End Source File
# Begin Source File

SOURCE=.\cbiditst.c
# End Source File
# Begin Source File

SOURCE=.\cbkittst.c
# End Source File
# Begin Source File

SOURCE=.\ccaltst.c
# End Source File
# Begin Source File

SOURCE=.\ccapitst.c
# End Source File
# Begin Source File

SOURCE=.\ccolltst.c
# End Source File
# Begin Source File

SOURCE=.\cconvtst.c
# End Source File
# Begin Source File

SOURCE=.\ccurrtst.c
# End Source File
# Begin Source File

SOURCE=.\cdantst.c
# End Source File
# Begin Source File

SOURCE=.\cdattst.c
# End Source File
# Begin Source File

SOURCE=.\cdetst.c
# End Source File
# Begin Source File

SOURCE=.\cdtdptst.c
# End Source File
# Begin Source File

SOURCE=.\cdtrgtst.c
# End Source File
# Begin Source File

SOURCE=.\cestst.c
# End Source File
# Begin Source File

SOURCE=.\cfintst.c
# End Source File
# Begin Source File

SOURCE=.\cformtst.c
# End Source File
# Begin Source File

SOURCE=.\cfrtst.c
# End Source File
# Begin Source File

SOURCE=.\cg7coll.c
# End Source File
# Begin Source File

SOURCE=.\chashtst.c
# End Source File
# Begin Source File

SOURCE=.\cintltst.c
# End Source File
# Begin Source File

SOURCE=.\citertst.c
# End Source File
# Begin Source File

SOURCE=.\cjaptst.c
# End Source File
# Begin Source File

SOURCE=.\cloctst.c
# End Source File
# Begin Source File

SOURCE=.\cmsgtst.c
# End Source File
# Begin Source File

SOURCE=.\cnmdptst.c
# End Source File
# Begin Source File

SOURCE=.\cnormtst.c
# End Source File
# Begin Source File

SOURCE=.\cnumtst.c
# End Source File
# Begin Source File

SOURCE=.\cregrtst.c
# End Source File
# Begin Source File

SOURCE=.\crestst.c
# End Source File
# Begin Source File

SOURCE=.\creststn.c
# End Source File
# Begin Source File

SOURCE=.\cturtst.c
# End Source File
# Begin Source File

SOURCE=.\cucdtst.c
# End Source File
# Begin Source File

SOURCE=.\cutiltst.c
# End Source File
# Begin Source File

SOURCE=.\encoll.c
# End Source File
# Begin Source File

SOURCE=.\eurocreg.c
# End Source File
# Begin Source File

SOURCE=.\nccbtst.c
# End Source File
# Begin Source File

SOURCE=.\ncnvfbts.c
# End Source File
# Begin Source File

SOURCE=.\ncnvtst.c
# End Source File
# Begin Source File

SOURCE=.\nucnvtst.c
# End Source File
# Begin Source File

SOURCE=.\putiltst.c
# End Source File
# Begin Source File

SOURCE=.\susctest.c
# End Source File
# Begin Source File

SOURCE=.\udatatst.c
# End Source File
# Begin Source File

SOURCE=.\utf16tst.c
# End Source File
# Begin Source File

SOURCE=.\utransts.c
# End Source File
# End Group
# Begin Group "Header Files"

# PROP Default_Filter "h;hpp;hxx;hm;inl"
# Begin Source File

SOURCE=.\callcoll.h
# End Source File
# Begin Source File

SOURCE=.\capitst.h
# End Source File
# Begin Source File

SOURCE=.\cbiapts.h
# End Source File
# Begin Source File

SOURCE=.\cbiditst.h
# End Source File
# Begin Source File

SOURCE=.\ccaltst.h
# End Source File
# Begin Source File

SOURCE=.\ccapitst.h
# End Source File
# Begin Source File

SOURCE=.\ccolltst.h
# End Source File
# Begin Source File

SOURCE=.\ccurrtst.h
# End Source File
# Begin Source File

SOURCE=.\cdantst.h
# End Source File
# Begin Source File

SOURCE=.\cdattst.h
# End Source File
# Begin Source File

SOURCE=.\cdetst.h
# End Source File
# Begin Source File

SOURCE=.\cdtdptst.h
# End Source File
# Begin Source File

SOURCE=.\cdtrgtst.h
# End Source File
# Begin Source File

SOURCE=.\cestst.h
# End Source File
# Begin Source File

SOURCE=.\cfintst.h
# End Source File
# Begin Source File

SOURCE=.\cformtst.h
# End Source File
# Begin Source File

SOURCE=.\cfrtst.h
# End Source File
# Begin Source File

SOURCE=.\cg7coll.h
# End Source File
# Begin Source File

SOURCE=.\cintltst.h
# End Source File
# Begin Source File

SOURCE=.\citertst.h
# End Source File
# Begin Source File

SOURCE=.\cjaptst.h
# End Source File
# Begin Source File

SOURCE=.\cloctst.h
# End Source File
# Begin Source File

SOURCE=.\cmsgtst.h
# End Source File
# Begin Source File

SOURCE=.\cnmdptst.h
# End Source File
# Begin Source File

SOURCE=.\cnormtst.h
# End Source File
# Begin Source File

SOURCE=.\cnumtst.h
# End Source File
# Begin Source File

SOURCE=.\cregrtst.h
# End Source File
# Begin Source File

SOURCE=.\crestst.h
# End Source File
# Begin Source File

SOURCE=.\creststn.h
# End Source File
# Begin Source File

SOURCE=.\cturtst.h
# End Source File
# Begin Source File

SOURCE=.\cucdtst.h
# End Source File
# Begin Source File

SOURCE=.\encoll.h
# End Source File
# Begin Source File

SOURCE=.\nccbtst.h
# End Source File
# Begin Source File

SOURCE=.\ncnvfbts.h
# End Source File
# Begin Source File

SOURCE=.\nucnvtst.h
# End Source File
# End Group
# Begin Group "Resource Files"

# PROP Default_Filter "ico;cur;bmp;dlg;rc2;rct;bin;rgs;gif;jpg;jpeg;jpe"
# End Group
# End Target
# End Project
