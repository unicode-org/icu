// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
// Copyright (C) 2008-2012 IBM Corporation and Others. All Rights Reserved. 
#include <stdio.h>
#include "unicode/utypes.h"
#include <string.h>

class XMLFile {
    public:  
    XMLFile(FILE *f);
     ~XMLFile();
     /**
      * Write indent at current level, increment level, and then return what the initial level was
      */
     int indent(const char *s, bool single = false);
     /**
      * Decrement level, write indent of 'outer' level, and return what the new level is. Should match your earlier call to indent.
      */
     int outdent(const char *s);
     
     /**
      * Write some string 
      */
     void writeln(const char *s);
     
     private:
       void writeIndent();
     /**
      * Write some string without indent. */
     void write(const char *s);
     
    int level;
    FILE *file;
};

class XMLElement {
    public:
        XMLElement(XMLFile &f, const char *name, const char *attribs  = nullptr, bool single=false);
        ~XMLElement();
        
        const char *name;
        int oldlevel;
        XMLFile &file;
        bool single;
};


    

