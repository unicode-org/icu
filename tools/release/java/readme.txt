# Copyright (C) 2009 IBM and Others. All Rights Reserved

A tool to generate a report of API status changes between two ICU releases

        To use the utility
        1. setup ICU
            (put the two ICU releases on your machine ^_^ 
		run 'configure' in both releases (or runConfigureICU)
	2. create a Makefile.local with these two lines:
			OLD_ICU=/xsrl/E/icu-1.0
			NEW_ICU=/xsrl/E/icu-6.8
	   ( where these are the paths to the parent of 'source', etc)
        3. Build the API docs
            make
	4. allow APIChangeReport.html to cool before use.
 
