# Microsoft Developer Studio Project File - Name="intltest" - Package Owner=<4>
# Microsoft Developer Studio Generated Build File, Format Version 6.00
# ** DO NOT EDIT **

# TARGTYPE "Win32 (x86) Console Application" 0x0103

CFG=intltest - Win32 Debug
!MESSAGE This is not a valid makefile. To build this project using NMAKE,
!MESSAGE use the Export Makefile command and run
!MESSAGE 
!MESSAGE NMAKE /f "intltest.mak".
!MESSAGE 
!MESSAGE You can specify a configuration when running NMAKE
!MESSAGE by defining the macro CFG on the command line. For example:
!MESSAGE 
!MESSAGE NMAKE /f "intltest.mak" CFG="intltest - Win32 Debug"
!MESSAGE 
!MESSAGE Possible choices for configuration are:
!MESSAGE 
!MESSAGE "intltest - Win32 Release" (based on "Win32 (x86) Console Application")
!MESSAGE "intltest - Win32 Debug" (based on "Win32 (x86) Console Application")
!MESSAGE "intltest - Win64 Release" (based on "Win32 (x86) Console Application")
!MESSAGE "intltest - Win64 Debug" (based on "Win32 (x86) Console Application")
!MESSAGE 

# Begin Project
# PROP AllowPerConfigDependencies 0
# PROP Scc_ProjName ""
# PROP Scc_LocalPath ""
CPP=cl.exe
RSC=rc.exe

!IF  "$(CFG)" == "intltest - Win32 Release"

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
MTL=midl.exe
# ADD BASE CPP /nologo /W3 /GX /O2 /D "WIN32" /D "NDEBUG" /D "_CONSOLE" /D "_MBCS" /FD /c
# ADD CPP /nologo /G6 /MT /Za /W3 /GX /Zi /Ox /Ob0 /I "..\..\..\include" /I "..\..\..\source\common" /I "..\..\..\source\i18n" /I "..\..\tools\toolutil" /D "WIN32" /D "NDEBUG" /D "_CONSOLE" /D "_MBCS" /FD /c
# ADD BASE RSC /l 0x409 /d "NDEBUG"
# ADD RSC /l 0x409 /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:console /machine:I386
# ADD LINK32 /nologo /subsystem:console /machine:I386 /pdbtype:sept /libpath:"..\..\..\lib"
# SUBTRACT LINK32 /pdb:none

!ELSEIF  "$(CFG)" == "intltest - Win32 Debug"

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
MTL=midl.exe
# ADD BASE CPP /nologo /W3 /Gm /GX /ZI /Od /D "WIN32" /D "_DEBUG" /D "_CONSOLE" /D "_MBCS" /FD /GZ /c
# ADD CPP /nologo /G6 /MTd /Za /W3 /Gm /GX /ZI /Od /I "..\..\..\include" /I "..\..\..\source\common" /I "..\..\..\source\i18n" /I "..\..\tools\toolutil" /D "WIN32" /D "_DEBUG" /D "_CONSOLE" /D "_MBCS" /D "UDATA_MAP_DLL" /FR /FD /GZ /c
# ADD BASE RSC /l 0x409 /d "_DEBUG"
# ADD RSC /l 0x409 /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:console /debug /machine:I386 /pdbtype:sept
# ADD LINK32 /nologo /subsystem:console /debug /machine:I386 /pdbtype:sept /libpath:"..\..\..\lib" /warn:3
# SUBTRACT LINK32 /pdb:none /map

!ELSEIF  "$(CFG)" == "intltest - Win64 Release"

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
MTL=midl.exe
# ADD BASE CPP /nologo /W3 /GX /O2 /D "WIN64" /D "NDEBUG" /D "_CONSOLE" /D "_MBCS" /FD /c
# ADD CPP /nologo /MD /Za /W3 /GX /Zi /O2 /I "..\..\..\include" /I "..\..\..\source\common" /I "..\..\..\source\i18n" /I "..\..\tools\toolutil" /D "WIN64" /D "NDEBUG" /D "_CONSOLE" /D "_MBCS" /D "_IA64_" /D "WIN32" /D "_AFX_NO_DAO_SUPPORT" /FD /QIA64_fmaopt /Wp64 /Zm600 /c
# ADD BASE RSC /l 0x409 /d "NDEBUG"
# ADD RSC /l 0x409 /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:console /machine:IX86 /machine:IA64
# ADD LINK32 icuuc.lib icuin.lib icutu.lib /nologo /subsystem:console /machine:IX86 /pdbtype:sept /libpath:"..\..\..\lib" /machine:IA64

!ELSEIF  "$(CFG)" == "intltest - Win64 Debug"

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
MTL=midl.exe
# ADD BASE CPP /nologo /W3 /Gm /GX /ZI /Od /D "WIN64" /D "_DEBUG" /D "_CONSOLE" /D "_MBCS" /FD /GZ /c
# ADD CPP /nologo /MDd /Za /W3 /Gm /GX /Zi /Od /I "..\..\..\include" /I "..\..\..\source\common" /I "..\..\..\source\i18n" /I "..\..\tools\toolutil" /D "WIN64" /D "_DEBUG" /D "_CONSOLE" /D "_MBCS" /D "UDATA_MAP_DLL" /D "_IA64_" /D "WIN32" /D "_AFX_NO_DAO_SUPPORT" /FR /FD /GZ /QIA64_fmaopt /Wp64 /Zm600 /c
# ADD BASE RSC /l 0x409 /d "_DEBUG"
# ADD RSC /l 0x409 /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:console /debug /machine:IX86 /pdbtype:sept /machine:IA64
# ADD LINK32 icuucd.lib icuind.lib icutud.lib /nologo /subsystem:console /incremental:no /debug /machine:IX86 /pdbtype:sept /libpath:"..\..\..\lib\\" /libpath:"..\..\..\lib" /machine:IA64

!ENDIF 

# Begin Target

# Name "intltest - Win32 Release"
# Name "intltest - Win32 Debug"
# Name "intltest - Win64 Release"
# Name "intltest - Win64 Debug"
# Begin Group "break iteration"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\itrbbi.cpp
# End Source File
# Begin Source File

SOURCE=.\itrbbi.h
# End Source File
# Begin Source File

SOURCE=.\rbbiapts.cpp
# End Source File
# Begin Source File

SOURCE=.\rbbiapts.h
# End Source File
# Begin Source File

SOURCE=.\rbbitst.cpp
# End Source File
# Begin Source File

SOURCE=.\rbbitst.h
# End Source File
# End Group
# Begin Group "collation"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\allcoll.cpp
# End Source File
# Begin Source File

SOURCE=.\allcoll.h
# End Source File
# Begin Source File

SOURCE=.\apicoll.cpp

!IF  "$(CFG)" == "intltest - Win32 Release"

!ELSEIF  "$(CFG)" == "intltest - Win32 Debug"

# ADD CPP /MTd

!ELSEIF  "$(CFG)" == "intltest - Win64 Release"

!ELSEIF  "$(CFG)" == "intltest - Win64 Debug"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=.\apicoll.h
# End Source File
# Begin Source File

SOURCE=.\cntabcol.cpp
# End Source File
# Begin Source File

SOURCE=.\cntabcol.h
# End Source File
# Begin Source File

SOURCE=.\currcoll.cpp
# End Source File
# Begin Source File

SOURCE=.\currcoll.h
# End Source File
# Begin Source File

SOURCE=.\dacoll.cpp
# End Source File
# Begin Source File

SOURCE=.\dacoll.h
# End Source File
# Begin Source File

SOURCE=.\dadrcoll.cpp
# End Source File
# Begin Source File

SOURCE=.\dadrcoll.h
# End Source File
# Begin Source File

SOURCE=.\decoll.cpp
# End Source File
# Begin Source File

SOURCE=.\decoll.h
# End Source File
# Begin Source File

SOURCE=.\encoll.cpp
# End Source File
# Begin Source File

SOURCE=.\encoll.h
# End Source File
# Begin Source File

SOURCE=.\escoll.cpp
# End Source File
# Begin Source File

SOURCE=.\escoll.h
# End Source File
# Begin Source File

SOURCE=.\ficoll.cpp
# End Source File
# Begin Source File

SOURCE=.\ficoll.h
# End Source File
# Begin Source File

SOURCE=.\frcoll.cpp
# End Source File
# Begin Source File

SOURCE=.\frcoll.h
# End Source File
# Begin Source File

SOURCE=.\g7coll.cpp
# End Source File
# Begin Source File

SOURCE=.\g7coll.h
# End Source File
# Begin Source File

SOURCE=.\itercoll.cpp
# End Source File
# Begin Source File

SOURCE=.\itercoll.h
# End Source File
# Begin Source File

SOURCE=.\jacoll.cpp
# End Source File
# Begin Source File

SOURCE=.\jacoll.h
# End Source File
# Begin Source File

SOURCE=.\lcukocol.cpp
# End Source File
# Begin Source File

SOURCE=.\lcukocol.h
# End Source File
# Begin Source File

SOURCE=.\mnkytst.cpp
# End Source File
# Begin Source File

SOURCE=.\mnkytst.h
# End Source File
# Begin Source File

SOURCE=.\regcoll.cpp
# End Source File
# Begin Source File

SOURCE=.\regcoll.h
# End Source File
# Begin Source File

SOURCE=.\srchtest.cpp
# End Source File
# Begin Source File

SOURCE=.\srchtest.h
# End Source File
# Begin Source File

SOURCE=.\svccoll.cpp
# End Source File
# Begin Source File

SOURCE=.\svccoll.h
# End Source File
# Begin Source File

SOURCE=.\thcoll.cpp
# End Source File
# Begin Source File

SOURCE=.\thcoll.h
# End Source File
# Begin Source File

SOURCE=.\trcoll.cpp
# End Source File
# Begin Source File

SOURCE=.\trcoll.h
# End Source File
# Begin Source File

SOURCE=.\tscoll.cpp
# End Source File
# Begin Source File

SOURCE=.\tscoll.h
# End Source File
# Begin Source File

SOURCE=.\ucaconf.cpp
# End Source File
# Begin Source File

SOURCE=.\ucaconf.h
# End Source File
# End Group
# Begin Group "collections"

# PROP Default_Filter ""
# End Group
# Begin Group "configuration"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\strtest.cpp
# End Source File
# Begin Source File

SOURCE=.\strtest.h
# End Source File
# Begin Source File

SOURCE=.\tsmthred.cpp
# ADD CPP /Ze
# End Source File
# Begin Source File

SOURCE=.\tsmthred.h
# End Source File
# Begin Source File

SOURCE=.\tsmutex.cpp
# End Source File
# Begin Source File

SOURCE=.\tsmutex.h
# End Source File
# Begin Source File

SOURCE=.\tsputil.cpp
# End Source File
# Begin Source File

SOURCE=.\tsputil.h
# End Source File
# End Group
# Begin Group "data & memory"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\uobjtest.cpp
# End Source File
# Begin Source File

SOURCE=.\uobjtest.h
# End Source File
# End Group
# Begin Group "formatting"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\callimts.cpp
# End Source File
# Begin Source File

SOURCE=.\callimts.h
# End Source File
# Begin Source File

SOURCE=.\calregts.cpp
# End Source File
# Begin Source File

SOURCE=.\calregts.h
# End Source File
# Begin Source File

SOURCE=.\caltest.cpp
# End Source File
# Begin Source File

SOURCE=.\caltest.h
# End Source File
# Begin Source File

SOURCE=.\caltztst.cpp
# End Source File
# Begin Source File

SOURCE=.\caltztst.h
# End Source File
# Begin Source File

SOURCE=.\dcfmapts.cpp
# End Source File
# Begin Source File

SOURCE=.\dcfmapts.h
# End Source File
# Begin Source File

SOURCE=.\dtfmapts.cpp
# End Source File
# Begin Source File

SOURCE=.\dtfmapts.h
# End Source File
# Begin Source File

SOURCE=.\dtfmrgts.cpp
# End Source File
# Begin Source File

SOURCE=.\dtfmrgts.h
# End Source File
# Begin Source File

SOURCE=.\dtfmtrtts.cpp
# End Source File
# Begin Source File

SOURCE=.\dtfmtrtts.h
# End Source File
# Begin Source File

SOURCE=.\dtfmttst.cpp
# End Source File
# Begin Source File

SOURCE=.\dtfmttst.h
# End Source File
# Begin Source File

SOURCE=.\incaltst.cpp
# End Source File
# Begin Source File

SOURCE=.\incaltst.h
# End Source File
# Begin Source File

SOURCE=.\itformat.cpp
# End Source File
# Begin Source File

SOURCE=.\itformat.h
# End Source File
# Begin Source File

SOURCE=.\itrbnf.cpp
# End Source File
# Begin Source File

SOURCE=.\itrbnf.h
# End Source File
# Begin Source File

SOURCE=.\itrbnfrt.cpp
# End Source File
# Begin Source File

SOURCE=.\itrbnfrt.h
# End Source File
# Begin Source File

SOURCE=.\miscdtfm.cpp
# End Source File
# Begin Source File

SOURCE=.\miscdtfm.h
# End Source File
# Begin Source File

SOURCE=.\msfmrgts.cpp
# End Source File
# Begin Source File

SOURCE=.\msfmrgts.h
# End Source File
# Begin Source File

SOURCE=.\nmfmapts.cpp
# End Source File
# Begin Source File

SOURCE=.\nmfmapts.h
# End Source File
# Begin Source File

SOURCE=.\nmfmtrt.cpp
# End Source File
# Begin Source File

SOURCE=.\nmfmtrt.h
# End Source File
# Begin Source File

SOURCE=.\numfmtst.cpp
# End Source File
# Begin Source File

SOURCE=.\numfmtst.h
# End Source File
# Begin Source File

SOURCE=.\numrgts.cpp
# End Source File
# Begin Source File

SOURCE=.\numrgts.h
# End Source File
# Begin Source File

SOURCE=.\pptest.cpp
# End Source File
# Begin Source File

SOURCE=.\pptest.h
# End Source File
# Begin Source File

SOURCE=.\sdtfmtts.cpp
# End Source File
# Begin Source File

SOURCE=.\sdtfmtts.h
# End Source File
# Begin Source File

SOURCE=.\tchcfmt.cpp
# End Source File
# Begin Source File

SOURCE=.\tchcfmt.h
# End Source File
# Begin Source File

SOURCE=.\tfsmalls.cpp
# End Source File
# Begin Source File

SOURCE=.\tfsmalls.h
# End Source File
# Begin Source File

SOURCE=.\tmsgfmt.cpp
# End Source File
# Begin Source File

SOURCE=.\tmsgfmt.h
# End Source File
# Begin Source File

SOURCE=.\tsdate.cpp
# End Source File
# Begin Source File

SOURCE=.\tsdate.h
# End Source File
# Begin Source File

SOURCE=.\tsdcfmsy.cpp
# End Source File
# Begin Source File

SOURCE=.\tsdcfmsy.h
# End Source File
# Begin Source File

SOURCE=.\tsdtfmsy.cpp
# End Source File
# Begin Source File

SOURCE=.\tsdtfmsy.h
# End Source File
# Begin Source File

SOURCE=.\tsnmfmt.cpp
# End Source File
# Begin Source File

SOURCE=.\tsnmfmt.h
# End Source File
# Begin Source File

SOURCE=.\tzbdtest.cpp
# End Source File
# Begin Source File

SOURCE=.\tzbdtest.h
# End Source File
# Begin Source File

SOURCE=.\tzregts.cpp
# End Source File
# Begin Source File

SOURCE=.\tzregts.h
# End Source File
# Begin Source File

SOURCE=.\tztest.cpp
# End Source File
# Begin Source File

SOURCE=.\tztest.h
# End Source File
# End Group
# Begin Group "locales & resources"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\loctest.cpp
# End Source File
# Begin Source File

SOURCE=.\loctest.h
# End Source File
# Begin Source File

SOURCE=.\restest.cpp
# End Source File
# Begin Source File

SOURCE=.\restest.h
# End Source File
# Begin Source File

SOURCE=.\restsnew.cpp
# End Source File
# Begin Source File

SOURCE=.\restsnew.h
# End Source File
# End Group
# Begin Group "misc"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\datamap.cpp
# End Source File
# Begin Source File

SOURCE=.\datamap.h
# End Source File
# Begin Source File

SOURCE=.\intltest.cpp
# End Source File
# Begin Source File

SOURCE=.\intltest.h
# End Source File
# Begin Source File

SOURCE=.\itmajor.cpp
# End Source File
# Begin Source File

SOURCE=.\itmajor.h
# End Source File
# Begin Source File

SOURCE=.\itutil.cpp
# End Source File
# Begin Source File

SOURCE=.\itutil.h
# End Source File
# Begin Source File

SOURCE=.\testdata.cpp
# End Source File
# Begin Source File

SOURCE=.\testdata.h
# End Source File
# Begin Source File

SOURCE=.\testutil.cpp
# End Source File
# Begin Source File

SOURCE=.\testutil.h
# End Source File
# Begin Source File

SOURCE=.\tstdtmod.cpp
# End Source File
# Begin Source File

SOURCE=.\tstdtmod.h
# End Source File
# End Group
# Begin Group "normalization"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\canittst.cpp
# End Source File
# Begin Source File

SOURCE=.\canittst.h
# End Source File
# Begin Source File

SOURCE=.\normconf.cpp
# End Source File
# Begin Source File

SOURCE=.\normconf.h
# End Source File
# Begin Source File

SOURCE=.\tstnorm.cpp
# End Source File
# Begin Source File

SOURCE=.\tstnorm.h
# End Source File
# Begin Source File

SOURCE=.\tstnrapi.cpp
# End Source File
# End Group
# Begin Group "properties & sets"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\ucdtest.cpp
# End Source File
# Begin Source File

SOURCE=.\ucdtest.h
# End Source File
# Begin Source File

SOURCE=.\usettest.cpp
# End Source File
# Begin Source File

SOURCE=.\usettest.h
# End Source File
# End Group
# Begin Group "regex"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\regextst.cpp
# End Source File
# Begin Source File

SOURCE=.\regextst.h
# End Source File
# End Group
# Begin Group "registration"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\icusvtst.cpp
# End Source File
# Begin Source File

SOURCE=.\icusvtst.h
# End Source File
# End Group
# Begin Group "strings"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\citrtest.cpp
# End Source File
# Begin Source File

SOURCE=.\citrtest.h
# End Source File
# Begin Source File

SOURCE=.\reptest.cpp
# End Source File
# Begin Source File

SOURCE=.\reptest.h
# End Source File
# Begin Source File

SOURCE=.\sfwdchit.cpp
# End Source File
# Begin Source File

SOURCE=.\sfwdchit.h
# End Source File
# Begin Source File

SOURCE=.\strcase.cpp
# End Source File
# Begin Source File

SOURCE=.\ustrtest.cpp
# End Source File
# Begin Source File

SOURCE=.\ustrtest.h
# End Source File
# End Group
# Begin Group "transforms"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\cpdtrtst.cpp
# End Source File
# Begin Source File

SOURCE=.\cpdtrtst.h
# End Source File
# Begin Source File

SOURCE=.\hxuntrts.cpp
# End Source File
# Begin Source File

SOURCE=.\hxuntrts.h
# End Source File
# Begin Source File

SOURCE=.\ittrans.cpp
# End Source File
# Begin Source File

SOURCE=.\ittrans.h
# End Source File
# Begin Source File

SOURCE=.\jamotest.cpp
# End Source File
# Begin Source File

SOURCE=.\jamotest.h
# End Source File
# Begin Source File

SOURCE=.\transapi.cpp
# End Source File
# Begin Source File

SOURCE=.\transapi.h
# End Source File
# Begin Source File

SOURCE=.\transrt.cpp
# End Source File
# Begin Source File

SOURCE=.\transrt.h
# End Source File
# Begin Source File

SOURCE=.\transtst.cpp
# End Source File
# Begin Source File

SOURCE=.\transtst.h
# End Source File
# Begin Source File

SOURCE=.\trnserr.cpp
# End Source File
# Begin Source File

SOURCE=.\trnserr.h
# End Source File
# Begin Source File

SOURCE=.\ufltlgts.cpp
# End Source File
# Begin Source File

SOURCE=.\ufltlgts.h
# End Source File
# Begin Source File

SOURCE=.\unhxtrts.cpp
# End Source File
# Begin Source File

SOURCE=.\unhxtrts.h
# End Source File
# End Group
# Begin Group "idna"

# PROP Default_Filter "*.c,*.h"
# Begin Source File

SOURCE=.\idnaref.cpp
# End Source File
# Begin Source File

SOURCE=.\idnaref.h
# End Source File
# Begin Source File

SOURCE=.\nptrans.cpp
# End Source File
# Begin Source File

SOURCE=.\nptrans.h
# End Source File
# Begin Source File

SOURCE=.\punyref.c
# End Source File
# Begin Source File

SOURCE=.\punyref.h
# End Source File
# Begin Source File

SOURCE=.\testidn.cpp
# End Source File
# Begin Source File

SOURCE=.\testidna.cpp
# End Source File
# Begin Source File

SOURCE=.\testidna.h
# End Source File
# End Group
# End Target
# End Project
