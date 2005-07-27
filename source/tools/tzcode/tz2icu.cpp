/*
**********************************************************************
* Copyright (c) 2003-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: July 10 2003
* Since: ICU 2.8
**********************************************************************
*/
#include "tzfile.h" // from Olson tzcode archive, copied to this dir

#ifdef WIN32

 #include <windows.h>
 #undef min // windows.h/STL conflict
 #undef max // windows.h/STL conflict
 // "identifier was truncated to 'number' characters" warning
 #pragma warning(disable: 4786)

#else

 #include <unistd.h>
 #include <stdio.h>
 #include <dirent.h>
 #include <string.h>
 #include <sys/stat.h>

#endif

#include <algorithm>
#include <cassert>
#include <ctime>
#include <fstream>
#include <iomanip>
#include <iostream>
#include <iterator>
#include <limits>
#include <map>
#include <set>
#include <sstream>
#include <sstream>
#include <stdexcept>
#include <string>
#include <vector>

#include "tz2icu.h"

using namespace std;

//--------------------------------------------------------------------
// Time utilities
//--------------------------------------------------------------------

const long SECS_PER_YEAR      = 31536000; // 365 days
const long SECS_PER_LEAP_YEAR = 31622400; // 366 days

bool isLeap(int y) {
    return (y%4 == 0) && ((y%100 != 0) || (y%400 == 0)); // Gregorian
}

long secsPerYear(int y) {
    return isLeap(y) ? SECS_PER_LEAP_YEAR : SECS_PER_YEAR;
}

/**
 * Given a calendar year, return the GMT epoch seconds for midnight
 * GMT of January 1 of that year.  yearToSeconds(1970) == 0.
 */
long yearToSeconds(int year) {
    // inefficient but foolproof
    long s = 0;
    int y = 1970;
    while (y < year) {
        s += secsPerYear(y++);
    }
    while (y > year) {
        s -= secsPerYear(--y);
    }
    return s;
}

/**
 * Given 1970 GMT epoch seconds, return the calendar year containing
 * that time.  secondsToYear(0) == 1970.
 */
int secondsToYear(long seconds) {
    // inefficient but foolproof
    int y = 1970;
    long s = 0;
    if (seconds >= 0) {
        for (;;) {
            s += secsPerYear(y++);
            if (s > seconds) break;
        }
        --y;
    } else {
        for (;;) {
            s -= secsPerYear(--y);
            if (s <= seconds) break;
        }
    }
    return y;
}

//--------------------------------------------------------------------
// Types
//--------------------------------------------------------------------

struct FinalZone;
struct FinalRule;
struct SimplifiedZoneType;

// A transition from one ZoneType to another
// Minimal size = 5 bytes (4+1)
struct Transition {
    long time;  // seconds, 1970 epoch
    int  type;  // index into 'ZoneInfo.types' 0..255
    Transition(long _time, int _type) {
        time = _time;
        type = _type;
    }
};

// A behavior mode (what zic calls a 'type') of a time zone.
// Minimal size = 6 bytes (4+1+3bits)
// SEE: SimplifiedZoneType
struct ZoneType {
    long rawoffset; // raw seconds offset from GMT
    long dstoffset; // dst seconds offset from GMT

    // We don't really need any of the following, but they are
    // retained for possible future use.  See SimplifiedZoneType.
    int  abbr;      // index into ZoneInfo.abbrs 0..n-1
    bool isdst;
    bool isstd;
    bool isgmt;
    
    ZoneType(const SimplifiedZoneType&); // used by optimizeTypeList

    ZoneType() : rawoffset(-1), dstoffset(-1), abbr(-1) {}

    // A restricted equality, of just the raw and dst offset
    bool matches(const ZoneType& other) {
        return rawoffset == other.rawoffset &&
            dstoffset == other.dstoffset;
    }
};

// A collection of transitions from one ZoneType to another, together
// with a list of the ZoneTypes.  A ZoneInfo object may have a long
// list of transitions between a smaller list of ZoneTypes.
//
// This object represents the contents of a single zic-created
// zoneinfo file.
struct ZoneInfo {
    vector<Transition> transitions;
    vector<ZoneType>   types;
    vector<string>     abbrs;

    string finalRuleID;
    int finalOffset;
    int finalYear; // -1 if none

    // If this is an alias, then all other fields are meaningless, and
    // this field will point to the "real" zone 0..n-1.
    int aliasTo; // -1 if this is a "real" zone

    // If there are aliases TO this zone, then the following set will
    // contain their index numbers (each index >= 0).
    set<int> aliases;

    ZoneInfo() : finalYear(-1), aliasTo(-1) {}

    void mergeFinalData(const FinalZone& fz);

    void optimizeTypeList();

    // Set this zone to be an alias TO another zone.
    void setAliasTo(int index);

    // Clear the list of aliases OF this zone.
    void clearAliases();

    // Add an alias to the list of aliases OF this zone.
    void addAlias(int index);

    // Is this an alias to another zone?
    bool isAlias() const {
        return aliasTo >= 0;
    }

    // Retrieve alias list
    const set<int>& getAliases() const {
        return aliases;
    }

    void print(ostream& os, const string& id) const;
};

void ZoneInfo::clearAliases() {
    assert(aliasTo < 0);
    aliases.clear();
}

void ZoneInfo::addAlias(int index) {
    assert(aliasTo < 0 && index >= 0 && aliases.find(index) == aliases.end());
    aliases.insert(index);
}

void ZoneInfo::setAliasTo(int index) {
    assert(index >= 0);
    assert(aliases.size() == 0);
    aliasTo = index;
}

typedef map<string, ZoneInfo> ZoneMap;

typedef ZoneMap::const_iterator ZoneMapIter;

//--------------------------------------------------------------------
// ZONEINFO
//--------------------------------------------------------------------

// Global map holding all our ZoneInfo objects, indexed by id.
ZoneMap ZONEINFO;

//--------------------------------------------------------------------
// zoneinfo file parsing
//--------------------------------------------------------------------

// Read zic-coded 32-bit integer from file
long readcoded(ifstream& file, long minv=numeric_limits<long>::min(),
                               long maxv=numeric_limits<long>::max()) {
    unsigned char buf[4]; // must be UNSIGNED
    long val=0;
    file.read((char*)buf, 4);
    for(int i=0,shift=24;i<4;++i,shift-=8) {
        val |= buf[i] << shift;
    }
    if (val < minv || val > maxv) {
        ostringstream os;
        os << "coded value out-of-range: " << val << ", expected ["
           << minv << ", " << maxv << "]";
        throw out_of_range(os.str());
    }
    return val;
}

// Read a boolean value
bool readbool(ifstream& file) {
    char c;
    file.read(&c, 1);
    if (c!=0 && c!=1) {
        ostringstream os;
        os << "boolean value out-of-range: " << (int)c;
        throw out_of_range(os.str());
    }
    return (c!=0);
}

/**
 * Read the zoneinfo file structure (see tzfile.h) into a ZoneInfo
 * @param file an already-open file stream
 */
void readzoneinfo(ifstream& file, ZoneInfo& info) {
    int i;

    // Check for TZ_ICU_MAGIC signature at file start.  If we get a
    // signature mismatch, it means we're trying to read a file which
    // isn't a ICU-modified-zic-created zoneinfo file.  Typically this
    // means the user is passing in a "normal" zoneinfo directory, or
    // a zoneinfo directory that is polluted with other files, or that
    // the user passed in the wrong directory.
    char buf[32];
    file.read(buf, 4);
    if (strncmp(buf, TZ_ICU_MAGIC, 4) != 0) {
        throw invalid_argument("TZ_ICU_MAGIC signature missing");
    }

    // Read reserved bytes.  The first of these will be a version byte.
    file.read(buf, 16);
    if (*(ICUZoneinfoVersion*)&buf != TZ_ICU_VERSION) {
        throw invalid_argument("File version mismatch");
    }

    // Read array sizes
    long isgmtcnt = readcoded(file, 0);
    long isdstcnt = readcoded(file, 0);
    long leapcnt  = readcoded(file, 0);
    long timecnt  = readcoded(file, 0);
    long typecnt  = readcoded(file, 0);
    long charcnt  = readcoded(file, 0);

    // Confirm sizes that we assume to be equal.  These assumptions
    // are drawn from a reading of the zic source (2003a), so they
    // should hold unless the zic source changes.
    if (isgmtcnt != typecnt || isdstcnt != typecnt) {
        throw invalid_argument("count mismatch between tzh_ttisgmtcnt, tzh_ttisdstcnt, tth_typecnt");
    }

    // Used temporarily to store transition times and types.  We need
    // to do this because the times and types are stored in two
    // separate arrays.
    vector<long> transitionTimes(timecnt, -1); // temporary
    vector<int>  transitionTypes(timecnt, -1); // temporary

    // Read transition times
    for (i=0; i<timecnt; ++i) {
        transitionTimes[i] = readcoded(file);
    }

    // Read transition types
    for (i=0; i<timecnt; ++i) {
        unsigned char c;
        file.read((char*) &c, 1);
        int t = (int) c;
        if (t < 0 || t >= typecnt) {
            ostringstream os;
            os << "illegal type: " << t << ", expected [0, " << (typecnt-1) << "]";
            throw out_of_range(os.str());
        }
        transitionTypes[i] = t;
    }

    // Build transitions vector out of corresponding times and types.
    for (i=0; i<timecnt; ++i) {
        info.transitions.push_back(Transition(transitionTimes[i], transitionTypes[i]));
    }

    // Read types (except for the isdst and isgmt flags, which come later (why??))
    for (i=0; i<typecnt; ++i) { 
        ZoneType type;

        type.rawoffset = readcoded(file);
        type.dstoffset = readcoded(file);
        type.isdst = readbool(file);

        unsigned char c;
        file.read((char*) &c, 1);
        type.abbr = (int) c;

        if (type.isdst != (type.dstoffset != 0)) {
            throw invalid_argument("isdst does not reflect dstoffset");
        }

        info.types.push_back(type);
    }
    assert(info.types.size() == (unsigned) typecnt);

    // Read the abbreviation string
    if (charcnt) {
        // All abbreviations are concatenated together, with a 0 at
        // the end of each abbr.
        char* str = new char[charcnt + 8];
        file.read(str, charcnt);

        // Split abbreviations apart into individual strings.  Record
        // offset of each abbr in a vector.
        vector<int> abbroffset;
        char *limit=str+charcnt;
        for (char* p=str; p<limit; ++p) {
            char* start = p;
            while (*p != 0) ++p;
            info.abbrs.push_back(string(start, p-start));
            abbroffset.push_back(start-str);
        }

        // Remap all the abbrs.  Old value is offset into concatenated
        // raw abbr strings.  New value is index into vector of
        // strings.  E.g., 0,5,10,14 => 0,1,2,3.

        // Keep track of which abbreviations get used.
        vector<bool> abbrseen(abbroffset.size(), false);

        for (vector<ZoneType>::iterator it=info.types.begin();
             it!=info.types.end();
             ++it) {
            vector<int>::const_iterator x=
                find(abbroffset.begin(), abbroffset.end(), it->abbr);
            if (x==abbroffset.end()) {
                // TODO: Modify code to add a new string to the end of
                // the abbr list when a middle offset is given, e.g.,
                // "abc*def*" where * == '\0', take offset of 1 and
                // make the array "abc", "def", "bc", and translate 1
                // => 2.  NOT CRITICAL since we don't even use the
                // abbr at this time.
#if 0                
                // TODO: Re-enable this warning if we start using
                // the Olson abbr data, or if the above TODO is completed.
                ostringstream os;
                os << "Warning: unusual abbr offset " << it->abbr
                   << ", expected one of";
                for (vector<int>::const_iterator y=abbroffset.begin();
                     y!=abbroffset.end(); ++y) {
                    os << ' ' << *y;
                }
                cerr << os.str() << "; using 0" << endl;
#endif
                it->abbr = 0;
            } else {
                int index = x - abbroffset.begin();
                it->abbr = index;
                abbrseen[index] = true;
            }
        }

        for (int ii=0;ii<(int) abbrseen.size();++ii) {
            if (!abbrseen[ii]) {
                cerr << "Warning: unused abbreviation: " << ii << endl;
            }
        }
    }

    // Read leap second info, if any.
    // *** We discard leap second data. ***
    for (i=0; i<leapcnt; ++i) {
        readcoded(file); // transition time
        readcoded(file); // total correction after above
    }

    // Read isstd flags
    for (i=0; i<typecnt; ++i) info.types[i].isstd = readbool(file);

    // Read isgmt flags
    for (i=0; i<typecnt; ++i) info.types[i].isgmt = readbool(file);
}

//--------------------------------------------------------------------
// Directory and file reading
//--------------------------------------------------------------------

/**
 * Process a single zoneinfo file, adding the data to ZONEINFO
 * @param path the full path to the file, e.g., ".\zoneinfo\America\Los_Angeles"
 * @param id the zone ID, e.g., "America/Los_Angeles"
 */
void handleFile(string path, string id) {
    // Check for duplicate id
    if (ZONEINFO.find(id) != ZONEINFO.end()) {
        ostringstream os;
        os << "duplicate zone ID: " << id;
        throw invalid_argument(os.str());
    }

    ifstream file(path.c_str(), ios::in | ios::binary);
    if (!file) {
        throw invalid_argument("can't open file");
    }
    ZoneInfo info;
    readzoneinfo(file, info);

    // Check for errors
    if (!file) {
        throw invalid_argument("read error");
    }

    // Check eof-relative pos (there may be a cleaner way to do this)
    long eofPos = (long) file.tellg();
    file.seekg(0, ios::end);
    eofPos =  eofPos - (long) file.tellg();
    if (eofPos) {
        ostringstream os;
        os << (-eofPos) << " unprocessed bytes at end";
        throw invalid_argument(os.str());
    }

    ZONEINFO[id] = info;
}

/**
 * Recursively scan the given directory, calling handleFile() for each
 * file in the tree.  The user should call with the root directory and
 * a prefix of "".  The function will call itself with non-empty
 * prefix values.
 */
#ifdef WIN32

void scandir(string dirname, string prefix="") {
    HANDLE          hList;
    WIN32_FIND_DATA FileData;
    
    // Get the first file
    hList = FindFirstFile((dirname + "\\*").c_str(), &FileData);
    if (hList == INVALID_HANDLE_VALUE) {
        cerr << "Error: Invalid directory: " << dirname << endl;
        exit(1);
    }
    for (;;) {
        string name(FileData.cFileName);
        string path(dirname + "\\" + name);
        if (FileData.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY) {
            if (name != "." && name != "..") {
                scandir(path, prefix + name + "/");
            }
        } else {
            try {
                string id = prefix + name;
                handleFile(path, id);
            } catch (const exception& e) {
                cerr << "Error: While processing \"" << path << "\", "
                     << e.what() << endl;
                exit(1);
            }
        }
        
        if (!FindNextFile(hList, &FileData)) {
            if (GetLastError() == ERROR_NO_MORE_FILES) {
                break;
            } // else...?
        }
    }
    FindClose(hList);
}

#else

void scandir(string dir, string prefix="") {
    DIR *dp;
    struct dirent *dir_entry;
    struct stat stat_info;
    char pwd[512];
    vector<string> subdirs;
    vector<string> subfiles;

    if ((dp = opendir(dir.c_str())) == NULL) {
        cerr << "Error: Invalid directory: " << dir << endl;
        exit(1);
    }
    if (!getcwd(pwd, sizeof(pwd))) {
        cerr << "Error: Directory name too long" << endl;
        exit(1);
    }
    chdir(dir.c_str());
    while ((dir_entry = readdir(dp)) != NULL) {
        string name = dir_entry->d_name;
        string path = dir + "/" + name;
        lstat(dir_entry->d_name,&stat_info);
        if (S_ISDIR(stat_info.st_mode)) {
            if (name != "." && name != "..") {
                subdirs.push_back(path);
                subdirs.push_back(prefix + name + "/");
                // scandir(path, prefix + name + "/");
            }
        } else {
            try {
                string id = prefix + name;
                subfiles.push_back(path);
                subfiles.push_back(id);
                // handleFile(path, id);
            } catch (const exception& e) {
                cerr << "Error: While processing \"" << path << "\", "
                     << e.what() << endl;
                exit(1);
            }
        }
    }
    closedir(dp);
    chdir(pwd);

    for(int i=0;i<(int)subfiles.size();i+=2) {
        try {
            handleFile(subfiles[i], subfiles[i+1]);
        } catch (const exception& e) {
            cerr << "Error: While processing \"" << subfiles[i] << "\", "
                 << e.what() << endl;
            exit(1);
        }
    }
    for(int i=0;i<(int)subdirs.size();i+=2) {
        scandir(subdirs[i], subdirs[i+1]);
    }
}

#endif

//--------------------------------------------------------------------
// Final zone and rule info
//--------------------------------------------------------------------

/**
 * Read and discard the current line.
 */
void consumeLine(istream& in) {
    int c;
    do {
        c = in.get();
    } while (c != EOF && c != '\n');
}

enum {
    DOM = 0,
    DOWGEQ = 1,
    DOWLEQ = 2
};

const char* TIME_MODE[] = {"w", "s", "u"};

const int MONTH_LEN[] = {31,28,31,30,31,30,31,31,30,31,30,31};

const int HOUR = 3600;

struct FinalZone {
    int offset; // raw offset
    int year; // takes effect for y >= year
    string ruleid;
    set<string> aliases;
    FinalZone(int _offset, int _year, const string& _ruleid) :
        offset(_offset), year(_year), ruleid(_ruleid)  {
        if (offset <= -16*HOUR || offset >= 16*HOUR ||
            year < 1900 || year >= 2050) {
            throw invalid_argument("Invalid input arguments");
        }
    }
    FinalZone() : offset(-1), year(-1) {}
    void addLink(const string& alias) {
        if (aliases.find(alias) != aliases.end()) {
            throw invalid_argument("Duplicate alias");
        }
        aliases.insert(alias);
    }
};

struct FinalRulePart {
    int mode;
    int month;
    int dom;
    int dow;
    int time;
    int offset; // dst offset, usually either 0 or 1:00

    // Isstd and isgmt only have 3 valid states, corresponding to local
    // wall time, local standard time, and GMT standard time.
    // Here is how the isstd & isgmt flags are set by zic:
    //| case 's':       /* Standard */
    //|         rp->r_todisstd = TRUE;
    //|         rp->r_todisgmt = FALSE;
    //| case 'w':       /* Wall */
    //|         rp->r_todisstd = FALSE;
    //|         rp->r_todisgmt = FALSE;
    //| case 'g':       /* Greenwich */
    //| case 'u':       /* Universal */
    //| case 'z':       /* Zulu */
    //|         rp->r_todisstd = TRUE;
    //|         rp->r_todisgmt = TRUE;
    bool isstd;
    bool isgmt;

    bool isset; // used during building; later ignored

    FinalRulePart() : isset(false) {}
    void set(const string& _mode,
             int _month,
             int _dom,
             int _dow,
             int _time,
             bool _isstd,
             bool _isgmt,
             int _offset) {
        if (isset) {
            throw invalid_argument("FinalRulePart set twice");
        }
        isset = true;
        if (_mode == "DOWLEQ") {
            mode = DOWLEQ;
        } else if (_mode == "DOWGEQ") {
            mode = DOWGEQ;
        } else if (_mode == "DOM") {
            mode = DOM;
        } else {
            throw invalid_argument("Unrecognized FinalRulePart mode");
        }
        month = _month;
        dom = _dom;
        dow = _dow;
        time = _time;
        isstd = _isstd;
        isgmt = _isgmt;
        offset = _offset;
        if (month < 0 || month >= 12 || dom < 1 || dom > MONTH_LEN[month] ||
            (mode != DOM && (dow < 0 || dow >= 7)) ||
            offset < 0 || offset > HOUR ||
            (isgmt && !isstd)) {
            throw invalid_argument("Invalid input arguments");
        }
    }

    /**
     * Return the time mode as an ICU SimpleTimeZone int from 0..2;
     * see simpletz.h.
     */
    int timemode() const {
        if (isgmt) {
            assert(isstd);
            return 2; // gmt standard
        }
        if (isstd) {
            return 1; // local standard
        }
        return 0; // local wall
    }

    // The SimpleTimeZone encoding method for rules is as follows:
    //          stz_dowim  stz_dow
    // DOM:     dom        0
    // DOWGEQ:  dom        -(dow+1)
    // DOWLEQ:  -dom       -(dow+1)
    // E.g., to encode Mon>=7, use stz_dowim=7, stz_dow=-2
    //       to encode Mon<=7, use stz_dowim=-7, stz_dow=-2
    //       to encode 7, use stz_dowim=7, stz_dow=0
    // Note that for this program and for SimpleTimeZone, 0==Jan,
    // but for this program 0==Sun while for SimpleTimeZone 1==Sun.

    /**
     * Return a "dowim" param suitable for SimpleTimeZone.
     */
    int stz_dowim() const {
        return (mode == DOWLEQ) ? -dom : dom;
    }

    /**
     * Return a "dow" param suitable for SimpleTimeZone.
     */
    int stz_dow() const {
        return (mode == DOM) ? 0 : -(dow+1);
    }
};

struct FinalRule {
    FinalRulePart part[2];

    bool isset() const {
        return part[0].isset && part[1].isset;
    }

    void print(ostream& os) const;
};

map<string,FinalZone> finalZones;
map<string,FinalRule> finalRules;

map<string, set<string> > links;
map<string, string> reverseLinks;
map<string, string> linkSource; // id => "Olson link" or "ICU alias"

/**
 * Predicate used to find FinalRule objects that do not have both
 * sub-parts set (indicating an error in the input file).
 */
bool isNotSet(const pair<const string,FinalRule>& p) {
    return !p.second.isset();
}

/**
 * Predicate used to find FinalZone objects that do not map to a known
 * rule (indicating an error in the input file).
 */
bool mapsToUnknownRule(const pair<const string,FinalZone>& p) {
    return finalRules.find(p.second.ruleid) == finalRules.end();
}

/**
 * This set is used to make sure each rule in finalRules is used at
 * least once.  First we populate it with all the rules from
 * finalRules; then we remove all the rules referred to in
 * finaleZones.
 */
set<string> ruleIDset;

void insertRuleID(const pair<string,FinalRule>& p) {
    ruleIDset.insert(p.first);
}

void eraseRuleID(const pair<string,FinalZone>& p) {
    ruleIDset.erase(p.second.ruleid);
}

/**
 * Populate finalZones and finalRules from the given istream.
 */
void readFinalZonesAndRules(istream& in) {

    for (;;) {
        string token;
        in >> token;
        if (in.eof() || !in) {
            break;
        } else if (token == "zone") {
            // zone Africa/Cairo 7200 1995 Egypt # zone Africa/Cairo, offset 7200, year >= 1995, rule Egypt (0)
            string id, ruleid;
            int offset, year;
            in >> id >> offset >> year >> ruleid;
            consumeLine(in);
            finalZones[id] = FinalZone(offset, year, ruleid);
        } else if (token == "rule") {
            // rule US DOWGEQ 3 1 0 7200 0 0 3600 # 52: US, file data/northamerica, line 119, mode DOWGEQ, April, dom 1, Sunday, time 7200, isstd 0, isgmt 0, offset 3600
            // rule US DOWLEQ 9 31 0 7200 0 0 0 # 53: US, file data/northamerica, line 114, mode DOWLEQ, October, dom 31, Sunday, time 7200, isstd 0, isgmt 0, offset 0
            string id, mode;
            int month, dom, dow, time, offset;
            bool isstd, isgmt;
            in >> id >> mode >> month >> dom >> dow >> time >> isstd >> isgmt >> offset;
            consumeLine(in);
            FinalRule& fr = finalRules[id];
            int p = fr.part[0].isset ? 1 : 0;
            fr.part[p].set(mode, month, dom, dow, time, isstd, isgmt, offset);
        } else if (token == "link") {
            string fromid, toid; // fromid == "real" zone, toid == alias
            in >> fromid >> toid;
            // DO NOT consumeLine(in);
            if (finalZones.find(toid) != finalZones.end()) {
                throw invalid_argument("Bad link: `to' id is a \"real\" zone");
            }

            links[fromid].insert(toid);
            reverseLinks[toid] = fromid;

            linkSource[fromid] = "Olson link";
            linkSource[toid] = "Olson link";
        } else if (token.length() > 0 && token[0] == '#') {
            consumeLine(in);
        } else {
            throw invalid_argument("Unrecognized keyword");
        }
    }

    if (!in.eof() && !in) {
        throw invalid_argument("Parse failure");
    }

    // Perform validity check: Each rule should have data for 2 parts.
    if (count_if(finalRules.begin(), finalRules.end(), isNotSet) != 0) {
        throw invalid_argument("One or more incomplete rule pairs");
    }

    // Perform validity check: Each zone should map to a known rule.
    if (count_if(finalZones.begin(), finalZones.end(), mapsToUnknownRule) != 0) {
        throw invalid_argument("One or more zones refers to an unknown rule");
    }

    // Perform validity check: Each rule should be referred to by a zone.
    ruleIDset.clear();
    for_each(finalRules.begin(), finalRules.end(), insertRuleID);
    for_each(finalZones.begin(), finalZones.end(), eraseRuleID);
    if (ruleIDset.size() != 0) {
        throw invalid_argument("Unused rules");
    }
}

//--------------------------------------------------------------------
// Resource bundle output
//--------------------------------------------------------------------

// SEE olsontz.h FOR RESOURCE BUNDLE DATA LAYOUT

void ZoneInfo::print(ostream& os, const string& id) const {
    // Implement compressed format #2:

  os << "  /* " << id << " */ ";

    if (aliasTo >= 0) {
        assert(aliases.size() == 0);
        os << ":int { " << aliasTo << " } "; // No endl - save room for comment.
        return;
    }

    os << ":array {" << endl;

    vector<Transition>::const_iterator trn;
    vector<ZoneType>::const_iterator typ;

    bool first=true;
    os << "    :intvector { ";
    for (trn = transitions.begin(); trn != transitions.end(); ++trn) {
        if (!first) os << ", ";
        first = false;
        os << trn->time;
    }
    os << " }" << endl;

    first=true;
    os << "    :intvector { ";
    for (typ = types.begin(); typ != types.end(); ++typ) {
        if (!first) os << ", ";
        first = false;
        os << typ->rawoffset << ", " << typ->dstoffset;
    }
    os << " }" << endl;

    os << "    :bin { \"" << hex << setfill('0');
    for (trn = transitions.begin(); trn != transitions.end(); ++trn) {
        os << setw(2) << trn->type;
    }
    os << dec << "\" }" << endl;

    // Final zone info, if any
    if (finalYear != -1) {
        os << "    \"" << finalRuleID << "\"" << endl;
        os << "    :intvector { " << finalOffset << ", "
           << finalYear << " }" << endl;
    }

    // Alias list, if any
    if (aliases.size() != 0) {
        first = true;
        os << "    :intvector { ";
        for (set<int>::const_iterator i=aliases.begin(); i!=aliases.end(); ++i) {
            if (!first) os << ", ";
            first = false;
            os << *i;
        }
        os << " }" << endl;
    }

    os << "  } "; // no trailing 'endl', so comments can be placed.
}

inline ostream&
operator<<(ostream& os, const ZoneMap& zoneinfo) {
    int c = 0;
    for (ZoneMapIter it = zoneinfo.begin();
         it != zoneinfo.end();
         ++it) {
        if(c)  os << ",";
        it->second.print(os, it->first);
        os << "//Z#" << c++ << endl;
    }
    return os;
}

// print the string list 
ostream& printStringList( ostream& os, const ZoneMap& zoneinfo) {
  int n = 0; // count
  int col = 0; // column
  os << " Names {" << endl
     << "    ";
  for (ZoneMapIter it = zoneinfo.begin();
       it != zoneinfo.end();
       ++it) {
    if(n) {
      os << ",";
      col ++;
    }
    const string& id = it->first;
    os << "\"" << id << "\"";
    col += id.length() + 2;
    if(col >= 50) {
      os << " // " << n << endl
         << "    ";
      col = 0;
    }
    n++;
  }
  os << " // " << (n-1) << endl
     << " }" << endl;

  return os;
}

//--------------------------------------------------------------------
// main
//--------------------------------------------------------------------

// Unary predicate for finding transitions after a given time
bool isAfter(const Transition t, long thresh) {
    return t.time >= thresh;
}

/**
 * A zone type that contains only the raw and dst offset.  Used by the
 * optimizeTypeList() method.
 */
struct SimplifiedZoneType {
    long rawoffset;
    long dstoffset;
    SimplifiedZoneType() : rawoffset(-1), dstoffset(-1) {}
    SimplifiedZoneType(const ZoneType& t) : rawoffset(t.rawoffset),
                                            dstoffset(t.dstoffset) {}
    bool operator<(const SimplifiedZoneType& t) const {
        return rawoffset < t.rawoffset ||
            (rawoffset == t.rawoffset &&
             dstoffset < t.dstoffset);
    }
};

/**
 * Construct a ZoneType from a SimplifiedZoneType.  Note that this
 * discards information; the new ZoneType will have meaningless
 * (empty) abbr, isdst, isstd, and isgmt flags; this is appropriate,
 * since ignoring these is how we do optimization (we have no use for
 * these in historical transitions).
 */
ZoneType::ZoneType(const SimplifiedZoneType& t) :
    rawoffset(t.rawoffset), dstoffset(t.dstoffset),
    abbr(-1), isdst(false), isstd(false), isgmt(false) {}

/**
 * Optimize the type list to remove excess entries.  The type list may
 * contain entries that are distinct only in terms of their dst, std,
 * or gmt flags.  Since we don't care about those flags, we can reduce
 * the type list to a set of unique raw/dst offset pairs, and remap
 * the type indices in the transition list, which stores, for each
 * transition, a transition time and a type index.
 */
void ZoneInfo::optimizeTypeList() {
    // Assemble set of unique types; only those in the `transitions'
    // list, since there may be unused types in the `types' list
    // corresponding to transitions that have been trimmed (during
    // merging of final data).

    if (aliasTo >= 0) return; // Nothing to do for aliases

    // If there are zero transitions and one type, then leave that as-is.
    if (transitions.size() == 0) {
        if (types.size() != 1) {
            cerr << "Error: transition count = 0, type count = " << types.size() << endl;
        }
        return;
    }

    set<SimplifiedZoneType> simpleset;
    for (vector<Transition>::const_iterator i=transitions.begin();
         i!=transitions.end(); ++i) {
        assert(i->type < (int)types.size());
        simpleset.insert(types[i->type]);
    }

    // Map types to integer indices
    map<SimplifiedZoneType,int> simplemap;
    int n=0;
    for (set<SimplifiedZoneType>::const_iterator i=simpleset.begin();
         i!=simpleset.end(); ++i) {
        simplemap[*i] = n++;
    }

    // Remap transitions
    for (vector<Transition>::iterator i=transitions.begin();
         i!=transitions.end(); ++i) {
        assert(i->type < (int)types.size());
        ZoneType oldtype = types[i->type];
        SimplifiedZoneType newtype(oldtype);
        assert(simplemap.find(newtype) != simplemap.end());
        i->type = simplemap[newtype];
    }

    // Replace type list
    types.clear();
    copy(simpleset.begin(), simpleset.end(), back_inserter(types));
}

/**
 * Merge final zone data into this zone.
 */
void ZoneInfo::mergeFinalData(const FinalZone& fz) {
    int year = fz.year;
    long seconds = yearToSeconds(year);
    vector<Transition>::iterator it =
        find_if(transitions.begin(), transitions.end(),
                bind2nd(ptr_fun(isAfter), seconds));
    transitions.erase(it, transitions.end());

    if (finalYear != -1) {
        throw invalid_argument("Final zone already merged in");
    }
    finalYear = fz.year;
    finalOffset = fz.offset;
    finalRuleID = fz.ruleid;
}

/**
 * Merge the data from the given final zone into the core zone data by
 * calling the ZoneInfo member function mergeFinalData.
 */
void mergeOne(const string& zoneid, const FinalZone& fz) {
    if (ZONEINFO.find(zoneid) == ZONEINFO.end()) {
        throw invalid_argument("Unrecognized final zone ID");
    }
    ZONEINFO[zoneid].mergeFinalData(fz);
}

/**
 * Visitor function that merges the final zone data into the main zone
 * data structures.  It calls mergeOne for each final zone and its
 * list of aliases.
 */
void mergeFinalZone(const pair<string,FinalZone>& p) {
    const string& id = p.first;
    const FinalZone& fz = p.second;

    mergeOne(id, fz);
}

/**
 * Print this rule in resource bundle format to os.  ID and enclosing
 * braces handled elsewhere.
 */
void FinalRule::print(ostream& os) const {
    // First print the rule part that enters DST; then the rule part
    // that exits it.
    int whichpart = (part[0].offset != 0) ? 0 : 1;
    assert(part[whichpart].offset != 0);
    assert(part[1-whichpart].offset == 0);

    os << "    ";
    for (int i=0; i<2; ++i) {
        const FinalRulePart& p = part[whichpart];
        whichpart = 1-whichpart;
        os << p.month << ", " << p.stz_dowim() << ", " << p.stz_dow() << ", "
           << p.time << ", " << p.timemode() << ", ";
    }
    os << part[whichpart].offset << endl;
}

int main(int argc, char *argv[]) {
    string rootpath, zonetab, version;

    if (argc != 4) {
        cout << "Usage: tz2icu <dir> <cmap> <vers>" << endl
             << " <dir>   path to zoneinfo file tree generated by" << endl
             << "         ICU-patched version of zic" << endl
             << " <cmap>  country map, from tzdata archive," << endl
             << "         typically named \"zone.tab\"" << endl
             << " <vers>  version string, such as \"2003e\"" << endl;
        exit(1);
    } else {
        rootpath = argv[1];
        zonetab = argv[2];
        version = argv[3];
    }

    cout << "Olson data version: " << version << endl;

    try {
        ifstream finals(ICU_ZONE_FILE);
        if (finals) {
            readFinalZonesAndRules(finals);

            cout << "Finished reading " << finalZones.size()
                 << " final zones and " << finalRules.size()
                 << " final rules from " ICU_ZONE_FILE << endl;
        } else {
            cerr << "Error: Unable to open " ICU_ZONE_FILE << endl;
            return 1;
        }
    } catch (const exception& error) {
        cerr << "Error: While reading " ICU_ZONE_FILE ": " << error.what() << endl;
        return 1;
    }

    // Read the legacy alias list and process it.  Treat the legacy mappings
    // like links, but also record them in the "legacy" hash.
    try {
        ifstream aliases(ICU_TZ_ALIAS);
        if (!aliases) {
            cerr << "Error: Unable to open " ICU_TZ_ALIAS << endl;
            return 1;
        }
        int n = 0;
        string line;
        while (getline(aliases, line)) {
            string::size_type lb = line.find('#');
            if (lb != string::npos) {
                line.resize(lb); // trim comments
            }
            vector<string> a;
            istringstream is(line);
            copy(istream_iterator<string>(is),istream_iterator<string>(),
                 back_inserter(a));
            if (a.size() == 0) continue; // blank line
            if (a.size() != 2) {
                cerr << "Error: Can't parse \"" << line << "\" in "
                    ICU_TZ_ALIAS << endl;
                exit(1);
            }
            ++n;
            
            string alias(a[0]), olson(a[1]);
            if (links.find(alias) != links.end()) {
                cerr << "Error: Alias \"" << alias
                     << "\" is an Olson zone in "
                     ICU_TZ_ALIAS << endl;
                return 1;
            }
            if (reverseLinks.find(alias) != reverseLinks.end()) {
                cerr << "Error: Alias \"" << alias
                     << "\" is an Olson link to \"" << reverseLinks[olson]
                     << "\" in " << ICU_TZ_ALIAS << endl;
                return 1;
            }

            // Record source for error reporting
            if (linkSource.find(olson) == linkSource.end()) {
                linkSource[olson] = "ICU alias";
            }
            assert(linkSource.find(alias) == linkSource.end());
            linkSource[alias] = "ICU alias";
            
            links[olson].insert(alias);
            reverseLinks[alias] = olson;
        }
        cout << "Finished reading " << n
             << " aliases from " ICU_TZ_ALIAS << endl;
    } catch (const exception& error) {
        cerr << "Error: While reading " ICU_TZ_ALIAS ": " << error.what() << endl;
        return 1;
    }

    try {
        // Recursively scan all files below the given path, accumulating
        // their data into ZONEINFO.  All files must be TZif files.  Any
        // failure along the way will result in a call to exit(1).
        scandir(rootpath);
    } catch (const exception& error) {
        cerr << "Error: While scanning " << rootpath << ": " << error.what() << endl;
        return 1;
    }

    cout << "Finished reading " << ZONEINFO.size() << " zoneinfo files ["
         << (ZONEINFO.begin())->first << ".."
         << (--ZONEINFO.end())->first << "]" << endl;

    try {
        for_each(finalZones.begin(), finalZones.end(), mergeFinalZone);
    } catch (const exception& error) {
        cerr << "Error: While merging final zone data: " << error.what() << endl;
        return 1;
    }

    // Process links (including ICU aliases).  For each link set we have
    // a canonical ID (e.g., America/Los_Angeles) and a set of one or more
    // aliases (e.g., PST, PST8PDT, ...).
    
    // 1. Add all aliases as zone objects in ZONEINFO
    for (map<string,set<string> >::const_iterator i = links.begin();
         i!=links.end(); ++i) {
        const string& olson = i->first;
        const set<string>& aliases = i->second;
        if (ZONEINFO.find(olson) == ZONEINFO.end()) {
            cerr << "Error: Invalid " << linkSource[olson] << " to non-existent \""
                 << olson << "\"" << endl;
            return 1;
        }
        for (set<string>::const_iterator j=aliases.begin();
             j!=aliases.end(); ++j) {
            ZONEINFO[*j] = ZoneInfo();
        }
    }
 
    // 2. Create a mapping from zones to index numbers 0..n-1.
    map<string,int> zoneIDs;
    vector<string> zoneIDlist;
    int z=0;
    for (ZoneMap::iterator i=ZONEINFO.begin(); i!=ZONEINFO.end(); ++i) {
        zoneIDs[i->first] = z++;
        zoneIDlist.push_back(i->first);
    }
    assert(z == (int) ZONEINFO.size());

    // 3. Merge aliases.  Sometimes aliases link to other aliases; we
    // resolve these into simplest possible sets.
    map<string,set<string> > links2;
    map<string,string> reverse2;
    for (map<string,set<string> >::const_iterator i = links.begin();
         i!=links.end(); ++i) {
        string olson = i->first;
        while (reverseLinks.find(olson) != reverseLinks.end()) {
            olson = reverseLinks[olson];
        }
        for (set<string>::const_iterator j=i->second.begin(); j!=i->second.end(); ++j) {
            links2[olson].insert(*j);
            reverse2[*j] = olson;
        }
    }
    links = links2;
    reverseLinks = reverse2;

    if (false) { // Debugging: Emit link map
        for (map<string,set<string> >::const_iterator i = links.begin();
             i!=links.end(); ++i) {
            cout << i->first << ": ";
            for (set<string>::const_iterator j=i->second.begin(); j!=i->second.end(); ++j) {
                cout << *j << ", ";
            }
            cout << endl;
        }
    }

    // 4. Update aliases
    for (map<string,set<string> >::const_iterator i = links.begin();
         i!=links.end(); ++i) {
        const string& olson = i->first;
        const set<string>& aliases = i->second;
        ZONEINFO[olson].clearAliases();
        ZONEINFO[olson].addAlias(zoneIDs[olson]);
        for (set<string>::const_iterator j=aliases.begin();
             j!=aliases.end(); ++j) {
            assert(zoneIDs.find(olson) != zoneIDs.end());
            assert(zoneIDs.find(*j) != zoneIDs.end());
            assert(ZONEINFO.find(*j) != ZONEINFO.end());
            ZONEINFO[*j].setAliasTo(zoneIDs[olson]);
            ZONEINFO[olson].addAlias(zoneIDs[*j]);
        }
    }

    // Once merging of final data is complete, we can optimize the type list
    for (ZoneMap::iterator i=ZONEINFO.begin(); i!=ZONEINFO.end(); ++i) {
        i->second.optimizeTypeList();
    }

    // Create the country map
    map<string, set<string> > countryMap;  // country -> set of zones
    map<string, string> reverseCountryMap; // zone -> country
    try {
        ifstream f(zonetab.c_str());
        if (!f) {
            cerr << "Error: Unable to open " << zonetab << endl;
            return 1;
        }
        int n = 0;
        string line;
        while (getline(f, line)) {
            string::size_type lb = line.find('#');
            if (lb != string::npos) {
                line.resize(lb); // trim comments
            }
            string country, coord, zone;
            istringstream is(line);
            is >> country >> coord >> zone;
            if (country.size() == 0) continue;
            if (country.size() != 2 || zone.size() < 1) {
                cerr << "Error: Can't parse " << line << " in " << zonetab << endl;
                return 1;
            }
            if (ZONEINFO.find(zone) == ZONEINFO.end()) {
                cerr << "Error: Country maps to invalid zone " << zone
                     << " in " << zonetab << endl;
                return 1;
            }
            countryMap[country].insert(zone);
            reverseCountryMap[zone] = country;
            //cerr << (n+1) << ": " << country << " <=> " << zone << endl;
            ++n;
        }
        cout << "Finished reading " << n
             << " country entries from " << zonetab << endl;
    } catch (const exception& error) {
        cerr << "Error: While reading " << zonetab << ": " << error.what() << endl;
        return 1;
    }

    // Merge ICU aliases into country map.  Don't merge any alias
    // that already has a country map, since that doesn't make sense.
    // E.g.  "Link Europe/Oslo Arctic/Longyearbyen" doesn't mean we
    // should cross-map the countries between these two zones.
    for (map<string,set<string> >::const_iterator i = links.begin();
         i!=links.end(); ++i) {
        const string& olson(i->first);
        if (reverseCountryMap.find(olson) == reverseCountryMap.end()) {
            continue;
        }
        string c = reverseCountryMap[olson];
        const set<string>& aliases(i->second);
        for (set<string>::const_iterator j=aliases.begin();
             j != aliases.end(); ++j) {
            if (reverseCountryMap.find(*j) == reverseCountryMap.end()) {
                countryMap[c].insert(*j);
                reverseCountryMap[*j] = c;
                //cerr << "Aliased country: " << c << " <=> " << *j << endl;
            }
        }
    }

    // Create a pseudo-country containing all zones belonging to no country
    set<string> nocountry;
    for (ZoneMap::iterator i=ZONEINFO.begin(); i!=ZONEINFO.end(); ++i) {
        if (reverseCountryMap.find(i->first) == reverseCountryMap.end()) {
            nocountry.insert(i->first);
        }
    }
    countryMap[""] = nocountry;

    // Get local time & year for below
    time_t sec;
    time(&sec);
    struct tm* now = localtime(&sec);
    int thisYear = now->tm_year + 1900;

    // Write out a resource-bundle source file containing data for
    // all zones.
    ofstream file(ICU_TZ_RESOURCE ".txt");
    if (file) {
        file << "//---------------------------------------------------------" << endl
             << "// Copyright (C) 2003";
        if (thisYear > 2003) {
            file << "-" << thisYear;
        }
        file << ", International Business Machines" << endl
             << "// Corporation and others.  All Rights Reserved." << endl
             << "//---------------------------------------------------------" << endl
             << "// Build tool: tz2icu" << endl
             << "// Build date: " << asctime(now) /* << endl -- asctime emits CR */
             << "// Olson source: ftp://elsie.nci.nih.gov/pub/" << endl
             << "// Olson version: " << version << endl
             << "//---------------------------------------------------------" << endl
             << "// >> !!! >>   THIS IS A MACHINE-GENERATED FILE   << !!! <<" << endl
             << "// >> !!! >>>            DO NOT EDIT             <<< !!! <<" << endl
             << "//---------------------------------------------------------" << endl
             << endl
             << ICU_TZ_RESOURCE " {" << endl
             << " Zones:array { " << endl
             << ZONEINFO // Zones (the actual data)
             << " }" << endl;

        // Names correspond to the Zones list, used for binary searching.
        printStringList ( file, ZONEINFO ); // print the Names list

        // Final Rules are used if requested by the zone
        file << " Rules { " << endl;
        // Emit final rules
        int frc = 0;
        for(map<string,FinalRule>::iterator i=finalRules.begin();
            i!=finalRules.end(); ++i) {
            const string& id = i->first;
            const FinalRule& r = i->second;
            file << "  " << id << ":intvector {" << endl;
            r.print(file);
            file << "  } //_#" << frc++ << endl;
        }
        file << " }" << endl;

        // Emit country (region) map.  Emitting the string zone IDs results
        // in a 188 kb binary resource; emitting the zone index numbers
        // trims this to 171 kb.  More work for the runtime code, but
        // a smaller data footprint.
        file << " Regions { " << endl;
        int  rc = 0;
        for (map<string, set<string> >::const_iterator i=countryMap.begin();
             i != countryMap.end(); ++i) {
            string country = i->first;
            const set<string>& zones(i->second);
            file << "  ";
            if(country[0]==0) {
              file << "Default";
            }
            file << country << ":intvector { ";
            bool first = true;
            for (set<string>::const_iterator j=zones.begin();
                 j != zones.end(); ++j) {
                if (!first) file << ", ";
                first = false;
                if (zoneIDs.find(*j) == zoneIDs.end()) {
                    cerr << "Error: Nonexistent zone in country map: " << *j << endl;
                    return 1;
                }
                file << zoneIDs[*j]; // emit the zone's index number
            }
            file << " } //R#" << rc++ << endl;
        }
        file << " }" << endl;

        file << "}" << endl;
    }

    file.close();
     
    if (file) { // recheck error bit
        cout << "Finished writing " ICU_TZ_RESOURCE ".txt" << endl;
    } else {
        cerr << "Error: Unable to open/write to " ICU_TZ_RESOURCE ".txt" << endl;
        return 1;
    }

#define ICU4J_TZ_CLASS "ZoneMetaData"

    // Write out a Java source file containing only a few pieces of
    // meta-data missing from the core JDK: the equivalency lists and
    // the country map.
    ofstream java(ICU4J_TZ_CLASS ".java");
    if (java) {
        java << "//---------------------------------------------------------" << endl
             << "// Copyright (C) 2003";
        if (thisYear > 2003) {
            java << "-" << thisYear;
        }
        java << ", International Business Machines" << endl
             << "// Corporation and others.  All Rights Reserved." << endl
             << "//---------------------------------------------------------" << endl
             << "// Build tool: tz2icu" << endl
             << "// Build date: " << asctime(now) /* << endl -- asctime emits CR */
             << "// Olson source: ftp://elsie.nci.nih.gov/pub/" << endl
             << "// Olson version: " << version << endl
             << "//---------------------------------------------------------" << endl
             << "// >> !!! >>   THIS IS A MACHINE-GENERATED FILE   << !!! <<" << endl
             << "// >> !!! >>>            DO NOT EDIT             <<< !!! <<" << endl
             << "//---------------------------------------------------------" << endl
             << endl
             << "package com.ibm.icu.impl;" << endl
             << endl
             << "public final class " ICU4J_TZ_CLASS " {" << endl;

        // Emit equivalency lists
        bool first1 = true;
        java << "  public static final String[][] EQUIV = {" << endl;
        for (ZoneMap::const_iterator i=ZONEINFO.begin(); i!=ZONEINFO.end(); ++i) {
            if (i->second.isAlias() || i->second.getAliases().size() == 0) {
                continue;
            }
            if (!first1) java << "," << endl;
            first1 = false;
            // The ID of this zone (the canonical zone, to which the
            // aliases point) will be sorted into the list, so it
            // won't be at position 0.  If we want to know which is
            // the canonical zone, we should move it to position 0.
            java << "    { ";
            bool first2 = true;
            const set<int>& s = i->second.getAliases();
            for (set<int>::const_iterator j=s.begin(); j!=s.end(); ++j) {
                if (!first2) java << ", ";
                java << '"' << zoneIDlist[*j] << '"';
                first2 = false;
            }
            java << " }";
        }
        java << endl
             << "  };" << endl;

        // Emit country map.
        first1 = true;
        java << "  public static final String[][] COUNTRY = {" << endl;
        for (map<string, set<string> >::const_iterator i=countryMap.begin();
             i != countryMap.end(); ++i) {
            if (!first1) java << "," << endl;
            first1 = false;
            string country = i->first;
            const set<string>& zones(i->second);
            java << "    { \"" << country << '"';
            for (set<string>::const_iterator j=zones.begin();
                 j != zones.end(); ++j) {
                java << ", \"" << *j << '"';
            }
            java << " }";
        }
        java << endl
             << "  };" << endl;

        java << "}" << endl;
    }

    java.close();

    if (java) { // recheck error bit
        cout << "Finished writing " ICU4J_TZ_CLASS ".java" << endl;
    } else {
        cerr << "Error: Unable to open/write to " ICU4J_TZ_CLASS ".java" << endl;
        return 1;
    }

    return 0;
}

//eof
