# Microsoft Developer Studio Project File - Name="derb" - Package Owner=<4>
# Microsoft Developer Studio Generated Build File, Format Version 6.00
# ** DO NOT EDIT **

# TARGTYPE "Win32 (x86) Console Application" 0x0103

CFG=derb - Win32 Debug
!MESSAGE This is not a valid makefile. To build this project using NMAKE,
!MESSAGE use the Export Makefile command and run
!MESSAGE 
!MESSAGE NMAKE /f "derb.mak".
!MESSAGE 
!MESSAGE You can specify a configuration when running NMAKE
!MESSAGE by defining the macro CFG on the command line. For example:
!MESSAGE 
!MESSAGE NMAKE /f "derb.mak" CFG="derb - Win32 Debug"
!MESSAGE 
!MESSAGE Possible choices for configuration are:
!MESSAGE 
!MESSAGE "derb - Win32 Release" (based on "Win32 (x86) Console Application")
!MESSAGE "derb - Win32 Debug" (based on "Win32 (x86) Console Application")
!MESSAGE "derb - Win64 Release" (based on "Win32 (x86) Console Application")
!MESSAGE "derb - Win64 Debug" (based on "Win32 (x86) Console Application")
!MESSAGE 

# Begin Project
# PROP AllowPerConfigDependencies 0
# PROP Scc_ProjName ""
# PROP Scc_LocalPath ""
CPP=cl.exe
RSC=rc.exe

!IF  "$(CFG)" == "derb - Win32 Release"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 0
# PROP BASE Output_Dir "derb___Win32_Release"
# PROP BASE Intermediate_Dir "derb___Win32_Release"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 0
# PROP Output_Dir "derb_Win32_Release"
# PROP Intermediate_Dir "derb_Win32_Release"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
MTL=midl.exe
# ADD BASE MTL /nologo /D "NDEBUG" /mktyplib203 /win32
# ADD MTL /nologo /D "NDEBUG" /mktyplib203 /win32
# ADD BASE CPP /nologo /W3 /GX /O2 /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /FD /c
# ADD CPP /nologo /G6 /MD /Za /W3 /GX /O2 /I "..\..\common" /I "..\toolutil" /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /FD /c
# ADD BASE RSC /l 0x409 /d "NDEBUG"
# ADD RSC /l 0x409 /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:console /machine:I386
# ADD LINK32 icuuc.lib icutu.lib /nologo /subsystem:console /machine:I386 /libpath:"..\..\..\lib"
# Begin Custom Build
TargetPath=.\derb_Win32_Release\derb.exe
InputPath=.\derb_Win32_Release\derb.exe
InputName=derb
SOURCE="$(InputPath)"

"..\..\..\bin\$(InputName).exe" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy $(TargetPath) ..\..\..\bin

# End Custom Build

!ELSEIF  "$(CFG)" == "derb - Win32 Debug"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 1
# PROP BASE Output_Dir "derb___Win32_Debug"
# PROP BASE Intermediate_Dir "derb___Win32_Debug"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 1
# PROP Output_Dir "derb_Win32_Debug"
# PROP Intermediate_Dir "derb_Win32_Debug"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
MTL=midl.exe
# ADD BASE MTL /nologo /D "_DEBUG" /mktyplib203 /win32
# ADD MTL /nologo /D "_DEBUG" /mktyplib203 /win32
# ADD BASE CPP /nologo /W3 /Gm /GX /ZI /Od /D "WIN32" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /FD /GZ /c
# ADD CPP /nologo /G6 /MDd /Za /W3 /Gm /GX /ZI /Od /I "..\..\common" /I "..\toolutil" /D "WIN32" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /FD /GZ /c
# ADD BASE RSC /l 0x409 /d "_DEBUG"
# ADD RSC /l 0x409 /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:console /debug /machine:I386 /pdbtype:sept
# ADD LINK32 icuucd.lib icutud.lib /nologo /subsystem:console /debug /machine:I386 /pdbtype:sept /libpath:"..\..\..\lib"
# Begin Custom Build
TargetPath=.\derb_Win32_Debug\derb.exe
InputPath=.\derb_Win32_Debug\derb.exe
InputName=derb
SOURCE="$(InputPath)"

"..\..\..\bin\$(InputName).exe" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy $(TargetPath) ..\..\..\bin

# End Custom Build

!ELSEIF  "$(CFG)" == "derb - Win64 Release"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 0
# PROP BASE Output_Dir "derb___Win64_Release"
# PROP BASE Intermediate_Dir "derb___Win64_Release"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 0
# PROP Output_Dir "derb_Win64_Release"
# PROP Intermediate_Dir "derb_Win64_Release"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
MTL=midl.exe
# ADD BASE MTL /nologo /D "NDEBUG" /mktyplib203 /win64
# ADD MTL /nologo /D "NDEBUG" /mktyplib203 /win64
# ADD BASE CPP /nologo /W3 /GX /O2 /D "WIN64" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /FD /c
# ADD CPP /nologo /MD /Za /W3 /GX /Zi /O2 /I "..\..\common" /I "..\toolutil" /D "WIN64" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_IA64_" /D "WIN32" /D "_AFX_NO_DAO_SUPPORT" /FD /QIA64_fmaopt /Wp64 /Zm600 /c
# ADD BASE RSC /l 0x409 /d "NDEBUG"
# ADD RSC /l 0x409 /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:console /machine:IX86 /machine:IA64
# ADD LINK32 icuuc.lib icutu.lib /nologo /subsystem:console /machine:IX86 /libpath:"..\..\..\lib" /machine:IA64
# Begin Custom Build
TargetPath=.\derb_Win64_Release\derb.exe
InputPath=.\derb_Win64_Release\derb.exe
InputName=derb
SOURCE="$(InputPath)"

"..\..\..\bin\$(InputName).exe" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy $(TargetPath) ..\..\..\bin

# End Custom Build

!ELSEIF  "$(CFG)" == "derb - Win64 Debug"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 1
# PROP BASE Output_Dir "derb___Win64_Debug"
# PROP BASE Intermediate_Dir "derb___Win64_Debug"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 1
# PROP Output_Dir "derb_Win64_Debug"
# PROP Intermediate_Dir "derb_Win64_Debug"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
MTL=midl.exe
# ADD BASE MTL /nologo /D "_DEBUG" /mktyplib203 /win64
# ADD MTL /nologo /D "_DEBUG" /mktyplib203 /win64
# ADD BASE CPP /nologo /W3 /Gm /GX /ZI /Od /D "WIN64" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /FD /GZ /c
# ADD CPP /nologo /MDd /Za /W3 /Gm /GX /Zi /Od /I "..\..\common" /I "..\toolutil" /D "WIN64" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_IA64_" /D "WIN32" /D "_AFX_NO_DAO_SUPPORT" /FD /GZ /QIA64_fmaopt /Wp64 /Zm600 /c
# ADD BASE RSC /l 0x409 /d "_DEBUG"
# ADD RSC /l 0x409 /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:console /debug /machine:IX86 /pdbtype:sept /machine:IA64
# ADD LINK32 icuucd.lib icutud.lib /nologo /subsystem:console /incremental:no /debug /machine:IX86 /pdbtype:sept /libpath:"..\..\..\lib" /machine:IA64
# Begin Custom Build
TargetPath=.\derb_Win64_Debug\derb.exe
InputPath=.\derb_Win64_Debug\derb.exe
InputName=derb
SOURCE="$(InputPath)"

"..\..\..\bin\$(InputName).exe" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	copy $(TargetPath) ..\..\..\bin

# End Custom Build

!ENDIF 

# Begin Target

# Name "derb - Win32 Release"
# Name "derb - Win32 Debug"
# Name "derb - Win64 Release"
# Name "derb - Win64 Debug"
# Begin Group "Source Files"

# PROP Default_Filter "cpp;c;cxx;rc;def;r;odl;idl;hpj;bat"
# Begin Source File

SOURCE=.\derb.c
# End Source File
# End Group
# Begin Group "Header Files"

# PROP Default_Filter "h;hpp;hxx;hm;inl"
# End Group
# Begin Group "Resource Files"

# PROP Default_Filter "ico;cur;bmp;dlg;rc2;rct;bin;rgs;gif;jpg;jpeg;jpe"
# End Group
# End Target
# End Project
