// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include <stdio.h>
#include <string>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <iostream>
#include <fstream>

// with caution:
#include "unicode/utf8.h"

std::string prog;

void usage() {
  fprintf(stderr, "%s: usage: %s infile.cpp outfile.cpp\n", prog.c_str(), prog.c_str());
}


int cleanup(const std::string &outfile) {
  const char *outstr = outfile.c_str();
  if(outstr && *outstr) {
    int rc = unlink(outstr);
    if(rc == 0) {
      fprintf(stderr, "%s: deleted %s\n", prog.c_str(), outstr);
      return 0;
    } else {
      if( errno == ENOENT ) {
        return 0; // File did not exist - no error.
      } else {
        perror("unlink");
        return 1;
      }
    }
  }
  return 0;
}

inline bool hasNonAscii(const char *line, size_t len) {
  const unsigned char *uline = reinterpret_cast<const unsigned char*>(line);
  for(size_t i=0;i<len; i++) {
    if( uline[i] > 0x7F) {
      return true;
    }
  }
  return false;
}

inline const char *skipws(const char *p, const char *e) {
  for(;p<e;p++) {
    switch(*p) {
    case ' ':
    case '\t':
    case '\n':
    case '\r':
      break;
    default:
      return p; // non ws
    }
  }
  return p;
}

inline bool isCommentOrEmpty(const char* line, size_t len) {
  const char *p = line;
  const char *e = line+len;
  p = skipws(p,e);
  if(p==e) {
    return true; // whitespace only
  }
  p++;
  switch(*p) {
  case '#': return true; // #directive
  case '/':
    p++;
    if(p==e) return false; // single slash
    switch(*p) {
    case '/': // '/ /'
    case '*': // '/ *'
      return true; // start of comment
    default: return false; // something else
    }
  default: return false; // something else
  }
  /*NOTREACHED*/
}

/**
 * fix the string at the position
 * false = no err
 * true = had err
 */
bool fixAt(std::string &linestr, size_t pos) {
  if(linestr[pos] != 'u') {
    fprintf(stderr, "Not a 'u'?");
    return true;
  }

  char quote = linestr[pos+1];

  //printf("u%c…%c\n", quote, quote);


  for(pos += 2; pos < linestr.size(); pos++) {
    if(linestr[pos] == quote) return false; // end of quote
    if(linestr[pos] == '\\') {
      pos++;
      if(linestr[pos] == quote) continue; // quoted quote
      if(linestr[pos] == 'u') continue; // for now ... unicode escape
      if(linestr[pos] == '\\') continue;
      // some other escape… ignore
    } else {
      // Proceed to decode utf-8
      const uint8_t *s = (const uint8_t*) (linestr.c_str());
      int32_t i = pos;
      int32_t length = linestr.size();
      UChar32 c;

      if(U8_IS_SINGLE((uint8_t)s[i])) continue; // single code point

      {
        U8_NEXT(s, i, length, c);
      }
      if(c<0) {
        fprintf(stderr, "Illegal utf-8 sequence\n");
        return true;
      }

      size_t seqLen = (i-pos);

      //printf("U+%04X pos %d [len %d]\n", c, pos, seqLen);

      char newSeq[] = "\\U0000FFFD";
      if( c <= 0xFFFF) {
        sprintf(newSeq, "\\u%04X", c);
      } else {
        sprintf(newSeq, "\\U%08X", c);
      }
      linestr.replace(pos, seqLen, newSeq);
      pos += strlen(newSeq) - 1;
    }
  }

  return false;
}

/**
 * false = no err
 * true = had err
 */
bool fixLine(int /*no*/, std::string &linestr) {
  const char *line = linestr.c_str();
  size_t len = linestr.size();

  // no u' in the line?
  if(!strstr(line, "u'") && !strstr(line, "u\"")) {
    return false; // Nothing to do. No u' or u" detected
  }

  // Quick Check: all ascii?
  if(!hasNonAscii(line, len)) {
    return false; // ASCII
  }

  // comment or empty line?
  if(isCommentOrEmpty(line, len)) {
    return false; // Comment or just empty
  }

  // start from the end and find all u" cases
  size_t pos = len = linestr.size();
  while((pos>0) && (pos = linestr.rfind("u\"", pos)) != std::string::npos) {
    //printf("found doublequote at %d\n", pos);
    if(fixAt(linestr, pos)) return true;
    if(pos == 0) break;
    pos--;
  }

  // reset and find all u' cases
  pos = len = linestr.size();
  while((pos>0) && (pos = linestr.rfind("u'", pos)) != std::string::npos) {
    //printf("found singlequote at %d\n", pos);
    if(fixAt(linestr, pos)) return true;
    if(pos == 0) break;
    pos--;
  }

  //fprintf(stderr, "%d - fixed\n", no);
  return false;
}

int convert(const std::string &infile, const std::string &outfile) {
  fprintf(stderr, "escapesrc: %s -> %s\n", infile.c_str(), outfile.c_str());

  std::ifstream inf;
  
  inf.open(infile.c_str(), std::ios::in);

  if(!inf.is_open()) {
    fprintf(stderr, "%s: could not open input file %s\n", prog.c_str(), infile.c_str());
    cleanup(outfile);
    return 1;
  }

  std::ofstream outf;

  outf.open(outfile.c_str(), std::ios::out);

  if(!outf.is_open()) {
    fprintf(stderr, "%s: could not open output file %s\n", prog.c_str(), outfile.c_str());
    return 1;
  }

  // TODO: any platform variations of #line?
  outf << "#line 1 \"" << infile << "\"" << '\n';

  int no = 0;
  std::string linestr;
  while( getline( inf, linestr)) {
    no++;
    if(fixLine(no, linestr)) {
      outf.close();
      fprintf(stderr, "%s:%d: Fixup failed by %s\n", infile.c_str(), no, prog.c_str());
      cleanup(outfile);
      return 1;
    }
    outf << linestr << '\n';
  }

  return 0;
}

int main(int argc, const char *argv[]) {
  prog = argv[0];

  if(argc != 3) {
    usage();
    return 1;
  }

  std::string infile = argv[1];
  std::string outfile = argv[2];

  return convert(infile, outfile);
}


#include "utf_impl.cpp"
