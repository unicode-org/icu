// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*
*   Copyright (C) 2014, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*/

// file name: genregexcasing.cpp
//
// Program to generate the casing data for use by ICU regular expressions.
// The data declarations output when running this program are to be copied
// into the file i18n/regexcmp.h
//
// See the function RegexCompile::findCaseInsensitiveStarters() for more explanation.

#include "unicode/uniset.h"
#include "unicode/usetiter.h"
#include "iostream"
#include <map>
#include <set>
#include <string>
#include <vector>

using namespace std;

std::string sstring(const UnicodeString &us) {
    string retString;
    us.toUTF8String(retString);
    return retString;
}

int main() {

    std::map<UChar32, std::set<UChar32>> cmap;

    for (UChar32 cp = 0; cp<=0x10ffff; cp++) {
        UnicodeSet s(cp, cp);
        s.closeOver(USET_CASE_INSENSITIVE);

        UnicodeSetIterator setIter(s);
        while (setIter.next()) {
            if (!setIter.isString()) {
                continue;
            }
            const UnicodeString &str = setIter.getString();

            cout << "Got a string for \"" << sstring(UnicodeString(cp)) << "\" [\\u" << hex << cp << "]\n";
            cout << "    \"" << sstring(str) << "\"    [";
            for (int32_t j=0; j<str.length(); j=str.moveIndex32(j, 1)) {
                cout << hex << "\\u" << str.char32At(j) << " ";
            }
            cout << "]" << endl;
            UChar32 c32 = str.char32At(0);
            if (s.contains(c32)) {
                cout << "    Set contains first char.\n";
            }
            cmap[c32].insert(cp);
        }
    }


    std::cout << "Iterating the map.\n";
    for (const auto &mapPair: cmap) {
        UChar32 cp = mapPair.first;
        std::cout << "key: \"" << sstring(UnicodeString(cp)) << "\"  \\u" << cp << " :  [";
        for (UChar32 valCP: mapPair.second) {
           std::cout << "\"" << sstring(UnicodeString(valCP)) << "\" \\u" << valCP << " ";
        }
        std::cout << "]\n";
    }

    //
    // Create the data arrays to be pasted into regexcmp.cpp
    //

    std::cout << "\n\nCopy the lines below into the file i18n/regexcmp.cpp.\n\n";
    std::cout << "// Machine Generated Data. Do not hand edit.\n";

    UnicodeString outString;
    struct Item {
        UChar32  fCP = 0;
        int16_t  fStrIndex = 0;
        int16_t  fCount = 0;
    };

    std::vector<Item> data;
    for (const auto &mapPair: cmap) {
        Item   dataForCP;
        dataForCP.fCP = mapPair.first;
        dataForCP.fStrIndex = outString.length();
        for (UChar32 valCP: mapPair.second) {
            outString.append(valCP);
            dataForCP.fCount++;
        }
        data.push_back(dataForCP);
    }

    std::cout << "    static const UChar32 RECaseFixCodePoints[] = {" ;
    int items=0;
    for (const Item &d: data) {
        if (items++ % 10 == 0) {
            std::cout << "\n        ";
        }
        std::cout << "0x" << d.fCP << ", ";
    }
    std::cout << "0x110000};\n\n";

    std::cout << "    static const int16_t RECaseFixStringOffsets[] = {";
    items = 0;
    for (const Item &d: data) {
        if (items++ % 10 == 0) {
            std::cout << "\n        ";
        }
        std::cout << "0x" << d.fStrIndex << ", ";
    }
    std::cout << "0};\n\n";

    std::cout << "    static const int16_t RECaseFixCounts[] = {";
    items = 0;
    for (const Item &d: data) {
        if (items++ % 10 == 0) {
            std::cout << "\n        ";
        }
        std::cout << "0x" << d.fCount << ", ";
    }
    std::cout << "0};\n\n";

    std::cout << "    static const char16_t RECaseFixData[] = {";
    for (int i=0; i<outString.length(); i++) {
        if (i % 10 == 0) {
            std::cout << "\n        ";
        }
        std::cout << "0x" << outString.charAt(i) << ", ";
    }
    std::cout << "0};\n\n";
    return 0;
}

