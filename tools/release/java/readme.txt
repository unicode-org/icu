# Copyright (C) 2008 IBM and Others. All Rights Reserved

A tool to generate a report of API status changes between two ICU releases

        To use the utility
        1. Generate the XML files
            (put the two ICU releases on your machine ^_^ )
            (generate 'Doxygen' file on Windows platform with Cygwin's help)
            Edit the generated 'Doxygen' file under ICU4C source directory
            a) GENERATE_XML           = YES
            b) Sync the ALIASES definiation
               (For example, copy the ALIASES defination from ICU 3.6
               Doxygen file to ICU 3.4 Doxygen file.)
            c) gerenate the XML files
        2. Build the tool
            ant doctools
        3. Edit the api-report.properties and change the values of olddir,   newdir ( don't need to set oldver or newver)
        4. Run the tool to generate the report
            ant apireport
 
