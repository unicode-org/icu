ufortune sample program demonstrating use of ICU resource files.

This sample demonstrates
         Defining resources for use by an application
         Compiling and packaging them into a dll
         Referencing the resource-containing dll from application code
         Loading resource data using ICU's API

To Build ufortune on Windows
    1.  Install and build ICU
    2.  In MSVC, open the workspace file icu\samples\ufortune\ufortune.dsw
    3.  Choose a Debug or Release build.
    4.  Build.
	
To Run on Windows
    1.  Start a command shell window
    2.  Add ICU's bin directory to the path, e.g.
            set PATH=c:\icu\bin;%PATH%
        (Use the path to where ever ICU is on your system.)
    3.  cd into the ufortune directory, e.g.
            cd c:\icu\source\samples\ufortune\debug
    4.  Run it
            ufortune

To build on Unixes
    ...
    
To run on Unixes
    gmake check
    