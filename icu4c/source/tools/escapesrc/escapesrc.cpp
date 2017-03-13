// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include <stdio.h>
#include <string>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>

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
      const uint8_t *b = s;
      int32_t i = pos;
      int32_t length = linestr.size();
      UChar32 c;

      if(U8_IS_SINGLE((uint8_t)s[i])) continue; // single code point

      {
        U8_NEXT(s, i, length, c);
      }
      if(c<0) {
        fprintf(stderr, "Illegal utf-8 sequence %04X pos %d\n", c, pos);
        return true;
      }

      size_t seqLen = (i-pos);

      printf("U+%04X pos %d [len %d]\n", c, pos, seqLen);

      if( c <= 0xFFFF) {
        char newSeq[] = "\\uFFFD";
        sprintf(newSeq, "\\u%04X", c);
        linestr.replace(pos, seqLen, newSeq);
        //pos += seqLen; // advance
        pos += strlen(newSeq) - 1;
      } else {
        fprintf(stderr, "%s: Error: not implemented yet: surrogate pairs for U+%04X\n", prog.c_str(), c);
        return true;
      }
    }
  }

  return false;
}

/**
 * false = no err
 * true = had err
 */
bool fixLine(int no, std::string &linestr) {
  const char *line = linestr.c_str();
  size_t len = linestr.size();
  // Quick Check: all ascii?

  if(!hasNonAscii(line, len)) {
    return false; // ASCII
  }

  if(isCommentOrEmpty(line, len)) {
    return false; // Comment or just empty
  }

  if(!strnstr(line, "u'", len) && !strnstr(line, "u\"", len)) {
    return false; // Nothing to do. No u' or u" detected
  }

  // start from the end and find all u" cases
  size_t pos = len = linestr.size();
  while((pos = linestr.rfind("u\"", pos)) != std::string::npos) {
    printf("found doublequote at %d\n", pos);
    if(fixAt(linestr, pos)) return true;
    pos--;
  }

  // reset and find all u' cases
  pos = len = linestr.size();
  while((pos = linestr.rfind("u'", pos)) != std::string::npos) {
    printf("found singlequote at %d\n", pos);
    if(fixAt(linestr, pos)) return true;
    pos--;
  }

  fprintf(stderr, "%d - fixed\n", no);
  return false;
}

int convert(const std::string &infile, const std::string &outfile) {
  fprintf(stderr, "%s: %s -> %s\n", prog.c_str(), infile.c_str(), outfile.c_str());

  FILE *inf = fopen(infile.c_str(), "rb");
  if(!inf) {
    fprintf(stderr, "%s: could not open input file %s\n", prog.c_str(), infile.c_str());
    cleanup(outfile);
    return 1;
  }

  FILE *outf = fopen(outfile.c_str(), "w");

  if(!outf) {
    fprintf(stderr, "%s: could not open output file %s\n", prog.c_str(), outfile.c_str());
    fclose(inf);
    return 1;
  }

  // TODO: any platform variations of this?
  fprintf(outf, "#line 1 \"%s\"\n", infile.c_str());

  size_t len;
  char *line;
  int no = 0;
  std::string linestr;
  while((line = fgetln(inf, &len))!= NULL) {
    no++;
    linestr.assign(line, len);
    if(fixLine(no, linestr)) {
      fclose(inf);
      fclose(outf);
      fprintf(stderr, "%s:%d: Fixup failed by %s\n", infile.c_str(), no, prog.c_str());
      cleanup(outfile);
      return 1;
    }
    len = linestr.size(); // size may have changed.
    
    if(fwrite(linestr.c_str(), 1, linestr.size(), outf) != len) {
      fclose(inf);
      fclose(outf);
      fprintf(stderr, "%s: short write to  %s:%d\n", prog.c_str(), outfile.c_str(), no);
      cleanup(outfile);
      return 1;
    }
  }

  fclose(inf);
  fclose(outf);
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
