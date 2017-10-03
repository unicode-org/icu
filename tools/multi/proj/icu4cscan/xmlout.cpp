// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
// Copyright (C) 2008-2012 IBM Corporation and Others. All Rights Reserved. 
#include <stdio.h>
#include "xmlout.h"
#include <unistd.h>
#include <stdlib.h>

XMLFile::XMLFile(FILE *f) {
    file = f;
    level = 0;
}

XMLFile::~XMLFile()
{

}

void XMLFile::write(const char *s) {
    fputs(s, file);
}

void XMLFile::writeln(const char *s) {
    writeIndent();
    write(s);
    write("\n");
}

int XMLFile::indent(const char *s, bool single) {
    int oldLevel = level;
    writeln(s);
    level++;
    if(single) {
        level--;
    }
    return oldLevel;
}
int XMLFile::outdent(const char *s) {
    level--;
    writeln(s);
    return level;
}

void XMLFile::writeIndent() {
    for(int i=0;i<level;i++) {
        write("\t");
    }
}

XMLElement::XMLElement(XMLFile &f, const char *name, const char *attribs, bool single) : file(f), name(name), single(single) {
    char outs[200];
    if(attribs!=NULL) {
        sprintf(outs,"<%s %s", name, attribs);
    } else {
        sprintf(outs, "<%s", name);
    }
    if(single) {
        strcat(outs, "/>");
    } else {
        strcat(outs, ">");
    }
    oldlevel = file.indent(outs, single);
}

XMLElement::~XMLElement() {
    if(!single) {
        char outs[200];
        sprintf(outs,"</%s>", name);
        int newlevel = file.outdent(outs);
        if(newlevel != oldlevel) {
            fprintf(stderr, "@@@ ERROR: elemet %s popped out to level %d but expected %d. Abort.\n", name, newlevel, oldlevel);
            fflush(stderr);
            abort();
        }
    }
}



