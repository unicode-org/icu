/*
*******************************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   file name:  genjp.
*   encoding:   US-ASCII
*
* Modification history
* Date        Name      Comments
* 10/13/2001  weiv      created
* 
* The GenJP class is useful for generating various stuff related to Japanese language.
* Right now, it uses ICU to generate rules for JIS X 4061 compliant collation.
* Also, it is useful for getting compatibility versions of the characters.
*/

#include "genjp.h"

int main(int argc, const char* const argv[]) {
  UErrorCode status = U_ZERO_ERROR;
  GenJP jp;
  jp.writeHeader(status);
  jp.processLengthMark(status);
  jp.processIterationMark(status);
  jp.equalKatakanaToHiragana(status);
  jp.processCompatibility(status);
  jp.printOutKanji(status);
  jp.writeFooter(status);
  return status;
}

// Here is the deal
// We need to produce the following things
// 1. &katakana=katakana=hiragana=compatibilityform
// 2. &otherchars=compatibilitychar
// 3. &[before 3]small_katakana_vowel
//     <<<katakanas_ending_in_vowel|\u30fc...=
//       =hiraganas_ending_in_vowel|\u30fc...=
//       =copmatibility_ending_in_vowel|\u30fc
// 4. &[before 3]large_katakana
//     <<<

GenJP::GenJP() {
  UErrorCode status = U_ZERO_ERROR;
  kanaToHalf = ucmpe32_open(0xFFFF, 0, &status);
  nameBuff = (char *)uprv_malloc(_bufferSize*sizeof(char));
  out = stdout;
  const char *nameBuffer;
  UChar decompBuff[_bufferSize];
  uint32_t decompLen = 0;
  UChar ch = 0;
  for(ch = 0; ch < 0xFFFF; ch++) {
    nameBuffer = getName(ch, status);
    if(strstr(nameBuffer, "FULLWIDTH") || strstr(nameBuffer, "HALFWIDTH")) {
      decompLen = unorm_normalize(&ch, 1, UNORM_NFKD, 0, decompBuff, 254, &status);
      ucmpe32_set32(kanaToHalf, decompBuff[0], ch);
    }
  }
}

GenJP::~GenJP() {
  uprv_free(nameBuff);
  ucmpe32_close(kanaToHalf);
}

UChar GenJP::getHalf(UChar u) {
  return (UChar)ucmpe32_get(kanaToHalf, u);
}

char *GenJP::printUnicodeStuff(UChar *zTStuff, char *resBuf) {
  uint32_t resBufLen = 0;
  while(*zTStuff!=0) {
    if(*zTStuff >= 0x20 && *zTStuff <= 0x7F) {
      resBuf[resBufLen++] = (char)(*zTStuff);
    } else {
      sprintf(resBuf+resBufLen, "\\u%04X", *zTStuff);
      resBufLen+=6;
    }
    zTStuff++;
  }
  resBuf[resBufLen] = 0;
  return resBuf;
}

UBool GenJP::isSemivoiced(UChar ch, UErrorCode &status) {
  UChar decompBuff[256];
  uint32_t decompLen = 0;

  decompLen = unorm_normalize(&ch, 1, UNORM_NFD, 0, decompBuff, 256, &status);

  if(decompBuff[decompLen-1]==0x309A) {
    return TRUE;
  } else {
    return FALSE;
  }
}

UBool GenJP::isVoiced(UChar ch, UErrorCode &status) {
  UChar decompBuff[256];
  uint32_t decompLen = 0;

  decompLen = unorm_normalize(&ch, 1, UNORM_NFD, 0, decompBuff, 256, &status);

  if(decompBuff[decompLen-1]==0x3099) {
    return TRUE;
  } else {
    return FALSE;
  }
}

const char *GenJP::getName(const UChar ch, UErrorCode &status) {
  uint32_t nameLen = 0;
  nameLen = u_charName(ch, U_UNICODE_CHAR_NAME, nameBuff, _bufferSize, &status);
  nameBuff[nameLen]=0;

  return nameBuff;
}

void GenJP::writeHeader(UErrorCode &status) {
fprintf(out, 
      "CollationElements {\n" 
      " Version { \"3.0\" }\n"
      " Sequence {\n"
      "  \"[strength 4][hiraganaQ on]\"\n");
}

void GenJP::writeFooter(UErrorCode &status) {
  fprintf(out, 
      "   }\n"
	  " }\n");
}


void GenJP::processCompatibility(UErrorCode &status) {
  UChar ch;
  fprintf(out, "\n// Equaling normal and halfwidth/fullwidth characters\n");
  fprintf(out, "\"&' '=\\u3000\" // IDEOGRAPHIC SPACE\n");
  for(ch = 0; ch < _hiraganaStart; ch++) {
    UChar compat = getHalf(ch);
    if(compat < 0xFFFF) {
      if(ch != 0x0027) {
        fprintf(out, "\"&'\\u%04X' = '\\u%04X'\" // %s\n", ch, compat, getName(ch, status)); // no name currently
      } else {
        fprintf(out, "\"&'' = '\\u%04X'\" // %s\n", compat, getName(ch, status)); // no name currently
      }
    }
  }
}

void GenJP::equalKatakanaToHiragana(UErrorCode &status) {
  UChar katakana=0, decompBuff[_bufferSize];
  const char *nameBuffer;
  uint32_t nameLen = 0, decompLen = 0;

  fprintf(out, "\n// Equaling Katakana, Hiragana and compatibility\n");
  // Make Hiragana and Katakana equal at the first three level
  for(katakana = _katakanaStart; katakana < _katakanaEnd; katakana++) {
    decompLen = unorm_normalize(&katakana, 1, UNORM_NFD, 0, decompBuff, 254, &status);
    if(decompLen == 1) {
      fprintf(out, "\"&\\u%04X = \\u%04X", katakana, katakana);

      UChar hiragana = getHiragana(katakana);
      if(hiragana <= _hiraganaEnd) {
        fprintf(out, " = \\u%04X", hiragana);
      }

      UChar compat = getHalf(katakana);
      if(compat < 0xFFFF) {
        fprintf(out, " = \\u%04X", compat);
      }

      nameBuffer = getName(katakana, status);

      fprintf(out, "\" // %s\n", nameBuffer);
    }
  }
}

void GenJP::processLengthMark(UErrorCode &status) { // This will do small vowels and generate rules for the length mark
  const UChar *vowel = _vowels;
  UChar kana = 0;
  char *vowelName = NULL, vowelNameBuffer[_bufferSize], nameBuffer[_bufferSize];
  uint32_t vowelNameLen = 0, nameLen = 0;
  
  fprintf(out, "\n// Rules for treating length mark\n");
  while(*vowel != NULL) { // process one vowel
    wasReset = TRUE;
    // printout "&[before 3]vowel"
    vowelNameLen = u_charName(*vowel, U_UNICODE_CHAR_NAME, vowelNameBuffer, _bufferSize, &status);
    vowelNameBuffer[vowelNameLen]=0;
    vowelName = vowelNameBuffer+vowelNameLen-1; // point at the vowel name - 

    fprintf(out, "\n\"&[before 3]\\u%04X\" //%s\n", *vowel, vowelNameBuffer);

    for(kana = _katakanaStart; kana <= _katakanaEnd; kana++) { // we iterate through Katakanas first - 
      nameLen = u_charName(kana, U_UNICODE_CHAR_NAME, nameBuffer, _bufferSize, &status);
      nameBuffer[nameLen]=0;   
      if(strcmp(vowelName, nameBuffer+nameLen-1)==0) { // This is the syllable ending in our vowel
        fprintf(out, " %s \\u%04X|\\u%04X", getRelation(), kana, _prolongedSoundMark);
        UChar comp = getHalf(kana);
        if(comp < 0xFFFF) { // if there is a compatibility, emit it...
          fprintf(out, "   = \\u%04X|\\u%04X", comp, _prolongedSoundMark);
        }
        UChar hiragana = getHiragana(kana);
        if(hiragana <= _hiraganaEnd) { // there is a corresponding Hiragana
          fprintf(out, "   = \\u%04X|\\u%04X", hiragana, _prolongedSoundMark);
        }       
        fprintf(out, "\" // %s\n", strrchr(nameBuffer, ' '));
      }
    }
    vowel++;
  }
}

void GenJP::processIterationMark(UErrorCode &status) {
  UChar katakana = _katakanaStart;
  const char *name;
  UBool hasSmall = FALSE;

  fprintf(out, "\n// Rules for treating iteration mark\n");
  while(katakana <= 0x30F3) { // We have anomalies for 0x30F3 and further.
    wasReset = TRUE;
    name = getName(katakana, status);
    fprintf(out, "\n");
    if (katakana == 0x30AB) { // KA needs to pick up small Ka (0x30F5)
      fprintf(out, "\"&[before 3]\\u%04X\" //%s\n", katakana, name);
      processIterationMark(katakana, status);
      processIterationMark(0x30F5, status);
    } else if (katakana == 0x30b1) { // KE needs to pick up small Ke (0x30F6)
      fprintf(out, "\"&[before 3]\\u%04X\" //%s\n", katakana, name);
      processIterationMark(katakana, status);
      processIterationMark(0x30F6, status);
    } else if (katakana == 0x30EE) { // Small WA, takes WA and VA
      katakana++;
      name = getName(katakana, status);
      fprintf(out, "\"&[before 3]\\u%04X\" //%s\n", katakana, name);
      processIterationMark(katakana, status); 
      processIterationMark(katakana-1, status);
      processIterationMark(0x30F7, status);
      processVoicedIterationMark(katakana, status);
      processVoicedIterationMark(0x30F7, status);
      processVoicedIterationMark(katakana-1, status);
    } else if (katakana >= 0x30F0 && katakana <= 0x30F2) { // WI, WE, WO -> VI, VE, VO
      uint16_t offset = 0x30F8-0x30F0;
      fprintf(out, "\"&[before 3]\\u%04X\" //%s\n", katakana, name);
      processIterationMark(katakana, status); 
      processIterationMark(katakana+offset, status);
      processVoicedIterationMark(katakana, status);
      processVoicedIterationMark(katakana+offset, status);
    } else if (katakana == 0x30A5) { // U -> VU (0x30F4)
      katakana++;
      name = getName(katakana, status);
      fprintf(out, "\"&[before 3]\\u%04X\" //%s\n", katakana, name);
      processIterationMark(katakana, status); 
      processIterationMark(katakana-1, status); 
      processIterationMark(0x30F4, status);
      processVoicedIterationMark(katakana, status);
      processVoicedIterationMark(katakana-1, status);
      processVoicedIterationMark(0x30F4, status);
    } else {
      if(strstr(name, "SMALL")) { // this is small Katakana
        hasSmall = TRUE;
        katakana++;
        name = getName(katakana, status);
      }
      fprintf(out, "\"&[before 3]\\u%04X\" //%s\n", katakana, name);

      // This is done for all the normal stuff 
      processIterationMark(katakana, status);
      if(hasSmall) {
        processIterationMark(katakana-1, status);
      }
  
      if(katakana < 0x30F3 && isVoiced(katakana+1, status)) { // Do the voiced part if we are not at the end
        processVoicedKana(katakana+1, status);
        if(hasSmall) {
          processVoicedKana(katakana, status);
        }

        if(isSemivoiced(katakana+2, status)) {
          processSemivoicedKana(katakana+2, status);
          // No semivoiced small kana
          katakana++;
        }
        katakana++;
      }

      hasSmall = FALSE;
    }
    katakana++;
  }

}

const char *GenJP::getRelation() {
  if(wasReset==TRUE) {
    wasReset = FALSE;
    return _tertiaryLess;
  } else {
    return _equal;
  }
}

void GenJP::processIterationMark(UChar katakana, UErrorCode &status) {
  fprintf(out, " %s \\u%04X|\\u%04X", getRelation(), katakana, _katakanaIterationMark);
  UChar compat = getHalf(katakana);
  if(compat < 0xFFFF) {
    fprintf(out, " = \\u%04X|\\u%04X", compat, _katakanaIterationMark);
  }
  UChar hiragana = getHiragana(katakana);
  if(hiragana <= _hiraganaEnd) {
    fprintf(out, " = \\u%04X|\\u%04X", hiragana, _hiraganaIterationMark);
  }
  fprintf(out, "\"\n");
}

void GenJP::processVoicedIterationMark(UChar katakana, UErrorCode &status) {
  fprintf(out, " %s \\u%04X|\\u%04X/\\u%04X", getRelation(), katakana, _katakanaVoicedIterationMark, _voicedMark);
  UChar compat = getHalf(katakana);
  if(compat < 0xFFFF) {
    fprintf(out, " = \\u%04X|\\u%04X/\\u%04X", compat, _katakanaVoicedIterationMark, _voicedMark);
  }
  UChar hiragana = getHiragana(katakana);
  if(hiragana <= _hiraganaEnd) {
    fprintf(out, " = \\u%04X|\\u%04X/\\u%04X", hiragana, _hiraganaVoicedIterationMark, _voicedMark);
  }
  fprintf(out, "\"\n");
}

void GenJP::processVoicedKana(UChar katakana, UErrorCode &status) {
  processIterationMark(katakana, status);
  processVoicedIterationMark(katakana-1, status);
  processVoicedIterationMark(katakana, status);
}

void GenJP::processSemivoicedKana(UChar katakana, UErrorCode &status) {
  processIterationMark(katakana, status);
  processVoicedIterationMark(katakana, status);
}

void GenJP::printOutKanji(UErrorCode &status) {
  fprintf(out, "        // Kanji, positioned over the top\n");
  fprintf(out, "        \"& [top] \"\n");
  fprintf(out, "        \"<\\u4e9c<\\u5516<\\u5a03<\\u963f<\\u54c0<\\u611b<\\u6328<\\u59f6<\\u9022\"\n");
  fprintf(out, "        \"<\\u8475<\\u831c<\\u7a50<\\u60aa<\\u63e1<\\u6e25<\\u65ed<\\u8466<\\u82a6\"\n");
  fprintf(out, "        \"<\\u9bf5<\\u6893<\\u5727<\\u65a1<\\u6271<\\u5b9b<\\u59d0<\\u867b<\\u98f4\"\n");
  fprintf(out, "        \"<\\u7d62<\\u7dbe<\\u9b8e<\\u6216<\\u7c9f<\\u88b7<\\u5b89<\\u5eb5<\\u6309\"\n");
  fprintf(out, "        \"<\\u6697<\\u6848<\\u95c7<\\u978d<\\u674f<\\u4ee5<\\u4f0a<\\u4f4d<\\u4f9d\"\n");
  fprintf(out, "        \"<\\u5049<\\u56f2<\\u5937<\\u59d4<\\u5a01<\\u5c09<\\u60df<\\u610f<\\u6170\"\n");
  fprintf(out, "        \"<\\u6613<\\u6905<\\u70ba<\\u754f<\\u7570<\\u79fb<\\u7dad<\\u7def<\\u80c3\"\n");
  fprintf(out, "        \"<\\u840e<\\u8863<\\u8b02<\\u9055<\\u907a<\\u533b<\\u4e95<\\u4ea5<\\u57df\"\n");
  fprintf(out, "        \"<\\u80b2<\\u90c1<\\u78ef<\\u4e00<\\u58f1<\\u6ea2<\\u9038<\\u7a32<\\u8328\"\n");
  fprintf(out, "        \"<\\u828b<\\u9c2f<\\u5141<\\u5370<\\u54bd<\\u54e1<\\u56e0<\\u59fb<\\u5f15\"\n");
  fprintf(out, "        \"<\\u98f2<\\u6deb<\\u80e4<\\u852d<\\u9662<\\u9670<\\u96a0<\\u97fb<\\u540b\"\n");
  fprintf(out, "        \"<\\u53f3<\\u5b87<\\u70cf<\\u7fbd<\\u8fc2<\\u96e8<\\u536f<\\u9d5c<\\u7aba\"\n");
  fprintf(out, "        \"<\\u4e11<\\u7893<\\u81fc<\\u6e26<\\u5618<\\u5504<\\u6b1d<\\u851a<\\u9c3b\"\n");
  fprintf(out, "        \"<\\u59e5<\\u53a9<\\u6d66<\\u74dc<\\u958f<\\u5642<\\u4e91<\\u904b<\\u96f2\"\n");
  fprintf(out, "        \"<\\u834f<\\u990c<\\u53e1<\\u55b6<\\u5b30<\\u5f71<\\u6620<\\u66f3<\\u6804\"\n");
  fprintf(out, "        \"<\\u6c38<\\u6cf3<\\u6d29<\\u745b<\\u76c8<\\u7a4e<\\u9834<\\u82f1<\\u885b\"\n");
  fprintf(out, "        \"<\\u8a60<\\u92ed<\\u6db2<\\u75ab<\\u76ca<\\u99c5<\\u60a6<\\u8b01<\\u8d8a\"\n");
  fprintf(out, "        \"<\\u95b2<\\u698e<\\u53ad<\\u5186<\\u5712<\\u5830<\\u5944<\\u5bb4<\\u5ef6\"\n");
  fprintf(out, "        \"<\\u6028<\\u63a9<\\u63f4<\\u6cbf<\\u6f14<\\u708e<\\u7114<\\u7159<\\u71d5\"\n");
  fprintf(out, "        \"<\\u733f<\\u7e01<\\u8276<\\u82d1<\\u8597<\\u9060<\\u925b<\\u9d1b<\\u5869\"\n");
  fprintf(out, "        \"<\\u65bc<\\u6c5a<\\u7525<\\u51f9<\\u592e<\\u5965<\\u5f80<\\u5fdc<\\u62bc\"\n");
  fprintf(out, "        \"<\\u65fa<\\u6a2a<\\u6b27<\\u6bb4<\\u738b<\\u7fc1<\\u8956<\\u9d2c<\\u9d0e\"\n");
  fprintf(out, "        \"<\\u9ec4<\\u5ca1<\\u6c96<\\u837b<\\u5104<\\u5c4b<\\u61b6<\\u81c6<\\u6876\"\n");
  fprintf(out, "        \"<\\u7261<\\u4e59<\\u4ffa<\\u5378<\\u6069<\\u6e29<\\u7a4f<\\u97f3<\\u4e0b\"\n");
  fprintf(out, "        \"<\\u5316<\\u4eee<\\u4f55<\\u4f3d<\\u4fa1<\\u4f73<\\u52a0<\\u53ef<\\u5609\"\n");
  fprintf(out, "        \"<\\u590f<\\u5ac1<\\u5bb6<\\u5be1<\\u79d1<\\u6687<\\u679c<\\u67b6<\\u6b4c\"\n");
  fprintf(out, "        \"<\\u6cb3<\\u706b<\\u73c2<\\u798d<\\u79be<\\u7a3c<\\u7b87<\\u82b1<\\u82db\"\n");
  fprintf(out, "        \"<\\u8304<\\u8377<\\u83ef<\\u83d3<\\u8766<\\u8ab2<\\u5629<\\u8ca8<\\u8fe6\"\n");
  fprintf(out, "        \"<\\u904e<\\u971e<\\u868a<\\u4fc4<\\u5ce8<\\u6211<\\u7259<\\u753b<\\u81e5\"\n");
  fprintf(out, "        \"<\\u82bd<\\u86fe<\\u8cc0<\\u96c5<\\u9913<\\u99d5<\\u4ecb<\\u4f1a<\\u89e3\"\n");
  fprintf(out, "        \"<\\u56de<\\u584a<\\u58ca<\\u5efb<\\u5feb<\\u602a<\\u6094<\\u6062<\\u61d0\"\n");
  fprintf(out, "        \"<\\u6212<\\u62d0<\\u6539<\\u9b41<\\u6666<\\u68b0<\\u6d77<\\u7070<\\u754c\"\n");
  fprintf(out, "        \"<\\u7686<\\u7d75<\\u82a5<\\u87f9<\\u958b<\\u968e<\\u8c9d<\\u51f1<\\u52be\"\n");
  fprintf(out, "        \"<\\u5916<\\u54b3<\\u5bb3<\\u5d16<\\u6168<\\u6982<\\u6daf<\\u788d<\\u84cb\"\n");
  fprintf(out, "        \"<\\u8857<\\u8a72<\\u93a7<\\u9ab8<\\u6d6c<\\u99a8<\\u86d9<\\u57a3<\\u67ff\"\n");
  fprintf(out, "        \"<\\u86ce<\\u920e<\\u5283<\\u5687<\\u5404<\\u5ed3<\\u62e1<\\u64b9<\\u683c\"\n");
  fprintf(out, "        \"<\\u6838<\\u6bbb<\\u7372<\\u78ba<\\u7a6b<\\u899a<\\u89d2<\\u8d6b<\\u8f03\"\n");
  fprintf(out, "        \"<\\u90ed<\\u95a3<\\u9694<\\u9769<\\u5b66<\\u5cb3<\\u697d<\\u984d<\\u984e\"\n");
  fprintf(out, "        \"<\\u639b<\\u7b20<\\u6a2b<\\u6a7f<\\u68b6<\\u9c0d<\\u6f5f<\\u5272<\\u559d\"\n");
  fprintf(out, "        \"<\\u6070<\\u62ec<\\u6d3b<\\u6e07<\\u6ed1<\\u845b<\\u8910<\\u8f44<\\u4e14\"\n");
  fprintf(out, "        \"<\\u9c39<\\u53f6<\\u691b<\\u6a3a<\\u9784<\\u682a<\\u515c<\\u7ac3<\\u84b2\"\n");
  fprintf(out, "        \"<\\u91dc<\\u938c<\\u565b<\\u9d28<\\u6822<\\u8305<\\u8431<\\u7ca5<\\u5208\"\n");
  fprintf(out, "        \"<\\u82c5<\\u74e6<\\u4e7e<\\u4f83<\\u51a0<\\u5bd2<\\u520a<\\u52d8<\\u52e7\"\n");
  fprintf(out, "        \"<\\u5dfb<\\u559a<\\u582a<\\u59e6<\\u5b8c<\\u5b98<\\u5bdb<\\u5e72<\\u5e79\"\n");
  fprintf(out, "        \"<\\u60a3<\\u611f<\\u6163<\\u61be<\\u63db<\\u6562<\\u67d1<\\u6853<\\u68fa\"\n");
  fprintf(out, "        \"<\\u6b3e<\\u6b53<\\u6c57<\\u6f22<\\u6f97<\\u6f45<\\u74b0<\\u7518<\\u76e3\"\n");
  fprintf(out, "        \"<\\u770b<\\u7aff<\\u7ba1<\\u7c21<\\u7de9<\\u7f36<\\u7ff0<\\u809d<\\u8266\"\n");
  fprintf(out, "        \"<\\u839e<\\u89b3<\\u8acc<\\u8cab<\\u9084<\\u9451<\\u9593<\\u9591<\\u95a2\"\n");
  fprintf(out, "        \"<\\u9665<\\u97d3<\\u9928<\\u8218<\\u4e38<\\u542b<\\u5cb8<\\u5dcc<\\u73a9\"\n");
  fprintf(out, "        \"<\\u764c<\\u773c<\\u5ca9<\\u7feb<\\u8d0b<\\u96c1<\\u9811<\\u9854<\\u9858\"\n");
  fprintf(out, "        \"<\\u4f01<\\u4f0e<\\u5371<\\u559c<\\u5668<\\u57fa<\\u5947<\\u5b09<\\u5bc4\"\n");
  fprintf(out, "        \"<\\u5c90<\\u5e0c<\\u5e7e<\\u5fcc<\\u63ee<\\u673a<\\u65d7<\\u65e2<\\u671f\"\n");
  fprintf(out, "        \"<\\u68cb<\\u68c4<\\u6a5f<\\u5e30<\\u6bc5<\\u6c17<\\u6c7d<\\u757f<\\u7948\"\n");
  fprintf(out, "        \"<\\u5b63<\\u7a00<\\u7d00<\\u5fbd<\\u898f<\\u8a18<\\u8cb4<\\u8d77<\\u8ecc\"\n");
  fprintf(out, "        \"<\\u8f1d<\\u98e2<\\u9a0e<\\u9b3c<\\u4e80<\\u507d<\\u5100<\\u5993<\\u5b9c\"\n");
  fprintf(out, "        \"<\\u622f<\\u6280<\\u64ec<\\u6b3a<\\u72a0<\\u7591<\\u7947<\\u7fa9<\\u87fb\"\n");
  fprintf(out, "        \"<\\u8abc<\\u8b70<\\u63ac<\\u83ca<\\u97a0<\\u5409<\\u5403<\\u55ab<\\u6854\"\n");
  fprintf(out, "        \"<\\u6a58<\\u8a70<\\u7827<\\u6775<\\u9ecd<\\u5374<\\u5ba2<\\u811a<\\u8650\"\n");
  fprintf(out, "        \"<\\u9006<\\u4e18<\\u4e45<\\u4ec7<\\u4f11<\\u53ca<\\u5438<\\u5bae<\\u5f13\"\n");
  fprintf(out, "        \"<\\u6025<\\u6551<\\u673d<\\u6c42<\\u6c72<\\u6ce3<\\u7078<\\u7403<\\u7a76\"\n");
  fprintf(out, "        \"<\\u7aae<\\u7b08<\\u7d1a<\\u7cfe<\\u7d66<\\u65e7<\\u725b<\\u53bb<\\u5c45\"\n");
  fprintf(out, "        \"<\\u5de8<\\u62d2<\\u62e0<\\u6319<\\u6e20<\\u865a<\\u8a31<\\u8ddd<\\u92f8\"\n");
  fprintf(out, "        \"<\\u6f01<\\u79a6<\\u9b5a<\\u4ea8<\\u4eab<\\u4eac<\\u4f9b<\\u4fa0<\\u50d1\"\n");
  fprintf(out, "        \"<\\u5147<\\u7af6<\\u5171<\\u51f6<\\u5354<\\u5321<\\u537f<\\u53eb<\\u55ac\"\n");
  fprintf(out, "        \"<\\u5883<\\u5ce1<\\u5f37<\\u5f4a<\\u602f<\\u6050<\\u606d<\\u631f<\\u6559\"\n");
  fprintf(out, "        \"<\\u6a4b<\\u6cc1<\\u72c2<\\u72ed<\\u77ef<\\u80f8<\\u8105<\\u8208<\\u854e\"\n");
  fprintf(out, "        \"<\\u90f7<\\u93e1<\\u97ff<\\u9957<\\u9a5a<\\u4ef0<\\u51dd<\\u5c2d<\\u6681\"\n");
  fprintf(out, "        \"<\\u696d<\\u5c40<\\u66f2<\\u6975<\\u7389<\\u6850<\\u7c81<\\u50c5<\\u52e4\"\n");
  fprintf(out, "        \"<\\u5747<\\u5dfe<\\u9326<\\u65a4<\\u6b23<\\u6b3d<\\u7434<\\u7981<\\u79bd\"\n");
  fprintf(out, "        \"<\\u7b4b<\\u7dca<\\u82b9<\\u83cc<\\u887f<\\u895f<\\u8b39<\\u8fd1<\\u91d1\"\n");
  fprintf(out, "        \"<\\u541f<\\u9280<\\u4e5d<\\u5036<\\u53e5<\\u533a<\\u72d7<\\u7396<\\u77e9\"\n");
  fprintf(out, "        \"<\\u82e6<\\u8eaf<\\u99c6<\\u99c8<\\u99d2<\\u5177<\\u611a<\\u865e<\\u55b0\"\n");
  fprintf(out, "        \"<\\u7a7a<\\u5076<\\u5bd3<\\u9047<\\u9685<\\u4e32<\\u6adb<\\u91e7<\\u5c51\"\n");
  fprintf(out, "        \"<\\u5c48<\\u6398<\\u7a9f<\\u6c93<\\u9774<\\u8f61<\\u7aaa<\\u718a<\\u9688\"\n");
  fprintf(out, "        \"<\\u7c82<\\u6817<\\u7e70<\\u6851<\\u936c<\\u52f2<\\u541b<\\u85ab<\\u8a13\"\n");
  fprintf(out, "        \"<\\u7fa4<\\u8ecd<\\u90e1<\\u5366<\\u8888<\\u7941<\\u4fc2<\\u50be<\\u5211\"\n");
  fprintf(out, "        \"<\\u5144<\\u5553<\\u572d<\\u73ea<\\u578b<\\u5951<\\u5f62<\\u5f84<\\u6075\"\n");
  fprintf(out, "        \"<\\u6176<\\u6167<\\u61a9<\\u63b2<\\u643a<\\u656c<\\u666f<\\u6842<\\u6e13\"\n");
  fprintf(out, "        \"<\\u7566<\\u7a3d<\\u7cfb<\\u7d4c<\\u7d99<\\u7e4b<\\u7f6b<\\u830e<\\u834a\"\n");
  fprintf(out, "        \"<\\u86cd<\\u8a08<\\u8a63<\\u8b66<\\u8efd<\\u981a<\\u9d8f<\\u82b8<\\u8fce\"\n");
  fprintf(out, "        \"<\\u9be8<\\u5287<\\u621f<\\u6483<\\u6fc0<\\u9699<\\u6841<\\u5091<\\u6b20\"\n");
  fprintf(out, "        \"<\\u6c7a<\\u6f54<\\u7a74<\\u7d50<\\u8840<\\u8a23<\\u6708<\\u4ef6<\\u5039\"\n");
  fprintf(out, "        \"<\\u5026<\\u5065<\\u517c<\\u5238<\\u5263<\\u55a7<\\u570f<\\u5805<\\u5acc\"\n");
  fprintf(out, "        \"<\\u5efa<\\u61b2<\\u61f8<\\u62f3<\\u6372<\\u691c<\\u6a29<\\u727d<\\u72ac\"\n");
  fprintf(out, "        \"<\\u732e<\\u7814<\\u786f<\\u7d79<\\u770c<\\u80a9<\\u898b<\\u8b19<\\u8ce2\"\n");
  fprintf(out, "        \"<\\u8ed2<\\u9063<\\u9375<\\u967a<\\u9855<\\u9a13<\\u9e78<\\u5143<\\u539f\"\n");
  fprintf(out, "        \"<\\u53b3<\\u5e7b<\\u5f26<\\u6e1b<\\u6e90<\\u7384<\\u73fe<\\u7d43<\\u8237\"\n");
  fprintf(out, "        \"<\\u8a00<\\u8afa<\\u9650<\\u4e4e<\\u500b<\\u53e4<\\u547c<\\u56fa<\\u59d1\"\n");
  fprintf(out, "        \"<\\u5b64<\\u5df1<\\u5eab<\\u5f27<\\u6238<\\u6545<\\u67af<\\u6e56<\\u72d0\"\n");
  fprintf(out, "        \"<\\u7cca<\\u88b4<\\u80a1<\\u80e1<\\u83f0<\\u864e<\\u8a87<\\u8de8<\\u9237\"\n");
  fprintf(out, "        \"<\\u96c7<\\u9867<\\u9f13<\\u4e94<\\u4e92<\\u4f0d<\\u5348<\\u5449<\\u543e\"\n");
  fprintf(out, "        \"<\\u5a2f<\\u5f8c<\\u5fa1<\\u609f<\\u68a7<\\u6a8e<\\u745a<\\u7881<\\u8a9e\"\n");
  fprintf(out, "        \"<\\u8aa4<\\u8b77<\\u9190<\\u4e5e<\\u9bc9<\\u4ea4<\\u4f7c<\\u4faf<\\u5019\"\n");
  fprintf(out, "        \"<\\u5016<\\u5149<\\u516c<\\u529f<\\u52b9<\\u52fe<\\u539a<\\u53e3<\\u5411\"\n");
  fprintf(out, "        \"<\\u540e<\\u5589<\\u5751<\\u57a2<\\u597d<\\u5b54<\\u5b5d<\\u5b8f<\\u5de5\"\n");
  fprintf(out, "        \"<\\u5de7<\\u5df7<\\u5e78<\\u5e83<\\u5e9a<\\u5eb7<\\u5f18<\\u6052<\\u614c\"\n");
  fprintf(out, "        \"<\\u6297<\\u62d8<\\u63a7<\\u653b<\\u6602<\\u6643<\\u66f4<\\u676d<\\u6821\"\n");
  fprintf(out, "        \"<\\u6897<\\u69cb<\\u6c5f<\\u6d2a<\\u6d69<\\u6e2f<\\u6e9d<\\u7532<\\u7687\"\n");
  fprintf(out, "        \"<\\u786c<\\u7a3f<\\u7ce0<\\u7d05<\\u7d18<\\u7d5e<\\u7db1<\\u8015<\\u8003\"\n");
  fprintf(out, "        \"<\\u80af<\\u80b1<\\u8154<\\u818f<\\u822a<\\u8352<\\u884c<\\u8861<\\u8b1b\"\n");
  fprintf(out, "        \"<\\u8ca2<\\u8cfc<\\u90ca<\\u9175<\\u9271<\\u783f<\\u92fc<\\u95a4<\\u964d\"\n");
  fprintf(out, "        \"<\\u9805<\\u9999<\\u9ad8<\\u9d3b<\\u525b<\\u52ab<\\u53f7<\\u5408<\\u58d5\"\n");
  fprintf(out, "        \"<\\u62f7<\\u6fe0<\\u8c6a<\\u8f5f<\\u9eb9<\\u514b<\\u523b<\\u544a<\\u56fd\"\n");
  fprintf(out, "        \"<\\u7a40<\\u9177<\\u9d60<\\u9ed2<\\u7344<\\u6f09<\\u8170<\\u7511<\\u5ffd\"\n");
  fprintf(out, "        \"<\\u60da<\\u9aa8<\\u72db<\\u8fbc<\\u6b64<\\u9803<\\u4eca<\\u56f0<\\u5764\"\n");
  fprintf(out, "        \"<\\u58be<\\u5a5a<\\u6068<\\u61c7<\\u660f<\\u6606<\\u6839<\\u68b1<\\u6df7\"\n");
  fprintf(out, "        \"<\\u75d5<\\u7d3a<\\u826e<\\u9b42<\\u4e9b<\\u4f50<\\u53c9<\\u5506<\\u5d6f\"\n");
  fprintf(out, "        \"<\\u5de6<\\u5dee<\\u67fb<\\u6c99<\\u7473<\\u7802<\\u8a50<\\u9396<\\u88df\"\n");
  fprintf(out, "        \"<\\u5750<\\u5ea7<\\u632b<\\u50b5<\\u50ac<\\u518d<\\u6700<\\u54c9<\\u585e\"\n");
  fprintf(out, "        \"<\\u59bb<\\u5bb0<\\u5f69<\\u624d<\\u63a1<\\u683d<\\u6b73<\\u6e08<\\u707d\"\n");
  fprintf(out, "        \"<\\u91c7<\\u7280<\\u7815<\\u7826<\\u796d<\\u658e<\\u7d30<\\u83dc<\\u88c1\"\n");
  fprintf(out, "        \"<\\u8f09<\\u969b<\\u5264<\\u5728<\\u6750<\\u7f6a<\\u8ca1<\\u51b4<\\u5742\"\n");
  fprintf(out, "        \"<\\u962a<\\u583a<\\u698a<\\u80b4<\\u54b2<\\u5d0e<\\u57fc<\\u7895<\\u9dfa\"\n");
  fprintf(out, "        \"<\\u4f5c<\\u524a<\\u548b<\\u643e<\\u6628<\\u6714<\\u67f5<\\u7a84<\\u7b56\"\n");
  fprintf(out, "        \"<\\u7d22<\\u932f<\\u685c<\\u9bad<\\u7b39<\\u5319<\\u518a<\\u5237<\\u5bdf\"\n");
  fprintf(out, "        \"<\\u62f6<\\u64ae<\\u64e6<\\u672d<\\u6bba<\\u85a9<\\u96d1<\\u7690<\\u9bd6\"\n");
  fprintf(out, "        \"<\\u634c<\\u9306<\\u9bab<\\u76bf<\\u6652<\\u4e09<\\u5098<\\u53c2<\\u5c71\"\n");
  fprintf(out, "        \"<\\u60e8<\\u6492<\\u6563<\\u685f<\\u71e6<\\u73ca<\\u7523<\\u7b97<\\u7e82\"\n");
  fprintf(out, "        \"<\\u8695<\\u8b83<\\u8cdb<\\u9178<\\u9910<\\u65ac<\\u66ab<\\u6b8b<\\u4ed5\"\n");
  fprintf(out, "        \"<\\u4ed4<\\u4f3a<\\u4f7f<\\u523a<\\u53f8<\\u53f2<\\u55e3<\\u56db<\\u58eb\"\n");
  fprintf(out, "        \"<\\u59cb<\\u59c9<\\u59ff<\\u5b50<\\u5c4d<\\u5e02<\\u5e2b<\\u5fd7<\\u601d\"\n");
  fprintf(out, "        \"<\\u6307<\\u652f<\\u5b5c<\\u65af<\\u65bd<\\u65e8<\\u679d<\\u6b62<\\u6b7b\"\n");
  fprintf(out, "        \"<\\u6c0f<\\u7345<\\u7949<\\u79c1<\\u7cf8<\\u7d19<\\u7d2b<\\u80a2<\\u8102\"\n");
  fprintf(out, "        \"<\\u81f3<\\u8996<\\u8a5e<\\u8a69<\\u8a66<\\u8a8c<\\u8aee<\\u8cc7<\\u8cdc\"\n");
  fprintf(out, "        \"<\\u96cc<\\u98fc<\\u6b6f<\\u4e8b<\\u4f3c<\\u4f8d<\\u5150<\\u5b57<\\u5bfa\"\n");
  fprintf(out, "        \"<\\u6148<\\u6301<\\u6642<\\u6b21<\\u6ecb<\\u6cbb<\\u723e<\\u74bd<\\u75d4\"\n");
  fprintf(out, "        \"<\\u78c1<\\u793a<\\u800c<\\u8033<\\u81ea<\\u8494<\\u8f9e<\\u6c50<\\u9e7f\"\n");
  fprintf(out, "        \"<\\u5f0f<\\u8b58<\\u9d2b<\\u7afa<\\u8ef8<\\u5b8d<\\u96eb<\\u4e03<\\u53f1\"\n");
  fprintf(out, "        \"<\\u57f7<\\u5931<\\u5ac9<\\u5ba4<\\u6089<\\u6e7f<\\u6f06<\\u75be<\\u8cea\"\n");
  fprintf(out, "        \"<\\u5b9f<\\u8500<\\u7be0<\\u5072<\\u67f4<\\u829d<\\u5c61<\\u854a<\\u7e1e\"\n");
  fprintf(out, "        \"<\\u820e<\\u5199<\\u5c04<\\u6368<\\u8d66<\\u659c<\\u716e<\\u793e<\\u7d17\"\n");
  fprintf(out, "        \"<\\u8005<\\u8b1d<\\u8eca<\\u906e<\\u86c7<\\u90aa<\\u501f<\\u52fa<\\u5c3a\"\n");
  fprintf(out, "        \"<\\u6753<\\u707c<\\u7235<\\u914c<\\u91c8<\\u932b<\\u82e5<\\u5bc2<\\u5f31\"\n");
  fprintf(out, "        \"<\\u60f9<\\u4e3b<\\u53d6<\\u5b88<\\u624b<\\u6731<\\u6b8a<\\u72e9<\\u73e0\"\n");
  fprintf(out, "        \"<\\u7a2e<\\u816b<\\u8da3<\\u9152<\\u9996<\\u5112<\\u53d7<\\u546a<\\u5bff\"\n");
  fprintf(out, "        \"<\\u6388<\\u6a39<\\u7dac<\\u9700<\\u56da<\\u53ce<\\u5468<\\u5b97<\\u5c31\"\n");
  fprintf(out, "        \"<\\u5dde<\\u4fee<\\u6101<\\u62fe<\\u6d32<\\u79c0<\\u79cb<\\u7d42<\\u7e4d\"\n");
  fprintf(out, "        \"<\\u7fd2<\\u81ed<\\u821f<\\u8490<\\u8846<\\u8972<\\u8b90<\\u8e74<\\u8f2f\"\n");
  fprintf(out, "        \"<\\u9031<\\u914b<\\u916c<\\u96c6<\\u919c<\\u4ec0<\\u4f4f<\\u5145<\\u5341\"\n");
  fprintf(out, "        \"<\\u5f93<\\u620e<\\u67d4<\\u6c41<\\u6e0b<\\u7363<\\u7e26<\\u91cd<\\u9283\"\n");
  fprintf(out, "        \"<\\u53d4<\\u5919<\\u5bbf<\\u6dd1<\\u795d<\\u7e2e<\\u7c9b<\\u587e<\\u719f\"\n");
  fprintf(out, "        \"<\\u51fa<\\u8853<\\u8ff0<\\u4fca<\\u5cfb<\\u6625<\\u77ac<\\u7ae3<\\u821c\"\n");
  fprintf(out, "        \"<\\u99ff<\\u51c6<\\u5faa<\\u65ec<\\u696f<\\u6b89<\\u6df3<\\u6e96<\\u6f64\"\n");
  fprintf(out, "        \"<\\u76fe<\\u7d14<\\u5de1<\\u9075<\\u9187<\\u9806<\\u51e6<\\u521d<\\u6240\"\n");
  fprintf(out, "        \"<\\u6691<\\u66d9<\\u6e1a<\\u5eb6<\\u7dd2<\\u7f72<\\u66f8<\\u85af<\\u85f7\"\n");
  fprintf(out, "        \"<\\u8af8<\\u52a9<\\u53d9<\\u5973<\\u5e8f<\\u5f90<\\u6055<\\u92e4<\\u9664\"\n");
  fprintf(out, "        \"<\\u50b7<\\u511f<\\u52dd<\\u5320<\\u5347<\\u53ec<\\u54e8<\\u5546<\\u5531\"\n");
  fprintf(out, "        \"<\\u5617<\\u5968<\\u59be<\\u5a3c<\\u5bb5<\\u5c06<\\u5c0f<\\u5c11<\\u5c1a\"\n");
  fprintf(out, "        \"<\\u5e84<\\u5e8a<\\u5ee0<\\u5f70<\\u627f<\\u6284<\\u62db<\\u638c<\\u6377\"\n");
  fprintf(out, "        \"<\\u6607<\\u660c<\\u662d<\\u6676<\\u677e<\\u68a2<\\u6a1f<\\u6a35<\\u6cbc\"\n");
  fprintf(out, "        \"<\\u6d88<\\u6e09<\\u6e58<\\u713c<\\u7126<\\u7167<\\u75c7<\\u7701<\\u785d\"\n");
  fprintf(out, "        \"<\\u7901<\\u7965<\\u79f0<\\u7ae0<\\u7b11<\\u7ca7<\\u7d39<\\u8096<\\u83d6\"\n");
  fprintf(out, "        \"<\\u848b<\\u8549<\\u885d<\\u88f3<\\u8a1f<\\u8a3c<\\u8a54<\\u8a73<\\u8c61\"\n");
  fprintf(out, "        \"<\\u8cde<\\u91a4<\\u9266<\\u937e<\\u9418<\\u969c<\\u9798<\\u4e0a<\\u4e08\"\n");
  fprintf(out, "        \"<\\u4e1e<\\u4e57<\\u5197<\\u5270<\\u57ce<\\u5834<\\u58cc<\\u5b22<\\u5e38\"\n");
  fprintf(out, "        \"<\\u60c5<\\u64fe<\\u6761<\\u6756<\\u6d44<\\u72b6<\\u7573<\\u7a63<\\u84b8\"\n");
  fprintf(out, "        \"<\\u8b72<\\u91b8<\\u9320<\\u5631<\\u57f4<\\u98fe<\\u62ed<\\u690d<\\u6b96\"\n");
  fprintf(out, "        \"<\\u71ed<\\u7e54<\\u8077<\\u8272<\\u89e6<\\u98df<\\u8755<\\u8fb1<\\u5c3b\"\n");
  fprintf(out, "        \"<\\u4f38<\\u4fe1<\\u4fb5<\\u5507<\\u5a20<\\u5bdd<\\u5be9<\\u5fc3<\\u614e\"\n");
  fprintf(out, "        \"<\\u632f<\\u65b0<\\u664b<\\u68ee<\\u699b<\\u6d78<\\u6df1<\\u7533<\\u75b9\"\n");
  fprintf(out, "        \"<\\u771f<\\u795e<\\u79e6<\\u7d33<\\u81e3<\\u82af<\\u85aa<\\u89aa<\\u8a3a\"\n");
  fprintf(out, "        \"<\\u8eab<\\u8f9b<\\u9032<\\u91dd<\\u9707<\\u4eba<\\u4ec1<\\u5203<\\u5875\"\n");
  fprintf(out, "        \"<\\u58ec<\\u5c0b<\\u751a<\\u5c3d<\\u814e<\\u8a0a<\\u8fc5<\\u9663<\\u976d\"\n");
  fprintf(out, "        \"<\\u7b25<\\u8acf<\\u9808<\\u9162<\\u56f3<\\u53a8<\\u9017<\\u5439<\\u5782\"\n");
  fprintf(out, "        \"<\\u5e25<\\u63a8<\\u6c34<\\u708a<\\u7761<\\u7c8b<\\u7fe0<\\u8870<\\u9042\"\n");
  fprintf(out, "        \"<\\u9154<\\u9310<\\u9318<\\u968f<\\u745e<\\u9ac4<\\u5d07<\\u5d69<\\u6570\"\n");
  fprintf(out, "        \"<\\u67a2<\\u8da8<\\u96db<\\u636e<\\u6749<\\u6919<\\u83c5<\\u9817<\\u96c0\"\n");
  fprintf(out, "        \"<\\u88fe<\\u6f84<\\u647a<\\u5bf8<\\u4e16<\\u702c<\\u755d<\\u662f<\\u51c4\"\n");
  fprintf(out, "        \"<\\u5236<\\u52e2<\\u59d3<\\u5f81<\\u6027<\\u6210<\\u653f<\\u6574<\\u661f\"\n");
  fprintf(out, "        \"<\\u6674<\\u68f2<\\u6816<\\u6b63<\\u6e05<\\u7272<\\u751f<\\u76db<\\u7cbe\"\n");
  fprintf(out, "        \"<\\u8056<\\u58f0<\\u88fd<\\u897f<\\u8aa0<\\u8a93<\\u8acb<\\u901d<\\u9192\"\n");
  fprintf(out, "        \"<\\u9752<\\u9759<\\u6589<\\u7a0e<\\u8106<\\u96bb<\\u5e2d<\\u60dc<\\u621a\"\n");
  fprintf(out, "        \"<\\u65a5<\\u6614<\\u6790<\\u77f3<\\u7a4d<\\u7c4d<\\u7e3e<\\u810a<\\u8cac\"\n");
  fprintf(out, "        \"<\\u8d64<\\u8de1<\\u8e5f<\\u78a9<\\u5207<\\u62d9<\\u63a5<\\u6442<\\u6298\"\n");
  fprintf(out, "        \"<\\u8a2d<\\u7a83<\\u7bc0<\\u8aac<\\u96ea<\\u7d76<\\u820c<\\u8749<\\u4ed9\"\n");
  fprintf(out, "        \"<\\u5148<\\u5343<\\u5360<\\u5ba3<\\u5c02<\\u5c16<\\u5ddd<\\u6226<\\u6247\"\n");
  fprintf(out, "        \"<\\u64b0<\\u6813<\\u6834<\\u6cc9<\\u6d45<\\u6d17<\\u67d3<\\u6f5c<\\u714e\"\n");
  fprintf(out, "        \"<\\u717d<\\u65cb<\\u7a7f<\\u7bad<\\u7dda<\\u7e4a<\\u7fa8<\\u817a<\\u821b\"\n");
  fprintf(out, "        \"<\\u8239<\\u85a6<\\u8a6e<\\u8cce<\\u8df5<\\u9078<\\u9077<\\u92ad<\\u9291\"\n");
  fprintf(out, "        \"<\\u9583<\\u9bae<\\u524d<\\u5584<\\u6f38<\\u7136<\\u5168<\\u7985<\\u7e55\"\n");
  fprintf(out, "        \"<\\u81b3<\\u7cce<\\u564c<\\u5851<\\u5ca8<\\u63aa<\\u66fe<\\u66fd<\\u695a\"\n");
  fprintf(out, "        \"<\\u72d9<\\u758f<\\u758e<\\u790e<\\u7956<\\u79df<\\u7c97<\\u7d20<\\u7d44\"\n");
  fprintf(out, "        \"<\\u8607<\\u8a34<\\u963b<\\u9061<\\u9f20<\\u50e7<\\u5275<\\u53cc<\\u53e2\"\n");
  fprintf(out, "        \"<\\u5009<\\u55aa<\\u58ee<\\u594f<\\u723d<\\u5b8b<\\u5c64<\\u531d<\\u60e3\"\n");
  fprintf(out, "        \"<\\u60f3<\\u635c<\\u6383<\\u633f<\\u63bb<\\u64cd<\\u65e9<\\u66f9<\\u5de3\"\n");
  fprintf(out, "        \"<\\u69cd<\\u69fd<\\u6f15<\\u71e5<\\u4e89<\\u75e9<\\u76f8<\\u7a93<\\u7cdf\"\n");
  fprintf(out, "        \"<\\u7dcf<\\u7d9c<\\u8061<\\u8349<\\u8358<\\u846c<\\u84bc<\\u85fb<\\u88c5\"\n");
  fprintf(out, "        \"<\\u8d70<\\u9001<\\u906d<\\u9397<\\u971c<\\u9a12<\\u50cf<\\u5897<\\u618e\"\n");
  fprintf(out, "        \"<\\u81d3<\\u8535<\\u8d08<\\u9020<\\u4fc3<\\u5074<\\u5247<\\u5373<\\u606f\"\n");
  fprintf(out, "        \"<\\u6349<\\u675f<\\u6e2c<\\u8db3<\\u901f<\\u4fd7<\\u5c5e<\\u8cca<\\u65cf\"\n");
  fprintf(out, "        \"<\\u7d9a<\\u5352<\\u8896<\\u5176<\\u63c3<\\u5b58<\\u5b6b<\\u5c0a<\\u640d\"\n");
  fprintf(out, "        \"<\\u6751<\\u905c<\\u4ed6<\\u591a<\\u592a<\\u6c70<\\u8a51<\\u553e<\\u5815\"\n");
  fprintf(out, "        \"<\\u59a5<\\u60f0<\\u6253<\\u67c1<\\u8235<\\u6955<\\u9640<\\u99c4<\\u9a28\"\n");
  fprintf(out, "        \"<\\u4f53<\\u5806<\\u5bfe<\\u8010<\\u5cb1<\\u5e2f<\\u5f85<\\u6020<\\u614b\"\n");
  fprintf(out, "        \"<\\u6234<\\u66ff<\\u6cf0<\\u6ede<\\u80ce<\\u817f<\\u82d4<\\u888b<\\u8cb8\"\n");
  fprintf(out, "        \"<\\u9000<\\u902e<\\u968a<\\u9edb<\\u9bdb<\\u4ee3<\\u53f0<\\u5927<\\u7b2c\"\n");
  fprintf(out, "        \"<\\u918d<\\u984c<\\u9df9<\\u6edd<\\u7027<\\u5353<\\u5544<\\u5b85<\\u6258\"\n");
  fprintf(out, "        \"<\\u629e<\\u62d3<\\u6ca2<\\u6fef<\\u7422<\\u8a17<\\u9438<\\u6fc1<\\u8afe\"\n");
  fprintf(out, "        \"<\\u8338<\\u51e7<\\u86f8<\\u53ea<\\u53e9<\\u4f46<\\u9054<\\u8fb0<\\u596a\"\n");
  fprintf(out, "        \"<\\u8131<\\u5dfd<\\u7aea<\\u8fbf<\\u68da<\\u8c37<\\u72f8<\\u9c48<\\u6a3d\"\n");
  fprintf(out, "        \"<\\u8ab0<\\u4e39<\\u5358<\\u5606<\\u5766<\\u62c5<\\u63a2<\\u65e6<\\u6b4e\"\n");
  fprintf(out, "        \"<\\u6de1<\\u6e5b<\\u70ad<\\u77ed<\\u7aef<\\u7baa<\\u7dbb<\\u803d<\\u80c6\"\n");
  fprintf(out, "        \"<\\u86cb<\\u8a95<\\u935b<\\u56e3<\\u58c7<\\u5f3e<\\u65ad<\\u6696<\\u6a80\"\n");
  fprintf(out, "        \"<\\u6bb5<\\u7537<\\u8ac7<\\u5024<\\u77e5<\\u5730<\\u5f1b<\\u6065<\\u667a\"\n");
  fprintf(out, "        \"<\\u6c60<\\u75f4<\\u7a1a<\\u7f6e<\\u81f4<\\u8718<\\u9045<\\u99b3<\\u7bc9\"\n");
  fprintf(out, "        \"<\\u755c<\\u7af9<\\u7b51<\\u84c4<\\u9010<\\u79e9<\\u7a92<\\u8336<\\u5ae1\"\n");
  fprintf(out, "        \"<\\u7740<\\u4e2d<\\u4ef2<\\u5b99<\\u5fe0<\\u62bd<\\u663c<\\u67f1<\\u6ce8\"\n");
  fprintf(out, "        \"<\\u866b<\\u8877<\\u8a3b<\\u914e<\\u92f3<\\u99d0<\\u6a17<\\u7026<\\u732a\"\n");
  fprintf(out, "        \"<\\u82e7<\\u8457<\\u8caf<\\u4e01<\\u5146<\\u51cb<\\u558b<\\u5bf5<\\u5e16\"\n");
  fprintf(out, "        \"<\\u5e33<\\u5e81<\\u5f14<\\u5f35<\\u5f6b<\\u5fb4<\\u61f2<\\u6311<\\u66a2\"\n");
  fprintf(out, "        \"<\\u671d<\\u6f6e<\\u7252<\\u753a<\\u773a<\\u8074<\\u8139<\\u8178<\\u8776\"\n");
  fprintf(out, "        \"<\\u8abf<\\u8adc<\\u8d85<\\u8df3<\\u929a<\\u9577<\\u9802<\\u9ce5<\\u52c5\"\n");
  fprintf(out, "        \"<\\u6357<\\u76f4<\\u6715<\\u6c88<\\u73cd<\\u8cc3<\\u93ae<\\u9673<\\u6d25\"\n");
  fprintf(out, "        \"<\\u589c<\\u690e<\\u69cc<\\u8ffd<\\u939a<\\u75db<\\u901a<\\u585a<\\u6802\"\n");
  fprintf(out, "        \"<\\u63b4<\\u69fb<\\u4f43<\\u6f2c<\\u67d8<\\u8fbb<\\u8526<\\u7db4<\\u9354\"\n");
  fprintf(out, "        \"<\\u693f<\\u6f70<\\u576a<\\u58f7<\\u5b2c<\\u7d2c<\\u722a<\\u540a<\\u91e3\"\n");
  fprintf(out, "        \"<\\u9db4<\\u4ead<\\u4f4e<\\u505c<\\u5075<\\u5243<\\u8c9e<\\u5448<\\u5824\"\n");
  fprintf(out, "        \"<\\u5b9a<\\u5e1d<\\u5e95<\\u5ead<\\u5ef7<\\u5f1f<\\u608c<\\u62b5<\\u633a\"\n");
  fprintf(out, "        \"<\\u63d0<\\u68af<\\u6c40<\\u7887<\\u798e<\\u7a0b<\\u7de0<\\u8247<\\u8a02\"\n");
  fprintf(out, "        \"<\\u8ae6<\\u8e44<\\u9013<\\u90b8<\\u912d<\\u91d8<\\u9f0e<\\u6ce5<\\u6458\"\n");
  fprintf(out, "        \"<\\u64e2<\\u6575<\\u6ef4<\\u7684<\\u7b1b<\\u9069<\\u93d1<\\u6eba<\\u54f2\"\n");
  fprintf(out, "        \"<\\u5fb9<\\u64a4<\\u8f4d<\\u8fed<\\u9244<\\u5178<\\u586b<\\u5929<\\u5c55\"\n");
  fprintf(out, "        \"<\\u5e97<\\u6dfb<\\u7e8f<\\u751c<\\u8cbc<\\u8ee2<\\u985b<\\u70b9<\\u4f1d\"\n");
  fprintf(out, "        \"<\\u6bbf<\\u6fb1<\\u7530<\\u96fb<\\u514e<\\u5410<\\u5835<\\u5857<\\u59ac\"\n");
  fprintf(out, "        \"<\\u5c60<\\u5f92<\\u6597<\\u675c<\\u6e21<\\u767b<\\u83df<\\u8ced<\\u9014\"\n");
  fprintf(out, "        \"<\\u90fd<\\u934d<\\u7825<\\u783a<\\u52aa<\\u5ea6<\\u571f<\\u5974<\\u6012\"\n");
  fprintf(out, "        \"<\\u5012<\\u515a<\\u51ac<\\u51cd<\\u5200<\\u5510<\\u5854<\\u5858<\\u5957\"\n");
  fprintf(out, "        \"<\\u5b95<\\u5cf6<\\u5d8b<\\u60bc<\\u6295<\\u642d<\\u6771<\\u6843<\\u68bc\"\n");
  fprintf(out, "        \"<\\u68df<\\u76d7<\\u6dd8<\\u6e6f<\\u6d9b<\\u706f<\\u71c8<\\u5f53<\\u75d8\"\n");
  fprintf(out, "        \"<\\u7977<\\u7b49<\\u7b54<\\u7b52<\\u7cd6<\\u7d71<\\u5230<\\u8463<\\u8569\"\n");
  fprintf(out, "        \"<\\u85e4<\\u8a0e<\\u8b04<\\u8c46<\\u8e0f<\\u9003<\\u900f<\\u9419<\\u9676\"\n");
  fprintf(out, "        \"<\\u982d<\\u9a30<\\u95d8<\\u50cd<\\u52d5<\\u540c<\\u5802<\\u5c0e<\\u61a7\"\n");
  fprintf(out, "        \"<\\u649e<\\u6d1e<\\u77b3<\\u7ae5<\\u80f4<\\u8404<\\u9053<\\u9285<\\u5ce0\"\n");
  fprintf(out, "        \"<\\u9d07<\\u533f<\\u5f97<\\u5fb3<\\u6d9c<\\u7279<\\u7763<\\u79bf<\\u7be4\"\n");
  fprintf(out, "        \"<\\u6bd2<\\u72ec<\\u8aad<\\u6803<\\u6a61<\\u51f8<\\u7a81<\\u6934<\\u5c4a\"\n");
  fprintf(out, "        \"<\\u9cf6<\\u82eb<\\u5bc5<\\u9149<\\u701e<\\u5678<\\u5c6f<\\u60c7<\\u6566\"\n");
  fprintf(out, "        \"<\\u6c8c<\\u8c5a<\\u9041<\\u9813<\\u5451<\\u66c7<\\u920d<\\u5948<\\u90a3\"\n");
  fprintf(out, "        \"<\\u5185<\\u4e4d<\\u51ea<\\u8599<\\u8b0e<\\u7058<\\u637a<\\u934b<\\u6962\"\n");
  fprintf(out, "        \"<\\u99b4<\\u7e04<\\u7577<\\u5357<\\u6960<\\u8edf<\\u96e3<\\u6c5d<\\u4e8c\"\n");
  fprintf(out, "        \"<\\u5c3c<\\u5f10<\\u8fe9<\\u5302<\\u8cd1<\\u8089<\\u8679<\\u5eff<\\u65e5\"\n");
  fprintf(out, "        \"<\\u4e73<\\u5165<\\u5982<\\u5c3f<\\u97ee<\\u4efb<\\u598a<\\u5fcd<\\u8a8d\"\n");
  fprintf(out, "        \"<\\u6fe1<\\u79b0<\\u7962<\\u5be7<\\u8471<\\u732b<\\u71b1<\\u5e74<\\u5ff5\"\n");
  fprintf(out, "        \"<\\u637b<\\u649a<\\u71c3<\\u7c98<\\u4e43<\\u5efc<\\u4e4b<\\u57dc<\\u56a2\"\n");
  fprintf(out, "        \"<\\u60a9<\\u6fc3<\\u7d0d<\\u80fd<\\u8133<\\u81bf<\\u8fb2<\\u8997<\\u86a4\"\n");
  fprintf(out, "        \"<\\u5df4<\\u628a<\\u64ad<\\u8987<\\u6777<\\u6ce2<\\u6d3e<\\u7436<\\u7834\"\n");
  fprintf(out, "        \"<\\u5a46<\\u7f75<\\u82ad<\\u99ac<\\u4ff3<\\u5ec3<\\u62dd<\\u6392<\\u6557\"\n");
  fprintf(out, "        \"<\\u676f<\\u76c3<\\u724c<\\u80cc<\\u80ba<\\u8f29<\\u914d<\\u500d<\\u57f9\"\n");
  fprintf(out, "        \"<\\u5a92<\\u6885<\\u6973<\\u7164<\\u72fd<\\u8cb7<\\u58f2<\\u8ce0<\\u966a\"\n");
  fprintf(out, "        \"<\\u9019<\\u877f<\\u79e4<\\u77e7<\\u8429<\\u4f2f<\\u5265<\\u535a<\\u62cd\"\n");
  fprintf(out, "        \"<\\u67cf<\\u6cca<\\u767d<\\u7b94<\\u7c95<\\u8236<\\u8584<\\u8feb<\\u66dd\"\n");
  fprintf(out, "        \"<\\u6f20<\\u7206<\\u7e1b<\\u83ab<\\u99c1<\\u9ea6<\\u51fd<\\u7bb1<\\u7872\"\n");
  fprintf(out, "        \"<\\u7bb8<\\u8087<\\u7b48<\\u6ae8<\\u5e61<\\u808c<\\u7551<\\u7560<\\u516b\"\n");
  fprintf(out, "        \"<\\u9262<\\u6e8c<\\u767a<\\u9197<\\u9aea<\\u4f10<\\u7f70<\\u629c<\\u7b4f\"\n");
  fprintf(out, "        \"<\\u95a5<\\u9ce9<\\u567a<\\u5859<\\u86e4<\\u96bc<\\u4f34<\\u5224<\\u534a\"\n");
  fprintf(out, "        \"<\\u53cd<\\u53db<\\u5e06<\\u642c<\\u6591<\\u677f<\\u6c3e<\\u6c4e<\\u7248\"\n");
  fprintf(out, "        \"<\\u72af<\\u73ed<\\u7554<\\u7e41<\\u822c<\\u85e9<\\u8ca9<\\u7bc4<\\u91c6\"\n");
  fprintf(out, "        \"<\\u7169<\\u9812<\\u98ef<\\u633d<\\u6669<\\u756a<\\u76e4<\\u78d0<\\u8543\"\n");
  fprintf(out, "        \"<\\u86ee<\\u532a<\\u5351<\\u5426<\\u5983<\\u5e87<\\u5f7c<\\u60b2<\\u6249\"\n");
  fprintf(out, "        \"<\\u6279<\\u62ab<\\u6590<\\u6bd4<\\u6ccc<\\u75b2<\\u76ae<\\u7891<\\u79d8\"\n");
  fprintf(out, "        \"<\\u7dcb<\\u7f77<\\u80a5<\\u88ab<\\u8ab9<\\u8cbb<\\u907f<\\u975e<\\u98db\"\n");
  fprintf(out, "        \"<\\u6a0b<\\u7c38<\\u5099<\\u5c3e<\\u5fae<\\u6787<\\u6bd8<\\u7435<\\u7709\"\n");
  fprintf(out, "        \"<\\u7f8e<\\u9f3b<\\u67ca<\\u7a17<\\u5339<\\u758b<\\u9aed<\\u5f66<\\u819d\"\n");
  fprintf(out, "        \"<\\u83f1<\\u8098<\\u5f3c<\\u5fc5<\\u7562<\\u7b46<\\u903c<\\u6867<\\u59eb\"\n");
  fprintf(out, "        \"<\\u5a9b<\\u7d10<\\u767e<\\u8b2c<\\u4ff5<\\u5f6a<\\u6a19<\\u6c37<\\u6f02\"\n");
  fprintf(out, "        \"<\\u74e2<\\u7968<\\u8868<\\u8a55<\\u8c79<\\u5edf<\\u63cf<\\u75c5<\\u79d2\"\n");
  fprintf(out, "        \"<\\u82d7<\\u9328<\\u92f2<\\u849c<\\u86ed<\\u9c2d<\\u54c1<\\u5f6c<\\u658c\"\n");
  fprintf(out, "        \"<\\u6d5c<\\u7015<\\u8ca7<\\u8cd3<\\u983b<\\u654f<\\u74f6<\\u4e0d<\\u4ed8\"\n");
  fprintf(out, "        \"<\\u57e0<\\u592b<\\u5a66<\\u5bcc<\\u51a8<\\u5e03<\\u5e9c<\\u6016<\\u6276\"\n");
  fprintf(out, "        \"<\\u6577<\\u65a7<\\u666e<\\u6d6e<\\u7236<\\u7b26<\\u8150<\\u819a<\\u8299\"\n");
  fprintf(out, "        \"<\\u8b5c<\\u8ca0<\\u8ce6<\\u8d74<\\u961c<\\u9644<\\u4fae<\\u64ab<\\u6b66\"\n");
  fprintf(out, "        \"<\\u821e<\\u8461<\\u856a<\\u90e8<\\u5c01<\\u6953<\\u98a8<\\u847a<\\u8557\"\n");
  fprintf(out, "        \"<\\u4f0f<\\u526f<\\u5fa9<\\u5e45<\\u670d<\\u798f<\\u8179<\\u8907<\\u8986\"\n");
  fprintf(out, "        \"<\\u6df5<\\u5f17<\\u6255<\\u6cb8<\\u4ecf<\\u7269<\\u9b92<\\u5206<\\u543b\"\n");
  fprintf(out, "        \"<\\u5674<\\u58b3<\\u61a4<\\u626e<\\u711a<\\u596e<\\u7c89<\\u7cde<\\u7d1b\"\n");
  fprintf(out, "        \"<\\u96f0<\\u6587<\\u805e<\\u4e19<\\u4f75<\\u5175<\\u5840<\\u5e63<\\u5e73\"\n");
  fprintf(out, "        \"<\\u5f0a<\\u67c4<\\u4e26<\\u853d<\\u9589<\\u965b<\\u7c73<\\u9801<\\u50fb\"\n");
  fprintf(out, "        \"<\\u58c1<\\u7656<\\u78a7<\\u5225<\\u77a5<\\u8511<\\u7b86<\\u504f<\\u5909\"\n");
  fprintf(out, "        \"<\\u7247<\\u7bc7<\\u7de8<\\u8fba<\\u8fd4<\\u904d<\\u4fbf<\\u52c9<\\u5a29\"\n");
  fprintf(out, "        \"<\\u5f01<\\u97ad<\\u4fdd<\\u8217<\\u92ea<\\u5703<\\u6355<\\u6b69<\\u752b\"\n");
  fprintf(out, "        \"<\\u88dc<\\u8f14<\\u7a42<\\u52df<\\u5893<\\u6155<\\u620a<\\u66ae<\\u6bcd\"\n");
  fprintf(out, "        \"<\\u7c3f<\\u83e9<\\u5023<\\u4ff8<\\u5305<\\u5446<\\u5831<\\u5949<\\u5b9d\"\n");
  fprintf(out, "        \"<\\u5cf0<\\u5cef<\\u5d29<\\u5e96<\\u62b1<\\u6367<\\u653e<\\u65b9<\\u670b\"\n");
  fprintf(out, "        \"<\\u6cd5<\\u6ce1<\\u70f9<\\u7832<\\u7e2b<\\u80de<\\u82b3<\\u840c<\\u84ec\"\n");
  fprintf(out, "        \"<\\u8702<\\u8912<\\u8a2a<\\u8c4a<\\u90a6<\\u92d2<\\u98fd<\\u9cf3<\\u9d6c\"\n");
  fprintf(out, "        \"<\\u4e4f<\\u4ea1<\\u508d<\\u5256<\\u574a<\\u59a8<\\u5e3d<\\u5fd8<\\u5fd9\"\n");
  fprintf(out, "        \"<\\u623f<\\u66b4<\\u671b<\\u67d0<\\u68d2<\\u5192<\\u7d21<\\u80aa<\\u81a8\"\n");
  fprintf(out, "        \"<\\u8b00<\\u8c8c<\\u8cbf<\\u927e<\\u9632<\\u5420<\\u982c<\\u5317<\\u50d5\"\n");
  fprintf(out, "        \"<\\u535c<\\u58a8<\\u64b2<\\u6734<\\u7267<\\u7766<\\u7a46<\\u91e6<\\u52c3\"\n");
  fprintf(out, "        \"<\\u6ca1<\\u6b86<\\u5800<\\u5e4c<\\u5954<\\u672c<\\u7ffb<\\u51e1<\\u76c6\"\n");
  fprintf(out, "        \"<\\u6469<\\u78e8<\\u9b54<\\u9ebb<\\u57cb<\\u59b9<\\u6627<\\u679a<\\u6bce\"\n");
  fprintf(out, "        \"<\\u54e9<\\u69d9<\\u5e55<\\u819c<\\u6795<\\u9baa<\\u67fe<\\u9c52<\\u685d\"\n");
  fprintf(out, "        \"<\\u4ea6<\\u4fe3<\\u53c8<\\u62b9<\\u672b<\\u6cab<\\u8fc4<\\u4fad<\\u7e6d\"\n");
  fprintf(out, "        \"<\\u9ebf<\\u4e07<\\u6162<\\u6e80<\\u6f2b<\\u8513<\\u5473<\\u672a<\\u9b45\"\n");
  fprintf(out, "        \"<\\u5df3<\\u7b95<\\u5cac<\\u5bc6<\\u871c<\\u6e4a<\\u84d1<\\u7a14<\\u8108\"\n");
  fprintf(out, "        \"<\\u5999<\\u7c8d<\\u6c11<\\u7720<\\u52d9<\\u5922<\\u7121<\\u725f<\\u77db\"\n");
  fprintf(out, "        \"<\\u9727<\\u9d61<\\u690b<\\u5a7f<\\u5a18<\\u51a5<\\u540d<\\u547d<\\u660e\"\n");
  fprintf(out, "        \"<\\u76df<\\u8ff7<\\u9298<\\u9cf4<\\u59ea<\\u725d<\\u6ec5<\\u514d<\\u68c9\"\n");
  fprintf(out, "        \"<\\u7dbf<\\u7dec<\\u9762<\\u9eba<\\u6478<\\u6a21<\\u8302<\\u5984<\\u5b5f\"\n");
  fprintf(out, "        \"<\\u6bdb<\\u731b<\\u76f2<\\u7db2<\\u8017<\\u8499<\\u5132<\\u6728<\\u9ed9\"\n");
  fprintf(out, "        \"<\\u76ee<\\u6762<\\u52ff<\\u9905<\\u5c24<\\u623b<\\u7c7e<\\u8cb0<\\u554f\"\n");
  fprintf(out, "        \"<\\u60b6<\\u7d0b<\\u9580<\\u5301<\\u4e5f<\\u51b6<\\u591c<\\u723a<\\u8036\"\n");
  fprintf(out, "        \"<\\u91ce<\\u5f25<\\u77e2<\\u5384<\\u5f79<\\u7d04<\\u85ac<\\u8a33<\\u8e8d\"\n");
  fprintf(out, "        \"<\\u9756<\\u67f3<\\u85ae<\\u9453<\\u6109<\\u6108<\\u6cb9<\\u7652<\\u8aed\"\n");
  fprintf(out, "        \"<\\u8f38<\\u552f<\\u4f51<\\u512a<\\u52c7<\\u53cb<\\u5ba5<\\u5e7d<\\u60a0\"\n");
  fprintf(out, "        \"<\\u6182<\\u63d6<\\u6709<\\u67da<\\u6e67<\\u6d8c<\\u7336<\\u7337<\\u7531\"\n");
  fprintf(out, "        \"<\\u7950<\\u88d5<\\u8a98<\\u904a<\\u9091<\\u90f5<\\u96c4<\\u878d<\\u5915\"\n");
  fprintf(out, "        \"<\\u4e88<\\u4f59<\\u4e0e<\\u8a89<\\u8f3f<\\u9810<\\u50ad<\\u5e7c<\\u5996\"\n");
  fprintf(out, "        \"<\\u5bb9<\\u5eb8<\\u63da<\\u63fa<\\u64c1<\\u66dc<\\u694a<\\u69d8<\\u6d0b\"\n");
  fprintf(out, "        \"<\\u6eb6<\\u7194<\\u7528<\\u7aaf<\\u7f8a<\\u8000<\\u8449<\\u84c9<\\u8981\"\n");
  fprintf(out, "        \"<\\u8b21<\\u8e0a<\\u9065<\\u967d<\\u990a<\\u617e<\\u6291<\\u6b32<\\u6c83\"\n");
  fprintf(out, "        \"<\\u6d74<\\u7fcc<\\u7ffc<\\u6dc0<\\u7f85<\\u87ba<\\u88f8<\\u6765<\\u83b1\"\n");
  fprintf(out, "        \"<\\u983c<\\u96f7<\\u6d1b<\\u7d61<\\u843d<\\u916a<\\u4e71<\\u5375<\\u5d50\"\n");
  fprintf(out, "        \"<\\u6b04<\\u6feb<\\u85cd<\\u862d<\\u89a7<\\u5229<\\u540f<\\u5c65<\\u674e\"\n");
  fprintf(out, "        \"<\\u68a8<\\u7406<\\u7483<\\u75e2<\\u88cf<\\u88e1<\\u91cc<\\u96e2<\\u9678\"\n");
  fprintf(out, "        \"<\\u5f8b<\\u7387<\\u7acb<\\u844e<\\u63a0<\\u7565<\\u5289<\\u6d41<\\u6e9c\"\n");
  fprintf(out, "        \"<\\u7409<\\u7559<\\u786b<\\u7c92<\\u9686<\\u7adc<\\u9f8d<\\u4fb6<\\u616e\"\n");
  fprintf(out, "        \"<\\u65c5<\\u865c<\\u4e86<\\u4eae<\\u50da<\\u4e21<\\u51cc<\\u5bee<\\u6599\"\n");
  fprintf(out, "        \"<\\u6881<\\u6dbc<\\u731f<\\u7642<\\u77ad<\\u7a1c<\\u7ce7<\\u826f<\\u8ad2\"\n");
  fprintf(out, "        \"<\\u907c<\\u91cf<\\u9675<\\u9818<\\u529b<\\u7dd1<\\u502b<\\u5398<\\u6797\"\n");
  fprintf(out, "        \"<\\u6dcb<\\u71d0<\\u7433<\\u81e8<\\u8f2a<\\u96a3<\\u9c57<\\u9e9f<\\u7460\"\n");
  fprintf(out, "        \"<\\u5841<\\u6d99<\\u7d2f<\\u985e<\\u4ee4<\\u4f36<\\u4f8b<\\u51b7<\\u52b1\"\n");
  fprintf(out, "        \"<\\u5dba<\\u601c<\\u73b2<\\u793c<\\u82d3<\\u9234<\\u96b7<\\u96f6<\\u970a\"\n");
  fprintf(out, "        \"<\\u9e97<\\u9f62<\\u66a6<\\u6b74<\\u5217<\\u52a3<\\u70c8<\\u88c2<\\u5ec9\"\n");
  fprintf(out, "        \"<\\u604b<\\u6190<\\u6f23<\\u7149<\\u7c3e<\\u7df4<\\u806f<\\u84ee<\\u9023\"\n");
  fprintf(out, "        \"<\\u932c<\\u5442<\\u9b6f<\\u6ad3<\\u7089<\\u8cc2<\\u8def<\\u9732<\\u52b4\"\n");
  fprintf(out, "        \"<\\u5a41<\\u5eca<\\u5f04<\\u6717<\\u697c<\\u6994<\\u6d6a<\\u6f0f<\\u7262\"\n");
  fprintf(out, "        \"<\\u72fc<\\u7bed<\\u8001<\\u807e<\\u874b<\\u90ce<\\u516d<\\u9e93<\\u7984\"\n");
  fprintf(out, "        \"<\\u808b<\\u9332<\\u8ad6<\\u502d<\\u548c<\\u8a71<\\u6b6a<\\u8cc4<\\u8107\"\n");
  fprintf(out, "        \"<\\u60d1<\\u67a0<\\u9df2<\\u4e99<\\u4e98<\\u9c10<\\u8a6b<\\u85c1<\\u8568\"\n");
  fprintf(out, "        \"<\\u6900<\\u6e7e<\\u7897<\\u8155<\\u5f0c<\\u4e10<\\u4e15<\\u4e2a<\\u4e31\"\n");
  fprintf(out, "        \"<\\u4e36<\\u4e3c<\\u4e3f<\\u4e42<\\u4e56<\\u4e58<\\u4e82<\\u4e85<\\u8c6b\"\n");
  fprintf(out, "        \"<\\u4e8a<\\u8212<\\u5f0d<\\u4e8e<\\u4e9e<\\u4e9f<\\u4ea0<\\u4ea2<\\u4eb0\"\n");
  fprintf(out, "        \"<\\u4eb3<\\u4eb6<\\u4ece<\\u4ecd<\\u4ec4<\\u4ec6<\\u4ec2<\\u4ed7<\\u4ede\"\n");
  fprintf(out, "        \"<\\u4eed<\\u4edf<\\u4ef7<\\u4f09<\\u4f5a<\\u4f30<\\u4f5b<\\u4f5d<\\u4f57\"\n");
  fprintf(out, "        \"<\\u4f47<\\u4f76<\\u4f88<\\u4f8f<\\u4f98<\\u4f7b<\\u4f69<\\u4f70<\\u4f91\"\n");
  fprintf(out, "        \"<\\u4f6f<\\u4f86<\\u4f96<\\u5118<\\u4fd4<\\u4fdf<\\u4fce<\\u4fd8<\\u4fdb\"\n");
  fprintf(out, "        \"<\\u4fd1<\\u4fda<\\u4fd0<\\u4fe4<\\u4fe5<\\u501a<\\u5028<\\u5014<\\u502a\"\n");
  fprintf(out, "        \"<\\u5025<\\u5005<\\u4f1c<\\u4ff6<\\u5021<\\u5029<\\u502c<\\u4ffe<\\u4fef\"\n");
  fprintf(out, "        \"<\\u5011<\\u5006<\\u5043<\\u5047<\\u6703<\\u5055<\\u5050<\\u5048<\\u505a\"\n");
  fprintf(out, "        \"<\\u5056<\\u506c<\\u5078<\\u5080<\\u509a<\\u5085<\\u50b4<\\u50b2<\\u50c9\"\n");
  fprintf(out, "        \"<\\u50ca<\\u50b3<\\u50c2<\\u50d6<\\u50de<\\u50e5<\\u50ed<\\u50e3<\\u50ee\"\n");
  fprintf(out, "        \"<\\u50f9<\\u50f5<\\u5109<\\u5101<\\u5102<\\u5116<\\u5115<\\u5114<\\u511a\"\n");
  fprintf(out, "        \"<\\u5121<\\u513a<\\u5137<\\u513c<\\u513b<\\u513f<\\u5140<\\u5152<\\u514c\"\n");
  fprintf(out, "        \"<\\u5154<\\u5162<\\u7af8<\\u5169<\\u516a<\\u516e<\\u5180<\\u5182<\\u56d8\"\n");
  fprintf(out, "        \"<\\u518c<\\u5189<\\u518f<\\u5191<\\u5193<\\u5195<\\u5196<\\u51a4<\\u51a6\"\n");
  fprintf(out, "        \"<\\u51a2<\\u51a9<\\u51aa<\\u51ab<\\u51b3<\\u51b1<\\u51b2<\\u51b0<\\u51b5\"\n");
  fprintf(out, "        \"<\\u51bd<\\u51c5<\\u51c9<\\u51db<\\u51e0<\\u8655<\\u51e9<\\u51ed<\\u51f0\"\n");
  fprintf(out, "        \"<\\u51f5<\\u51fe<\\u5204<\\u520b<\\u5214<\\u520e<\\u5227<\\u522a<\\u522e\"\n");
  fprintf(out, "        \"<\\u5233<\\u5239<\\u524f<\\u5244<\\u524b<\\u524c<\\u525e<\\u5254<\\u526a\"\n");
  fprintf(out, "        \"<\\u5274<\\u5269<\\u5273<\\u527f<\\u527d<\\u528d<\\u5294<\\u5292<\\u5271\"\n");
  fprintf(out, "        \"<\\u5288<\\u5291<\\u8fa8<\\u8fa7<\\u52ac<\\u52ad<\\u52bc<\\u52b5<\\u52c1\"\n");
  fprintf(out, "        \"<\\u52cd<\\u52d7<\\u52de<\\u52e3<\\u52e6<\\u98ed<\\u52e0<\\u52f3<\\u52f5\"\n");
  fprintf(out, "        \"<\\u52f8<\\u52f9<\\u5306<\\u5308<\\u7538<\\u530d<\\u5310<\\u530f<\\u5315\"\n");
  fprintf(out, "        \"<\\u531a<\\u5323<\\u532f<\\u5331<\\u5333<\\u5338<\\u5340<\\u5346<\\u5345\"\n");
  fprintf(out, "        \"<\\u4e17<\\u5349<\\u534d<\\u51d6<\\u535e<\\u5369<\\u536e<\\u5918<\\u537b\"\n");
  fprintf(out, "        \"<\\u5377<\\u5382<\\u5396<\\u53a0<\\u53a6<\\u53a5<\\u53ae<\\u53b0<\\u53b6\"\n");
  fprintf(out, "        \"<\\u53c3<\\u7c12<\\u96d9<\\u53df<\\u66fc<\\u71ee<\\u53ee<\\u53e8<\\u53ed\"\n");
  fprintf(out, "        \"<\\u53fa<\\u5401<\\u543d<\\u5440<\\u542c<\\u542d<\\u543c<\\u542e<\\u5436\"\n");
  fprintf(out, "        \"<\\u5429<\\u541d<\\u544e<\\u548f<\\u5475<\\u548e<\\u545f<\\u5471<\\u5477\"\n");
  fprintf(out, "        \"<\\u5470<\\u5492<\\u547b<\\u5480<\\u5476<\\u5484<\\u5490<\\u5486<\\u54c7\"\n");
  fprintf(out, "        \"<\\u54a2<\\u54b8<\\u54a5<\\u54ac<\\u54c4<\\u54c8<\\u54a8<\\u54ab<\\u54c2\"\n");
  fprintf(out, "        \"<\\u54a4<\\u54be<\\u54bc<\\u54d8<\\u54e5<\\u54e6<\\u550f<\\u5514<\\u54fd\"\n");
  fprintf(out, "        \"<\\u54ee<\\u54ed<\\u54fa<\\u54e2<\\u5539<\\u5540<\\u5563<\\u554c<\\u552e\"\n");
  fprintf(out, "        \"<\\u555c<\\u5545<\\u5556<\\u5557<\\u5538<\\u5533<\\u555d<\\u5599<\\u5580\"\n");
  fprintf(out, "        \"<\\u54af<\\u558a<\\u559f<\\u557b<\\u557e<\\u5598<\\u559e<\\u55ae<\\u557c\"\n");
  fprintf(out, "        \"<\\u5583<\\u55a9<\\u5587<\\u55a8<\\u55da<\\u55c5<\\u55df<\\u55c4<\\u55dc\"\n");
  fprintf(out, "        \"<\\u55e4<\\u55d4<\\u5614<\\u55f7<\\u5616<\\u55fe<\\u55fd<\\u561b<\\u55f9\"\n");
  fprintf(out, "        \"<\\u564e<\\u5650<\\u71df<\\u5634<\\u5636<\\u5632<\\u5638<\\u566b<\\u5664\"\n");
  fprintf(out, "        \"<\\u562f<\\u566c<\\u566a<\\u5686<\\u5680<\\u568a<\\u56a0<\\u5694<\\u568f\"\n");
  fprintf(out, "        \"<\\u56a5<\\u56ae<\\u56b6<\\u56b4<\\u56c2<\\u56bc<\\u56c1<\\u56c3<\\u56c0\"\n");
  fprintf(out, "        \"<\\u56c8<\\u56ce<\\u56d1<\\u56d3<\\u56d7<\\u56ee<\\u56f9<\\u5700<\\u56ff\"\n");
  fprintf(out, "        \"<\\u5704<\\u5709<\\u5708<\\u570b<\\u570d<\\u5713<\\u5718<\\u5716<\\u55c7\"\n");
  fprintf(out, "        \"<\\u571c<\\u5726<\\u5737<\\u5738<\\u574e<\\u573b<\\u5740<\\u574f<\\u5769\"\n");
  fprintf(out, "        \"<\\u57c0<\\u5788<\\u5761<\\u577f<\\u5789<\\u5793<\\u57a0<\\u57b3<\\u57a4\"\n");
  fprintf(out, "        \"<\\u57aa<\\u57b0<\\u57c3<\\u57c6<\\u57d4<\\u57d2<\\u57d3<\\u580a<\\u57d6\"\n");
  fprintf(out, "        \"<\\u57e3<\\u580b<\\u5819<\\u581d<\\u5872<\\u5821<\\u5862<\\u584b<\\u5870\"\n");
  fprintf(out, "        \"<\\u6bc0<\\u5852<\\u583d<\\u5879<\\u5885<\\u58b9<\\u589f<\\u58ab<\\u58ba\"\n");
  fprintf(out, "        \"<\\u58de<\\u58bb<\\u58b8<\\u58ae<\\u58c5<\\u58d3<\\u58d1<\\u58d7<\\u58d9\"\n");
  fprintf(out, "        \"<\\u58d8<\\u58e5<\\u58dc<\\u58e4<\\u58df<\\u58ef<\\u58fa<\\u58f9<\\u58fb\"\n");
  fprintf(out, "        \"<\\u58fc<\\u58fd<\\u5902<\\u590a<\\u5910<\\u591b<\\u68a6<\\u5925<\\u592c\"\n");
  fprintf(out, "        \"<\\u592d<\\u5932<\\u5938<\\u593e<\\u7ad2<\\u5955<\\u5950<\\u594e<\\u595a\"\n");
  fprintf(out, "        \"<\\u5958<\\u5962<\\u5960<\\u5967<\\u596c<\\u5969<\\u5978<\\u5981<\\u599d\"\n");
  fprintf(out, "        \"<\\u4f5e<\\u4fab<\\u59a3<\\u59b2<\\u59c6<\\u59e8<\\u59dc<\\u598d<\\u59d9\"\n");
  fprintf(out, "        \"<\\u59da<\\u5a25<\\u5a1f<\\u5a11<\\u5a1c<\\u5a09<\\u5a1a<\\u5a40<\\u5a6c\"\n");
  fprintf(out, "        \"<\\u5a49<\\u5a35<\\u5a36<\\u5a62<\\u5a6a<\\u5a9a<\\u5abc<\\u5abe<\\u5acb\"\n");
  fprintf(out, "        \"<\\u5ac2<\\u5abd<\\u5ae3<\\u5ad7<\\u5ae6<\\u5ae9<\\u5ad6<\\u5afa<\\u5afb\"\n");
  fprintf(out, "        \"<\\u5b0c<\\u5b0b<\\u5b16<\\u5b32<\\u5ad0<\\u5b2a<\\u5b36<\\u5b3e<\\u5b43\"\n");
  fprintf(out, "        \"<\\u5b45<\\u5b40<\\u5b51<\\u5b55<\\u5b5a<\\u5b5b<\\u5b65<\\u5b69<\\u5b70\"\n");
  fprintf(out, "        \"<\\u5b73<\\u5b75<\\u5b78<\\u6588<\\u5b7a<\\u5b80<\\u5b83<\\u5ba6<\\u5bb8\"\n");
  fprintf(out, "        \"<\\u5bc3<\\u5bc7<\\u5bc9<\\u5bd4<\\u5bd0<\\u5be4<\\u5be6<\\u5be2<\\u5bde\"\n");
  fprintf(out, "        \"<\\u5be5<\\u5beb<\\u5bf0<\\u5bf6<\\u5bf3<\\u5c05<\\u5c07<\\u5c08<\\u5c0d\"\n");
  fprintf(out, "        \"<\\u5c13<\\u5c20<\\u5c22<\\u5c28<\\u5c38<\\u5c39<\\u5c41<\\u5c46<\\u5c4e\"\n");
  fprintf(out, "        \"<\\u5c53<\\u5c50<\\u5c4f<\\u5b71<\\u5c6c<\\u5c6e<\\u4e62<\\u5c76<\\u5c79\"\n");
  fprintf(out, "        \"<\\u5c8c<\\u5c91<\\u5c94<\\u599b<\\u5cab<\\u5cbb<\\u5cb6<\\u5cbc<\\u5cb7\"\n");
  fprintf(out, "        \"<\\u5cc5<\\u5cbe<\\u5cc7<\\u5cd9<\\u5ce9<\\u5cfd<\\u5cfa<\\u5ced<\\u5d8c\"\n");
  fprintf(out, "        \"<\\u5cea<\\u5d0b<\\u5d15<\\u5d17<\\u5d5c<\\u5d1f<\\u5d1b<\\u5d11<\\u5d14\"\n");
  fprintf(out, "        \"<\\u5d22<\\u5d1a<\\u5d19<\\u5d18<\\u5d4c<\\u5d52<\\u5d4e<\\u5d4b<\\u5d6c\"\n");
  fprintf(out, "        \"<\\u5d73<\\u5d76<\\u5d87<\\u5d84<\\u5d82<\\u5da2<\\u5d9d<\\u5dac<\\u5dae\"\n");
  fprintf(out, "        \"<\\u5dbd<\\u5d90<\\u5db7<\\u5dbc<\\u5dc9<\\u5dcd<\\u5dd3<\\u5dd2<\\u5dd6\"\n");
  fprintf(out, "        \"<\\u5ddb<\\u5deb<\\u5df2<\\u5df5<\\u5e0b<\\u5e1a<\\u5e19<\\u5e11<\\u5e1b\"\n");
  fprintf(out, "        \"<\\u5e36<\\u5e37<\\u5e44<\\u5e43<\\u5e40<\\u5e4e<\\u5e57<\\u5e54<\\u5e5f\"\n");
  fprintf(out, "        \"<\\u5e62<\\u5e64<\\u5e47<\\u5e75<\\u5e76<\\u5e7a<\\u9ebc<\\u5e7f<\\u5ea0\"\n");
  fprintf(out, "        \"<\\u5ec1<\\u5ec2<\\u5ec8<\\u5ed0<\\u5ecf<\\u5ed6<\\u5ee3<\\u5edd<\\u5eda\"\n");
  fprintf(out, "        \"<\\u5edb<\\u5ee2<\\u5ee1<\\u5ee8<\\u5ee9<\\u5eec<\\u5ef1<\\u5ef3<\\u5ef0\"\n");
  fprintf(out, "        \"<\\u5ef4<\\u5ef8<\\u5efe<\\u5f03<\\u5f09<\\u5f5d<\\u5f5c<\\u5f0b<\\u5f11\"\n");
  fprintf(out, "        \"<\\u5f16<\\u5f29<\\u5f2d<\\u5f38<\\u5f41<\\u5f48<\\u5f4c<\\u5f4e<\\u5f2f\"\n");
  fprintf(out, "        \"<\\u5f51<\\u5f56<\\u5f57<\\u5f59<\\u5f61<\\u5f6d<\\u5f73<\\u5f77<\\u5f83\"\n");
  fprintf(out, "        \"<\\u5f82<\\u5f7f<\\u5f8a<\\u5f88<\\u5f91<\\u5f87<\\u5f9e<\\u5f99<\\u5f98\"\n");
  fprintf(out, "        \"<\\u5fa0<\\u5fa8<\\u5fad<\\u5fbc<\\u5fd6<\\u5ffb<\\u5fe4<\\u5ff8<\\u5ff1\"\n");
  fprintf(out, "        \"<\\u5fdd<\\u60b3<\\u5fff<\\u6021<\\u6060<\\u6019<\\u6010<\\u6029<\\u600e\"\n");
  fprintf(out, "        \"<\\u6031<\\u601b<\\u6015<\\u602b<\\u6026<\\u600f<\\u603a<\\u605a<\\u6041\"\n");
  fprintf(out, "        \"<\\u606a<\\u6077<\\u605f<\\u604a<\\u6046<\\u604d<\\u6063<\\u6043<\\u6064\"\n");
  fprintf(out, "        \"<\\u6042<\\u606c<\\u606b<\\u6059<\\u6081<\\u608d<\\u60e7<\\u6083<\\u609a\"\n");
  fprintf(out, "        \"<\\u6084<\\u609b<\\u6096<\\u6097<\\u6092<\\u60a7<\\u608b<\\u60e1<\\u60b8\"\n");
  fprintf(out, "        \"<\\u60e0<\\u60d3<\\u60b4<\\u5ff0<\\u60bd<\\u60c6<\\u60b5<\\u60d8<\\u614d\"\n");
  fprintf(out, "        \"<\\u6115<\\u6106<\\u60f6<\\u60f7<\\u6100<\\u60f4<\\u60fa<\\u6103<\\u6121\"\n");
  fprintf(out, "        \"<\\u60fb<\\u60f1<\\u610d<\\u610e<\\u6147<\\u613e<\\u6128<\\u6127<\\u614a\"\n");
  fprintf(out, "        \"<\\u613f<\\u613c<\\u612c<\\u6134<\\u613d<\\u6142<\\u6144<\\u6173<\\u6177\"\n");
  fprintf(out, "        \"<\\u6158<\\u6159<\\u615a<\\u616b<\\u6174<\\u616f<\\u6165<\\u6171<\\u615f\"\n");
  fprintf(out, "        \"<\\u615d<\\u6153<\\u6175<\\u6199<\\u6196<\\u6187<\\u61ac<\\u6194<\\u619a\"\n");
  fprintf(out, "        \"<\\u618a<\\u6191<\\u61ab<\\u61ae<\\u61cc<\\u61ca<\\u61c9<\\u61f7<\\u61c8\"\n");
  fprintf(out, "        \"<\\u61c3<\\u61c6<\\u61ba<\\u61cb<\\u7f79<\\u61cd<\\u61e6<\\u61e3<\\u61f6\"\n");
  fprintf(out, "        \"<\\u61fa<\\u61f4<\\u61ff<\\u61fd<\\u61fc<\\u61fe<\\u6200<\\u6208<\\u6209\"\n");
  fprintf(out, "        \"<\\u620d<\\u620c<\\u6214<\\u621b<\\u621e<\\u6221<\\u622a<\\u622e<\\u6230\"\n");
  fprintf(out, "        \"<\\u6232<\\u6233<\\u6241<\\u624e<\\u625e<\\u6263<\\u625b<\\u6260<\\u6268\"\n");
  fprintf(out, "        \"<\\u627c<\\u6282<\\u6289<\\u627e<\\u6292<\\u6293<\\u6296<\\u62d4<\\u6283\"\n");
  fprintf(out, "        \"<\\u6294<\\u62d7<\\u62d1<\\u62bb<\\u62cf<\\u62ff<\\u62c6<\\u64d4<\\u62c8\"\n");
  fprintf(out, "        \"<\\u62dc<\\u62cc<\\u62ca<\\u62c2<\\u62c7<\\u629b<\\u62c9<\\u630c<\\u62ee\"\n");
  fprintf(out, "        \"<\\u62f1<\\u6327<\\u6302<\\u6308<\\u62ef<\\u62f5<\\u6350<\\u633e<\\u634d\"\n");
  fprintf(out, "        \"<\\u641c<\\u634f<\\u6396<\\u638e<\\u6380<\\u63ab<\\u6376<\\u63a3<\\u638f\"\n");
  fprintf(out, "        \"<\\u6389<\\u639f<\\u63b5<\\u636b<\\u6369<\\u63be<\\u63e9<\\u63c0<\\u63c6\"\n");
  fprintf(out, "        \"<\\u63e3<\\u63c9<\\u63d2<\\u63f6<\\u63c4<\\u6416<\\u6434<\\u6406<\\u6413\"\n");
  fprintf(out, "        \"<\\u6426<\\u6436<\\u651d<\\u6417<\\u6428<\\u640f<\\u6467<\\u646f<\\u6476\"\n");
  fprintf(out, "        \"<\\u644e<\\u652a<\\u6495<\\u6493<\\u64a5<\\u64a9<\\u6488<\\u64bc<\\u64da\"\n");
  fprintf(out, "        \"<\\u64d2<\\u64c5<\\u64c7<\\u64bb<\\u64d8<\\u64c2<\\u64f1<\\u64e7<\\u8209\"\n");
  fprintf(out, "        \"<\\u64e0<\\u64e1<\\u62ac<\\u64e3<\\u64ef<\\u652c<\\u64f6<\\u64f4<\\u64f2\"\n");
  fprintf(out, "        \"<\\u64fa<\\u6500<\\u64fd<\\u6518<\\u651c<\\u6505<\\u6524<\\u6523<\\u652b\"\n");
  fprintf(out, "        \"<\\u6534<\\u6535<\\u6537<\\u6536<\\u6538<\\u754b<\\u6548<\\u6556<\\u6555\"\n");
  fprintf(out, "        \"<\\u654d<\\u6558<\\u655e<\\u655d<\\u6572<\\u6578<\\u6582<\\u6583<\\u8b8a\"\n");
  fprintf(out, "        \"<\\u659b<\\u659f<\\u65ab<\\u65b7<\\u65c3<\\u65c6<\\u65c1<\\u65c4<\\u65cc\"\n");
  fprintf(out, "        \"<\\u65d2<\\u65db<\\u65d9<\\u65e0<\\u65e1<\\u65f1<\\u6772<\\u660a<\\u6603\"\n");
  fprintf(out, "        \"<\\u65fb<\\u6773<\\u6635<\\u6636<\\u6634<\\u661c<\\u664f<\\u6644<\\u6649\"\n");
  fprintf(out, "        \"<\\u6641<\\u665e<\\u665d<\\u6664<\\u6667<\\u6668<\\u665f<\\u6662<\\u6670\"\n");
  fprintf(out, "        \"<\\u6683<\\u6688<\\u668e<\\u6689<\\u6684<\\u6698<\\u669d<\\u66c1<\\u66b9\"\n");
  fprintf(out, "        \"<\\u66c9<\\u66be<\\u66bc<\\u66c4<\\u66b8<\\u66d6<\\u66da<\\u66e0<\\u663f\"\n");
  fprintf(out, "        \"<\\u66e6<\\u66e9<\\u66f0<\\u66f5<\\u66f7<\\u670f<\\u6716<\\u671e<\\u6726\"\n");
  fprintf(out, "        \"<\\u6727<\\u9738<\\u672e<\\u673f<\\u6736<\\u6741<\\u6738<\\u6737<\\u6746\"\n");
  fprintf(out, "        \"<\\u675e<\\u6760<\\u6759<\\u6763<\\u6764<\\u6789<\\u6770<\\u67a9<\\u677c\"\n");
  fprintf(out, "        \"<\\u676a<\\u678c<\\u678b<\\u67a6<\\u67a1<\\u6785<\\u67b7<\\u67ef<\\u67b4\"\n");
  fprintf(out, "        \"<\\u67ec<\\u67b3<\\u67e9<\\u67b8<\\u67e4<\\u67de<\\u67dd<\\u67e2<\\u67ee\"\n");
  fprintf(out, "        \"<\\u67b9<\\u67ce<\\u67c6<\\u67e7<\\u6a9c<\\u681e<\\u6846<\\u6829<\\u6840\"\n");
  fprintf(out, "        \"<\\u684d<\\u6832<\\u684e<\\u68b3<\\u682b<\\u6859<\\u6863<\\u6877<\\u687f\"\n");
  fprintf(out, "        \"<\\u689f<\\u688f<\\u68ad<\\u6894<\\u689d<\\u689b<\\u6883<\\u6aae<\\u68b9\"\n");
  fprintf(out, "        \"<\\u6874<\\u68b5<\\u68a0<\\u68ba<\\u690f<\\u688d<\\u687e<\\u6901<\\u68ca\"\n");
  fprintf(out, "        \"<\\u6908<\\u68d8<\\u6922<\\u6926<\\u68e1<\\u690c<\\u68cd<\\u68d4<\\u68e7\"\n");
  fprintf(out, "        \"<\\u68d5<\\u6936<\\u6912<\\u6904<\\u68d7<\\u68e3<\\u6925<\\u68f9<\\u68e0\"\n");
  fprintf(out, "        \"<\\u68ef<\\u6928<\\u692a<\\u691a<\\u6923<\\u6921<\\u68c6<\\u6979<\\u6977\"\n");
  fprintf(out, "        \"<\\u695c<\\u6978<\\u696b<\\u6954<\\u697e<\\u696e<\\u6939<\\u6974<\\u693d\"\n");
  fprintf(out, "        \"<\\u6959<\\u6930<\\u6961<\\u695e<\\u695d<\\u6981<\\u696a<\\u69b2<\\u69ae\"\n");
  fprintf(out, "        \"<\\u69d0<\\u69bf<\\u69c1<\\u69d3<\\u69be<\\u69ce<\\u5be8<\\u69ca<\\u69dd\"\n");
  fprintf(out, "        \"<\\u69bb<\\u69c3<\\u69a7<\\u6a2e<\\u6991<\\u69a0<\\u699c<\\u6995<\\u69b4\"\n");
  fprintf(out, "        \"<\\u69de<\\u69e8<\\u6a02<\\u6a1b<\\u69ff<\\u6b0a<\\u69f9<\\u69f2<\\u69e7\"\n");
  fprintf(out, "        \"<\\u6a05<\\u69b1<\\u6a1e<\\u69ed<\\u6a14<\\u69eb<\\u6a0a<\\u6a12<\\u6ac1\"\n");
  fprintf(out, "        \"<\\u6a23<\\u6a13<\\u6a44<\\u6a0c<\\u6a72<\\u6a36<\\u6a78<\\u6a47<\\u6a62\"\n");
  fprintf(out, "        \"<\\u6a59<\\u6a66<\\u6a48<\\u6a38<\\u6a22<\\u6a90<\\u6a8d<\\u6aa0<\\u6a84\"\n");
  fprintf(out, "        \"<\\u6aa2<\\u6aa3<\\u6a97<\\u8617<\\u6abb<\\u6ac3<\\u6ac2<\\u6ab8<\\u6ab3\"\n");
  fprintf(out, "        \"<\\u6aac<\\u6ade<\\u6ad1<\\u6adf<\\u6aaa<\\u6ada<\\u6aea<\\u6afb<\\u6b05\"\n");
  fprintf(out, "        \"<\\u8616<\\u6afa<\\u6b12<\\u6b16<\\u9b31<\\u6b1f<\\u6b38<\\u6b37<\\u76dc\"\n");
  fprintf(out, "        \"<\\u6b39<\\u98ee<\\u6b47<\\u6b43<\\u6b49<\\u6b50<\\u6b59<\\u6b54<\\u6b5b\"\n");
  fprintf(out, "        \"<\\u6b5f<\\u6b61<\\u6b78<\\u6b79<\\u6b7f<\\u6b80<\\u6b84<\\u6b83<\\u6b8d\"\n");
  fprintf(out, "        \"<\\u6b98<\\u6b95<\\u6b9e<\\u6ba4<\\u6baa<\\u6bab<\\u6baf<\\u6bb2<\\u6bb1\"\n");
  fprintf(out, "        \"<\\u6bb3<\\u6bb7<\\u6bbc<\\u6bc6<\\u6bcb<\\u6bd3<\\u6bdf<\\u6bec<\\u6beb\"\n");
  fprintf(out, "        \"<\\u6bf3<\\u6bef<\\u9ebe<\\u6c08<\\u6c13<\\u6c14<\\u6c1b<\\u6c24<\\u6c23\"\n");
  fprintf(out, "        \"<\\u6c5e<\\u6c55<\\u6c62<\\u6c6a<\\u6c82<\\u6c8d<\\u6c9a<\\u6c81<\\u6c9b\"\n");
  fprintf(out, "        \"<\\u6c7e<\\u6c68<\\u6c73<\\u6c92<\\u6c90<\\u6cc4<\\u6cf1<\\u6cd3<\\u6cbd\"\n");
  fprintf(out, "        \"<\\u6cd7<\\u6cc5<\\u6cdd<\\u6cae<\\u6cb1<\\u6cbe<\\u6cba<\\u6cdb<\\u6cef\"\n");
  fprintf(out, "        \"<\\u6cd9<\\u6cea<\\u6d1f<\\u884d<\\u6d36<\\u6d2b<\\u6d3d<\\u6d38<\\u6d19\"\n");
  fprintf(out, "        \"<\\u6d35<\\u6d33<\\u6d12<\\u6d0c<\\u6d63<\\u6d93<\\u6d64<\\u6d5a<\\u6d79\"\n");
  fprintf(out, "        \"<\\u6d59<\\u6d8e<\\u6d95<\\u6fe4<\\u6d85<\\u6df9<\\u6e15<\\u6e0a<\\u6db5\"\n");
  fprintf(out, "        \"<\\u6dc7<\\u6de6<\\u6db8<\\u6dc6<\\u6dec<\\u6dde<\\u6dcc<\\u6de8<\\u6dd2\"\n");
  fprintf(out, "        \"<\\u6dc5<\\u6dfa<\\u6dd9<\\u6de4<\\u6dd5<\\u6dea<\\u6dee<\\u6e2d<\\u6e6e\"\n");
  fprintf(out, "        \"<\\u6e2e<\\u6e19<\\u6e72<\\u6e5f<\\u6e3e<\\u6e23<\\u6e6b<\\u6e2b<\\u6e76\"\n");
  fprintf(out, "        \"<\\u6e4d<\\u6e1f<\\u6e43<\\u6e3a<\\u6e4e<\\u6e24<\\u6eff<\\u6e1d<\\u6e38\"\n");
  fprintf(out, "        \"<\\u6e82<\\u6eaa<\\u6e98<\\u6ec9<\\u6eb7<\\u6ed3<\\u6ebd<\\u6eaf<\\u6ec4\"\n");
  fprintf(out, "        \"<\\u6eb2<\\u6ed4<\\u6ed5<\\u6e8f<\\u6ea5<\\u6ec2<\\u6e9f<\\u6f41<\\u6f11\"\n");
  fprintf(out, "        \"<\\u704c<\\u6eec<\\u6ef8<\\u6efe<\\u6f3f<\\u6ef2<\\u6f31<\\u6eef<\\u6f32\"\n");
  fprintf(out, "        \"<\\u6ecc<\\u6f3e<\\u6f13<\\u6ef7<\\u6f86<\\u6f7a<\\u6f78<\\u6f81<\\u6f80\"\n");
  fprintf(out, "        \"<\\u6f6f<\\u6f5b<\\u6ff3<\\u6f6d<\\u6f82<\\u6f7c<\\u6f58<\\u6f8e<\\u6f91\"\n");
  fprintf(out, "        \"<\\u6fc2<\\u6f66<\\u6fb3<\\u6fa3<\\u6fa1<\\u6fa4<\\u6fb9<\\u6fc6<\\u6faa\"\n");
  fprintf(out, "        \"<\\u6fdf<\\u6fd5<\\u6fec<\\u6fd4<\\u6fd8<\\u6ff1<\\u6fee<\\u6fdb<\\u7009\"\n");
  fprintf(out, "        \"<\\u700b<\\u6ffa<\\u7011<\\u7001<\\u700f<\\u6ffe<\\u701b<\\u701a<\\u6f74\"\n");
  fprintf(out, "        \"<\\u701d<\\u7018<\\u701f<\\u7030<\\u703e<\\u7032<\\u7051<\\u7063<\\u7099\"\n");
  fprintf(out, "        \"<\\u7092<\\u70af<\\u70f1<\\u70ac<\\u70b8<\\u70b3<\\u70ae<\\u70df<\\u70cb\"\n");
  fprintf(out, "        \"<\\u70dd<\\u70d9<\\u7109<\\u70fd<\\u711c<\\u7119<\\u7165<\\u7155<\\u7188\"\n");
  fprintf(out, "        \"<\\u7166<\\u7162<\\u714c<\\u7156<\\u716c<\\u718f<\\u71fb<\\u7184<\\u7195\"\n");
  fprintf(out, "        \"<\\u71a8<\\u71ac<\\u71d7<\\u71b9<\\u71be<\\u71d2<\\u71c9<\\u71d4<\\u71ce\"\n");
  fprintf(out, "        \"<\\u71e0<\\u71ec<\\u71e7<\\u71f5<\\u71fc<\\u71f9<\\u71ff<\\u720d<\\u7210\"\n");
  fprintf(out, "        \"<\\u721b<\\u7228<\\u722d<\\u722c<\\u7230<\\u7232<\\u723b<\\u723c<\\u723f\"\n");
  fprintf(out, "        \"<\\u7240<\\u7246<\\u724b<\\u7258<\\u7274<\\u727e<\\u7282<\\u7281<\\u7287\"\n");
  fprintf(out, "        \"<\\u7292<\\u7296<\\u72a2<\\u72a7<\\u72b9<\\u72b2<\\u72c3<\\u72c6<\\u72c4\"\n");
  fprintf(out, "        \"<\\u72ce<\\u72d2<\\u72e2<\\u72e0<\\u72e1<\\u72f9<\\u72f7<\\u500f<\\u7317\"\n");
  fprintf(out, "        \"<\\u730a<\\u731c<\\u7316<\\u731d<\\u7334<\\u732f<\\u7329<\\u7325<\\u733e\"\n");
  fprintf(out, "        \"<\\u734e<\\u734f<\\u9ed8<\\u7357<\\u736a<\\u7368<\\u7370<\\u7378<\\u7375\"\n");
  fprintf(out, "        \"<\\u737b<\\u737a<\\u73c8<\\u73b3<\\u73ce<\\u73bb<\\u73c0<\\u73e5<\\u73ee\"\n");
  fprintf(out, "        \"<\\u73de<\\u74a2<\\u7405<\\u746f<\\u7425<\\u73f8<\\u7432<\\u743a<\\u7455\"\n");
  fprintf(out, "        \"<\\u743f<\\u745f<\\u7459<\\u7441<\\u745c<\\u7469<\\u7470<\\u7463<\\u746a\"\n");
  fprintf(out, "        \"<\\u7476<\\u747e<\\u748b<\\u749e<\\u74a7<\\u74ca<\\u74cf<\\u74d4<\\u73f1\"\n");
  fprintf(out, "        \"<\\u74e0<\\u74e3<\\u74e7<\\u74e9<\\u74ee<\\u74f2<\\u74f0<\\u74f1<\\u74f8\"\n");
  fprintf(out, "        \"<\\u74f7<\\u7504<\\u7503<\\u7505<\\u750c<\\u750e<\\u750d<\\u7515<\\u7513\"\n");
  fprintf(out, "        \"<\\u751e<\\u7526<\\u752c<\\u753c<\\u7544<\\u754d<\\u754a<\\u7549<\\u755b\"\n");
  fprintf(out, "        \"<\\u7546<\\u755a<\\u7569<\\u7564<\\u7567<\\u756b<\\u756d<\\u7578<\\u7576\"\n");
  fprintf(out, "        \"<\\u7586<\\u7587<\\u7574<\\u758a<\\u7589<\\u7582<\\u7594<\\u759a<\\u759d\"\n");
  fprintf(out, "        \"<\\u75a5<\\u75a3<\\u75c2<\\u75b3<\\u75c3<\\u75b5<\\u75bd<\\u75b8<\\u75bc\"\n");
  fprintf(out, "        \"<\\u75b1<\\u75cd<\\u75ca<\\u75d2<\\u75d9<\\u75e3<\\u75de<\\u75fe<\\u75ff\"\n");
  fprintf(out, "        \"<\\u75fc<\\u7601<\\u75f0<\\u75fa<\\u75f2<\\u75f3<\\u760b<\\u760d<\\u7609\"\n");
  fprintf(out, "        \"<\\u761f<\\u7627<\\u7620<\\u7621<\\u7622<\\u7624<\\u7634<\\u7630<\\u763b\"\n");
  fprintf(out, "        \"<\\u7647<\\u7648<\\u7646<\\u765c<\\u7658<\\u7661<\\u7662<\\u7668<\\u7669\"\n");
  fprintf(out, "        \"<\\u766a<\\u7667<\\u766c<\\u7670<\\u7672<\\u7676<\\u7678<\\u767c<\\u7680\"\n");
  fprintf(out, "        \"<\\u7683<\\u7688<\\u768b<\\u768e<\\u7696<\\u7693<\\u7699<\\u769a<\\u76b0\"\n");
  fprintf(out, "        \"<\\u76b4<\\u76b8<\\u76b9<\\u76ba<\\u76c2<\\u76cd<\\u76d6<\\u76d2<\\u76de\"\n");
  fprintf(out, "        \"<\\u76e1<\\u76e5<\\u76e7<\\u76ea<\\u862f<\\u76fb<\\u7708<\\u7707<\\u7704\"\n");
  fprintf(out, "        \"<\\u7729<\\u7724<\\u771e<\\u7725<\\u7726<\\u771b<\\u7737<\\u7738<\\u7747\"\n");
  fprintf(out, "        \"<\\u775a<\\u7768<\\u776b<\\u775b<\\u7765<\\u777f<\\u777e<\\u7779<\\u778e\"\n");
  fprintf(out, "        \"<\\u778b<\\u7791<\\u77a0<\\u779e<\\u77b0<\\u77b6<\\u77b9<\\u77bf<\\u77bc\"\n");
  fprintf(out, "        \"<\\u77bd<\\u77bb<\\u77c7<\\u77cd<\\u77d7<\\u77da<\\u77dc<\\u77e3<\\u77ee\"\n");
  fprintf(out, "        \"<\\u77fc<\\u780c<\\u7812<\\u7926<\\u7820<\\u792a<\\u7845<\\u788e<\\u7874\"\n");
  fprintf(out, "        \"<\\u7886<\\u787c<\\u789a<\\u788c<\\u78a3<\\u78b5<\\u78aa<\\u78af<\\u78d1\"\n");
  fprintf(out, "        \"<\\u78c6<\\u78cb<\\u78d4<\\u78be<\\u78bc<\\u78c5<\\u78ca<\\u78ec<\\u78e7\"\n");
  fprintf(out, "        \"<\\u78da<\\u78fd<\\u78f4<\\u7907<\\u7912<\\u7911<\\u7919<\\u792c<\\u792b\"\n");
  fprintf(out, "        \"<\\u7940<\\u7960<\\u7957<\\u795f<\\u795a<\\u7955<\\u7953<\\u797a<\\u797f\"\n");
  fprintf(out, "        \"<\\u798a<\\u799d<\\u79a7<\\u9f4b<\\u79aa<\\u79ae<\\u79b3<\\u79b9<\\u79ba\"\n");
  fprintf(out, "        \"<\\u79c9<\\u79d5<\\u79e7<\\u79ec<\\u79e1<\\u79e3<\\u7a08<\\u7a0d<\\u7a18\"\n");
  fprintf(out, "        \"<\\u7a19<\\u7a20<\\u7a1f<\\u7980<\\u7a31<\\u7a3b<\\u7a3e<\\u7a37<\\u7a43\"\n");
  fprintf(out, "        \"<\\u7a57<\\u7a49<\\u7a61<\\u7a62<\\u7a69<\\u9f9d<\\u7a70<\\u7a79<\\u7a7d\"\n");
  fprintf(out, "        \"<\\u7a88<\\u7a97<\\u7a95<\\u7a98<\\u7a96<\\u7aa9<\\u7ac8<\\u7ab0<\\u7ab6\"\n");
  fprintf(out, "        \"<\\u7ac5<\\u7ac4<\\u7abf<\\u9083<\\u7ac7<\\u7aca<\\u7acd<\\u7acf<\\u7ad5\"\n");
  fprintf(out, "        \"<\\u7ad3<\\u7ad9<\\u7ada<\\u7add<\\u7ae1<\\u7ae2<\\u7ae6<\\u7aed<\\u7af0\"\n");
  fprintf(out, "        \"<\\u7b02<\\u7b0f<\\u7b0a<\\u7b06<\\u7b33<\\u7b18<\\u7b19<\\u7b1e<\\u7b35\"\n");
  fprintf(out, "        \"<\\u7b28<\\u7b36<\\u7b50<\\u7b7a<\\u7b04<\\u7b4d<\\u7b0b<\\u7b4c<\\u7b45\"\n");
  fprintf(out, "        \"<\\u7b75<\\u7b65<\\u7b74<\\u7b67<\\u7b70<\\u7b71<\\u7b6c<\\u7b6e<\\u7b9d\"\n");
  fprintf(out, "        \"<\\u7b98<\\u7b9f<\\u7b8d<\\u7b9c<\\u7b9a<\\u7b8b<\\u7b92<\\u7b8f<\\u7b5d\"\n");
  fprintf(out, "        \"<\\u7b99<\\u7bcb<\\u7bc1<\\u7bcc<\\u7bcf<\\u7bb4<\\u7bc6<\\u7bdd<\\u7be9\"\n");
  fprintf(out, "        \"<\\u7c11<\\u7c14<\\u7be6<\\u7be5<\\u7c60<\\u7c00<\\u7c07<\\u7c13<\\u7bf3\"\n");
  fprintf(out, "        \"<\\u7bf7<\\u7c17<\\u7c0d<\\u7bf6<\\u7c23<\\u7c27<\\u7c2a<\\u7c1f<\\u7c37\"\n");
  fprintf(out, "        \"<\\u7c2b<\\u7c3d<\\u7c4c<\\u7c43<\\u7c54<\\u7c4f<\\u7c40<\\u7c50<\\u7c58\"\n");
  fprintf(out, "        \"<\\u7c5f<\\u7c64<\\u7c56<\\u7c65<\\u7c6c<\\u7c75<\\u7c83<\\u7c90<\\u7ca4\"\n");
  fprintf(out, "        \"<\\u7cad<\\u7ca2<\\u7cab<\\u7ca1<\\u7ca8<\\u7cb3<\\u7cb2<\\u7cb1<\\u7cae\"\n");
  fprintf(out, "        \"<\\u7cb9<\\u7cbd<\\u7cc0<\\u7cc5<\\u7cc2<\\u7cd8<\\u7cd2<\\u7cdc<\\u7ce2\"\n");
  fprintf(out, "        \"<\\u9b3b<\\u7cef<\\u7cf2<\\u7cf4<\\u7cf6<\\u7cfa<\\u7d06<\\u7d02<\\u7d1c\"\n");
  fprintf(out, "        \"<\\u7d15<\\u7d0a<\\u7d45<\\u7d4b<\\u7d2e<\\u7d32<\\u7d3f<\\u7d35<\\u7d46\"\n");
  fprintf(out, "        \"<\\u7d73<\\u7d56<\\u7d4e<\\u7d72<\\u7d68<\\u7d6e<\\u7d4f<\\u7d63<\\u7d93\"\n");
  fprintf(out, "        \"<\\u7d89<\\u7d5b<\\u7d8f<\\u7d7d<\\u7d9b<\\u7dba<\\u7dae<\\u7da3<\\u7db5\"\n");
  fprintf(out, "        \"<\\u7dc7<\\u7dbd<\\u7dab<\\u7e3d<\\u7da2<\\u7daf<\\u7ddc<\\u7db8<\\u7d9f\"\n");
  fprintf(out, "        \"<\\u7db0<\\u7dd8<\\u7ddd<\\u7de4<\\u7dde<\\u7dfb<\\u7df2<\\u7de1<\\u7e05\"\n");
  fprintf(out, "        \"<\\u7e0a<\\u7e23<\\u7e21<\\u7e12<\\u7e31<\\u7e1f<\\u7e09<\\u7e0b<\\u7e22\"\n");
  fprintf(out, "        \"<\\u7e46<\\u7e66<\\u7e3b<\\u7e35<\\u7e39<\\u7e43<\\u7e37<\\u7e32<\\u7e3a\"\n");
  fprintf(out, "        \"<\\u7e67<\\u7e5d<\\u7e56<\\u7e5e<\\u7e59<\\u7e5a<\\u7e79<\\u7e6a<\\u7e69\"\n");
  fprintf(out, "        \"<\\u7e7c<\\u7e7b<\\u7e83<\\u7dd5<\\u7e7d<\\u8fae<\\u7e7f<\\u7e88<\\u7e89\"\n");
  fprintf(out, "        \"<\\u7e8c<\\u7e92<\\u7e90<\\u7e93<\\u7e94<\\u7e96<\\u7e8e<\\u7e9b<\\u7e9c\"\n");
  fprintf(out, "        \"<\\u7f38<\\u7f3a<\\u7f45<\\u7f4c<\\u7f4d<\\u7f4e<\\u7f50<\\u7f51<\\u7f55\"\n");
  fprintf(out, "        \"<\\u7f54<\\u7f58<\\u7f5f<\\u7f60<\\u7f68<\\u7f69<\\u7f67<\\u7f78<\\u7f82\"\n");
  fprintf(out, "        \"<\\u7f86<\\u7f83<\\u7f88<\\u7f87<\\u7f8c<\\u7f94<\\u7f9e<\\u7f9d<\\u7f9a\"\n");
  fprintf(out, "        \"<\\u7fa3<\\u7faf<\\u7fb2<\\u7fb9<\\u7fae<\\u7fb6<\\u7fb8<\\u8b71<\\u7fc5\"\n");
  fprintf(out, "        \"<\\u7fc6<\\u7fca<\\u7fd5<\\u7fd4<\\u7fe1<\\u7fe6<\\u7fe9<\\u7ff3<\\u7ff9\"\n");
  fprintf(out, "        \"<\\u98dc<\\u8006<\\u8004<\\u800b<\\u8012<\\u8018<\\u8019<\\u801c<\\u8021\"\n");
  fprintf(out, "        \"<\\u8028<\\u803f<\\u803b<\\u804a<\\u8046<\\u8052<\\u8058<\\u805a<\\u805f\"\n");
  fprintf(out, "        \"<\\u8062<\\u8068<\\u8073<\\u8072<\\u8070<\\u8076<\\u8079<\\u807d<\\u807f\"\n");
  fprintf(out, "        \"<\\u8084<\\u8086<\\u8085<\\u809b<\\u8093<\\u809a<\\u80ad<\\u5190<\\u80ac\"\n");
  fprintf(out, "        \"<\\u80db<\\u80e5<\\u80d9<\\u80dd<\\u80c4<\\u80da<\\u80d6<\\u8109<\\u80ef\"\n");
  fprintf(out, "        \"<\\u80f1<\\u811b<\\u8129<\\u8123<\\u812f<\\u814b<\\u968b<\\u8146<\\u813e\"\n");
  fprintf(out, "        \"<\\u8153<\\u8151<\\u80fc<\\u8171<\\u816e<\\u8165<\\u8166<\\u8174<\\u8183\"\n");
  fprintf(out, "        \"<\\u8188<\\u818a<\\u8180<\\u8182<\\u81a0<\\u8195<\\u81a4<\\u81a3<\\u815f\"\n");
  fprintf(out, "        \"<\\u8193<\\u81a9<\\u81b0<\\u81b5<\\u81be<\\u81b8<\\u81bd<\\u81c0<\\u81c2\"\n");
  fprintf(out, "        \"<\\u81ba<\\u81c9<\\u81cd<\\u81d1<\\u81d9<\\u81d8<\\u81c8<\\u81da<\\u81df\"\n");
  fprintf(out, "        \"<\\u81e0<\\u81e7<\\u81fa<\\u81fb<\\u81fe<\\u8201<\\u8202<\\u8205<\\u8207\"\n");
  fprintf(out, "        \"<\\u820a<\\u820d<\\u8210<\\u8216<\\u8229<\\u822b<\\u8238<\\u8233<\\u8240\"\n");
  fprintf(out, "        \"<\\u8259<\\u8258<\\u825d<\\u825a<\\u825f<\\u8264<\\u8262<\\u8268<\\u826a\"\n");
  fprintf(out, "        \"<\\u826b<\\u822e<\\u8271<\\u8277<\\u8278<\\u827e<\\u828d<\\u8292<\\u82ab\"\n");
  fprintf(out, "        \"<\\u829f<\\u82bb<\\u82ac<\\u82e1<\\u82e3<\\u82df<\\u82d2<\\u82f4<\\u82f3\"\n");
  fprintf(out, "        \"<\\u82fa<\\u8393<\\u8303<\\u82fb<\\u82f9<\\u82de<\\u8306<\\u82dc<\\u8309\"\n");
  fprintf(out, "        \"<\\u82d9<\\u8335<\\u8334<\\u8316<\\u8332<\\u8331<\\u8340<\\u8339<\\u8350\"\n");
  fprintf(out, "        \"<\\u8345<\\u832f<\\u832b<\\u8317<\\u8318<\\u8385<\\u839a<\\u83aa<\\u839f\"\n");
  fprintf(out, "        \"<\\u83a2<\\u8396<\\u8323<\\u838e<\\u8387<\\u838a<\\u837c<\\u83b5<\\u8373\"\n");
  fprintf(out, "        \"<\\u8375<\\u83a0<\\u8389<\\u83a8<\\u83f4<\\u8413<\\u83eb<\\u83ce<\\u83fd\"\n");
  fprintf(out, "        \"<\\u8403<\\u83d8<\\u840b<\\u83c1<\\u83f7<\\u8407<\\u83e0<\\u83f2<\\u840d\"\n");
  fprintf(out, "        \"<\\u8422<\\u8420<\\u83bd<\\u8438<\\u8506<\\u83fb<\\u846d<\\u842a<\\u843c\"\n");
  fprintf(out, "        \"<\\u855a<\\u8484<\\u8477<\\u846b<\\u84ad<\\u846e<\\u8482<\\u8469<\\u8446\"\n");
  fprintf(out, "        \"<\\u842c<\\u846f<\\u8479<\\u8435<\\u84ca<\\u8462<\\u84b9<\\u84bf<\\u849f\"\n");
  fprintf(out, "        \"<\\u84d9<\\u84cd<\\u84bb<\\u84da<\\u84d0<\\u84c1<\\u84c6<\\u84d6<\\u84a1\"\n");
  fprintf(out, "        \"<\\u8521<\\u84ff<\\u84f4<\\u8517<\\u8518<\\u852c<\\u851f<\\u8515<\\u8514\"\n");
  fprintf(out, "        \"<\\u84fc<\\u8540<\\u8563<\\u8558<\\u8548<\\u8541<\\u8602<\\u854b<\\u8555\"\n");
  fprintf(out, "        \"<\\u8580<\\u85a4<\\u8588<\\u8591<\\u858a<\\u85a8<\\u856d<\\u8594<\\u859b\"\n");
  fprintf(out, "        \"<\\u85ea<\\u8587<\\u859c<\\u8577<\\u857e<\\u8590<\\u85c9<\\u85ba<\\u85cf\"\n");
  fprintf(out, "        \"<\\u85b9<\\u85d0<\\u85d5<\\u85dd<\\u85e5<\\u85dc<\\u85f9<\\u860a<\\u8613\"\n");
  fprintf(out, "        \"<\\u860b<\\u85fe<\\u85fa<\\u8606<\\u8622<\\u861a<\\u8630<\\u863f<\\u864d\"\n");
  fprintf(out, "        \"<\\u4e55<\\u8654<\\u865f<\\u8667<\\u8671<\\u8693<\\u86a3<\\u86a9<\\u86aa\"\n");
  fprintf(out, "        \"<\\u868b<\\u868c<\\u86b6<\\u86af<\\u86c4<\\u86c6<\\u86b0<\\u86c9<\\u8823\"\n");
  fprintf(out, "        \"<\\u86ab<\\u86d4<\\u86de<\\u86e9<\\u86ec<\\u86df<\\u86db<\\u86ef<\\u8712\"\n");
  fprintf(out, "        \"<\\u8706<\\u8708<\\u8700<\\u8703<\\u86fb<\\u8711<\\u8709<\\u870d<\\u86f9\"\n");
  fprintf(out, "        \"<\\u870a<\\u8734<\\u873f<\\u8737<\\u873b<\\u8725<\\u8729<\\u871a<\\u8760\"\n");
  fprintf(out, "        \"<\\u875f<\\u8778<\\u874c<\\u874e<\\u8774<\\u8757<\\u8768<\\u876e<\\u8759\"\n");
  fprintf(out, "        \"<\\u8753<\\u8763<\\u876a<\\u8805<\\u87a2<\\u879f<\\u8782<\\u87af<\\u87cb\"\n");
  fprintf(out, "        \"<\\u87bd<\\u87c0<\\u87d0<\\u96d6<\\u87ab<\\u87c4<\\u87b3<\\u87c7<\\u87c6\"\n");
  fprintf(out, "        \"<\\u87bb<\\u87ef<\\u87f2<\\u87e0<\\u880f<\\u880d<\\u87fe<\\u87f6<\\u87f7\"\n");
  fprintf(out, "        \"<\\u880e<\\u87d2<\\u8811<\\u8816<\\u8815<\\u8822<\\u8821<\\u8831<\\u8836\"\n");
  fprintf(out, "        \"<\\u8839<\\u8827<\\u883b<\\u8844<\\u8842<\\u8852<\\u8859<\\u885e<\\u8862\"\n");
  fprintf(out, "        \"<\\u886b<\\u8881<\\u887e<\\u889e<\\u8875<\\u887d<\\u88b5<\\u8872<\\u8882\"\n");
  fprintf(out, "        \"<\\u8897<\\u8892<\\u88ae<\\u8899<\\u88a2<\\u888d<\\u88a4<\\u88b0<\\u88bf\"\n");
  fprintf(out, "        \"<\\u88b1<\\u88c3<\\u88c4<\\u88d4<\\u88d8<\\u88d9<\\u88dd<\\u88f9<\\u8902\"\n");
  fprintf(out, "        \"<\\u88fc<\\u88f4<\\u88e8<\\u88f2<\\u8904<\\u890c<\\u890a<\\u8913<\\u8943\"\n");
  fprintf(out, "        \"<\\u891e<\\u8925<\\u892a<\\u892b<\\u8941<\\u8944<\\u893b<\\u8936<\\u8938\"\n");
  fprintf(out, "        \"<\\u894c<\\u891d<\\u8960<\\u895e<\\u8966<\\u8964<\\u896d<\\u896a<\\u896f\"\n");
  fprintf(out, "        \"<\\u8974<\\u8977<\\u897e<\\u8983<\\u8988<\\u898a<\\u8993<\\u8998<\\u89a1\"\n");
  fprintf(out, "        \"<\\u89a9<\\u89a6<\\u89ac<\\u89af<\\u89b2<\\u89ba<\\u89bd<\\u89bf<\\u89c0\"\n");
  fprintf(out, "        \"<\\u89da<\\u89dc<\\u89dd<\\u89e7<\\u89f4<\\u89f8<\\u8a03<\\u8a16<\\u8a10\"\n");
  fprintf(out, "        \"<\\u8a0c<\\u8a1b<\\u8a1d<\\u8a25<\\u8a36<\\u8a41<\\u8a5b<\\u8a52<\\u8a46\"\n");
  fprintf(out, "        \"<\\u8a48<\\u8a7c<\\u8a6d<\\u8a6c<\\u8a62<\\u8a85<\\u8a82<\\u8a84<\\u8aa8\"\n");
  fprintf(out, "        \"<\\u8aa1<\\u8a91<\\u8aa5<\\u8aa6<\\u8a9a<\\u8aa3<\\u8ac4<\\u8acd<\\u8ac2\"\n");
  fprintf(out, "        \"<\\u8ada<\\u8aeb<\\u8af3<\\u8ae7<\\u8ae4<\\u8af1<\\u8b14<\\u8ae0<\\u8ae2\"\n");
  fprintf(out, "        \"<\\u8af7<\\u8ade<\\u8adb<\\u8b0c<\\u8b07<\\u8b1a<\\u8ae1<\\u8b16<\\u8b10\"\n");
  fprintf(out, "        \"<\\u8b17<\\u8b20<\\u8b33<\\u97ab<\\u8b26<\\u8b2b<\\u8b3e<\\u8b28<\\u8b41\"\n");
  fprintf(out, "        \"<\\u8b4c<\\u8b4f<\\u8b4e<\\u8b49<\\u8b56<\\u8b5b<\\u8b5a<\\u8b6b<\\u8b5f\"\n");
  fprintf(out, "        \"<\\u8b6c<\\u8b6f<\\u8b74<\\u8b7d<\\u8b80<\\u8b8c<\\u8b8e<\\u8b92<\\u8b93\"\n");
  fprintf(out, "        \"<\\u8b96<\\u8b99<\\u8b9a<\\u8c3a<\\u8c41<\\u8c3f<\\u8c48<\\u8c4c<\\u8c4e\"\n");
  fprintf(out, "        \"<\\u8c50<\\u8c55<\\u8c62<\\u8c6c<\\u8c78<\\u8c7a<\\u8c82<\\u8c89<\\u8c85\"\n");
  fprintf(out, "        \"<\\u8c8a<\\u8c8d<\\u8c8e<\\u8c94<\\u8c7c<\\u8c98<\\u621d<\\u8cad<\\u8caa\"\n");
  fprintf(out, "        \"<\\u8cbd<\\u8cb2<\\u8cb3<\\u8cae<\\u8cb6<\\u8cc8<\\u8cc1<\\u8ce4<\\u8ce3\"\n");
  fprintf(out, "        \"<\\u8cda<\\u8cfd<\\u8cfa<\\u8cfb<\\u8d04<\\u8d05<\\u8d0a<\\u8d07<\\u8d0f\"\n");
  fprintf(out, "        \"<\\u8d0d<\\u8d10<\\u9f4e<\\u8d13<\\u8ccd<\\u8d14<\\u8d16<\\u8d67<\\u8d6d\"\n");
  fprintf(out, "        \"<\\u8d71<\\u8d73<\\u8d81<\\u8d99<\\u8dc2<\\u8dbe<\\u8dba<\\u8dcf<\\u8dda\"\n");
  fprintf(out, "        \"<\\u8dd6<\\u8dcc<\\u8ddb<\\u8dcb<\\u8dea<\\u8deb<\\u8ddf<\\u8de3<\\u8dfc\"\n");
  fprintf(out, "        \"<\\u8e08<\\u8e09<\\u8dff<\\u8e1d<\\u8e1e<\\u8e10<\\u8e1f<\\u8e42<\\u8e35\"\n");
  fprintf(out, "        \"<\\u8e30<\\u8e34<\\u8e4a<\\u8e47<\\u8e49<\\u8e4c<\\u8e50<\\u8e48<\\u8e59\"\n");
  fprintf(out, "        \"<\\u8e64<\\u8e60<\\u8e2a<\\u8e63<\\u8e55<\\u8e76<\\u8e72<\\u8e7c<\\u8e81\"\n");
  fprintf(out, "        \"<\\u8e87<\\u8e85<\\u8e84<\\u8e8b<\\u8e8a<\\u8e93<\\u8e91<\\u8e94<\\u8e99\"\n");
  fprintf(out, "        \"<\\u8eaa<\\u8ea1<\\u8eac<\\u8eb0<\\u8ec6<\\u8eb1<\\u8ebe<\\u8ec5<\\u8ec8\"\n");
  fprintf(out, "        \"<\\u8ecb<\\u8edb<\\u8ee3<\\u8efc<\\u8efb<\\u8eeb<\\u8efe<\\u8f0a<\\u8f05\"\n");
  fprintf(out, "        \"<\\u8f15<\\u8f12<\\u8f19<\\u8f13<\\u8f1c<\\u8f1f<\\u8f1b<\\u8f0c<\\u8f26\"\n");
  fprintf(out, "        \"<\\u8f33<\\u8f3b<\\u8f39<\\u8f45<\\u8f42<\\u8f3e<\\u8f4c<\\u8f49<\\u8f46\"\n");
  fprintf(out, "        \"<\\u8f4e<\\u8f57<\\u8f5c<\\u8f62<\\u8f63<\\u8f64<\\u8f9c<\\u8f9f<\\u8fa3\"\n");
  fprintf(out, "        \"<\\u8fad<\\u8faf<\\u8fb7<\\u8fda<\\u8fe5<\\u8fe2<\\u8fea<\\u8fef<\\u9087\"\n");
  fprintf(out, "        \"<\\u8ff4<\\u9005<\\u8ff9<\\u8ffa<\\u9011<\\u9015<\\u9021<\\u900d<\\u901e\"\n");
  fprintf(out, "        \"<\\u9016<\\u900b<\\u9027<\\u9036<\\u9035<\\u9039<\\u8ff8<\\u904f<\\u9050\"\n");
  fprintf(out, "        \"<\\u9051<\\u9052<\\u900e<\\u9049<\\u903e<\\u9056<\\u9058<\\u905e<\\u9068\"\n");
  fprintf(out, "        \"<\\u906f<\\u9076<\\u96a8<\\u9072<\\u9082<\\u907d<\\u9081<\\u9080<\\u908a\"\n");
  fprintf(out, "        \"<\\u9089<\\u908f<\\u90a8<\\u90af<\\u90b1<\\u90b5<\\u90e2<\\u90e4<\\u6248\"\n");
  fprintf(out, "        \"<\\u90db<\\u9102<\\u9112<\\u9119<\\u9132<\\u9130<\\u914a<\\u9156<\\u9158\"\n");
  fprintf(out, "        \"<\\u9163<\\u9165<\\u9169<\\u9173<\\u9172<\\u918b<\\u9189<\\u9182<\\u91a2\"\n");
  fprintf(out, "        \"<\\u91ab<\\u91af<\\u91aa<\\u91b5<\\u91b4<\\u91ba<\\u91c0<\\u91c1<\\u91c9\"\n");
  fprintf(out, "        \"<\\u91cb<\\u91d0<\\u91d6<\\u91df<\\u91e1<\\u91db<\\u91fc<\\u91f5<\\u91f6\"\n");
  fprintf(out, "        \"<\\u921e<\\u91ff<\\u9214<\\u922c<\\u9215<\\u9211<\\u925e<\\u9257<\\u9245\"\n");
  fprintf(out, "        \"<\\u9249<\\u9264<\\u9248<\\u9295<\\u923f<\\u924b<\\u9250<\\u929c<\\u9296\"\n");
  fprintf(out, "        \"<\\u9293<\\u929b<\\u925a<\\u92cf<\\u92b9<\\u92b7<\\u92e9<\\u930f<\\u92fa\"\n");
  fprintf(out, "        \"<\\u9344<\\u932e<\\u9319<\\u9322<\\u931a<\\u9323<\\u933a<\\u9335<\\u933b\"\n");
  fprintf(out, "        \"<\\u935c<\\u9360<\\u937c<\\u936e<\\u9356<\\u93b0<\\u93ac<\\u93ad<\\u9394\"\n");
  fprintf(out, "        \"<\\u93b9<\\u93d6<\\u93d7<\\u93e8<\\u93e5<\\u93d8<\\u93c3<\\u93dd<\\u93d0\"\n");
  fprintf(out, "        \"<\\u93c8<\\u93e4<\\u941a<\\u9414<\\u9413<\\u9403<\\u9407<\\u9410<\\u9436\"\n");
  fprintf(out, "        \"<\\u942b<\\u9435<\\u9421<\\u943a<\\u9441<\\u9452<\\u9444<\\u945b<\\u9460\"\n");
  fprintf(out, "        \"<\\u9462<\\u945e<\\u946a<\\u9229<\\u9470<\\u9475<\\u9477<\\u947d<\\u945a\"\n");
  fprintf(out, "        \"<\\u947c<\\u947e<\\u9481<\\u947f<\\u9582<\\u9587<\\u958a<\\u9594<\\u9596\"\n");
  fprintf(out, "        \"<\\u9598<\\u9599<\\u95a0<\\u95a8<\\u95a7<\\u95ad<\\u95bc<\\u95bb<\\u95b9\"\n");
  fprintf(out, "        \"<\\u95be<\\u95ca<\\u6ff6<\\u95c3<\\u95cd<\\u95cc<\\u95d5<\\u95d4<\\u95d6\"\n");
  fprintf(out, "        \"<\\u95dc<\\u95e1<\\u95e5<\\u95e2<\\u9621<\\u9628<\\u962e<\\u962f<\\u9642\"\n");
  fprintf(out, "        \"<\\u964c<\\u964f<\\u964b<\\u9677<\\u965c<\\u965e<\\u965d<\\u965f<\\u9666\"\n");
  fprintf(out, "        \"<\\u9672<\\u966c<\\u968d<\\u9698<\\u9695<\\u9697<\\u96aa<\\u96a7<\\u96b1\"\n");
  fprintf(out, "        \"<\\u96b2<\\u96b0<\\u96b4<\\u96b6<\\u96b8<\\u96b9<\\u96ce<\\u96cb<\\u96c9\"\n");
  fprintf(out, "        \"<\\u96cd<\\u894d<\\u96dc<\\u970d<\\u96d5<\\u96f9<\\u9704<\\u9706<\\u9708\"\n");
  fprintf(out, "        \"<\\u9713<\\u970e<\\u9711<\\u970f<\\u9716<\\u9719<\\u9724<\\u972a<\\u9730\"\n");
  fprintf(out, "        \"<\\u9739<\\u973d<\\u973e<\\u9744<\\u9746<\\u9748<\\u9742<\\u9749<\\u975c\"\n");
  fprintf(out, "        \"<\\u9760<\\u9764<\\u9766<\\u9768<\\u52d2<\\u976b<\\u9771<\\u9779<\\u9785\"\n");
  fprintf(out, "        \"<\\u977c<\\u9781<\\u977a<\\u9786<\\u978b<\\u978f<\\u9790<\\u979c<\\u97a8\"\n");
  fprintf(out, "        \"<\\u97a6<\\u97a3<\\u97b3<\\u97b4<\\u97c3<\\u97c6<\\u97c8<\\u97cb<\\u97dc\"\n");
  fprintf(out, "        \"<\\u97ed<\\u9f4f<\\u97f2<\\u7adf<\\u97f6<\\u97f5<\\u980f<\\u980c<\\u9838\"\n");
  fprintf(out, "        \"<\\u9824<\\u9821<\\u9837<\\u983d<\\u9846<\\u984f<\\u984b<\\u986b<\\u986f\"\n");
  fprintf(out, "        \"<\\u9870<\\u9871<\\u9874<\\u9873<\\u98aa<\\u98af<\\u98b1<\\u98b6<\\u98c4\"\n");
  fprintf(out, "        \"<\\u98c3<\\u98c6<\\u98e9<\\u98eb<\\u9903<\\u9909<\\u9912<\\u9914<\\u9918\"\n");
  fprintf(out, "        \"<\\u9921<\\u991d<\\u991e<\\u9924<\\u9920<\\u992c<\\u992e<\\u993d<\\u993e\"\n");
  fprintf(out, "        \"<\\u9942<\\u9949<\\u9945<\\u9950<\\u994b<\\u9951<\\u9952<\\u994c<\\u9955\"\n");
  fprintf(out, "        \"<\\u9997<\\u9998<\\u99a5<\\u99ad<\\u99ae<\\u99bc<\\u99df<\\u99db<\\u99dd\"\n");
  fprintf(out, "        \"<\\u99d8<\\u99d1<\\u99ed<\\u99ee<\\u99f1<\\u99f2<\\u99fb<\\u99f8<\\u9a01\"\n");
  fprintf(out, "        \"<\\u9a0f<\\u9a05<\\u99e2<\\u9a19<\\u9a2b<\\u9a37<\\u9a45<\\u9a42<\\u9a40\"\n");
  fprintf(out, "        \"<\\u9a43<\\u9a3e<\\u9a55<\\u9a4d<\\u9a5b<\\u9a57<\\u9a5f<\\u9a62<\\u9a65\"\n");
  fprintf(out, "        \"<\\u9a64<\\u9a69<\\u9a6b<\\u9a6a<\\u9aad<\\u9ab0<\\u9abc<\\u9ac0<\\u9acf\"\n");
  fprintf(out, "        \"<\\u9ad1<\\u9ad3<\\u9ad4<\\u9ade<\\u9adf<\\u9ae2<\\u9ae3<\\u9ae6<\\u9aef\"\n");
  fprintf(out, "        \"<\\u9aeb<\\u9aee<\\u9af4<\\u9af1<\\u9af7<\\u9afb<\\u9b06<\\u9b18<\\u9b1a\"\n");
  fprintf(out, "        \"<\\u9b1f<\\u9b22<\\u9b23<\\u9b25<\\u9b27<\\u9b28<\\u9b29<\\u9b2a<\\u9b2e\"\n");
  fprintf(out, "        \"<\\u9b2f<\\u9b32<\\u9b44<\\u9b43<\\u9b4f<\\u9b4d<\\u9b4e<\\u9b51<\\u9b58\"\n");
  fprintf(out, "        \"<\\u9b74<\\u9b93<\\u9b83<\\u9b91<\\u9b96<\\u9b97<\\u9b9f<\\u9ba0<\\u9ba8\"\n");
  fprintf(out, "        \"<\\u9bb4<\\u9bc0<\\u9bca<\\u9bb9<\\u9bc6<\\u9bcf<\\u9bd1<\\u9bd2<\\u9be3\"\n");
  fprintf(out, "        \"<\\u9be2<\\u9be4<\\u9bd4<\\u9be1<\\u9c3a<\\u9bf2<\\u9bf1<\\u9bf0<\\u9c15\"\n");
  fprintf(out, "        \"<\\u9c14<\\u9c09<\\u9c13<\\u9c0c<\\u9c06<\\u9c08<\\u9c12<\\u9c0a<\\u9c04\"\n");
  fprintf(out, "        \"<\\u9c2e<\\u9c1b<\\u9c25<\\u9c24<\\u9c21<\\u9c30<\\u9c47<\\u9c32<\\u9c46\"\n");
  fprintf(out, "        \"<\\u9c3e<\\u9c5a<\\u9c60<\\u9c67<\\u9c76<\\u9c78<\\u9ce7<\\u9cec<\\u9cf0\"\n");
  fprintf(out, "        \"<\\u9d09<\\u9d08<\\u9ceb<\\u9d03<\\u9d06<\\u9d2a<\\u9d26<\\u9daf<\\u9d23\"\n");
  fprintf(out, "        \"<\\u9d1f<\\u9d44<\\u9d15<\\u9d12<\\u9d41<\\u9d3f<\\u9d3e<\\u9d46<\\u9d48\"\n");
  fprintf(out, "        \"<\\u9d5d<\\u9d5e<\\u9d64<\\u9d51<\\u9d50<\\u9d59<\\u9d72<\\u9d89<\\u9d87\"\n");
  fprintf(out, "        \"<\\u9dab<\\u9d6f<\\u9d7a<\\u9d9a<\\u9da4<\\u9da9<\\u9db2<\\u9dc4<\\u9dc1\"\n");
  fprintf(out, "        \"<\\u9dbb<\\u9db8<\\u9dba<\\u9dc6<\\u9dcf<\\u9dc2<\\u9dd9<\\u9dd3<\\u9df8\"\n");
  fprintf(out, "        \"<\\u9de6<\\u9ded<\\u9def<\\u9dfd<\\u9e1a<\\u9e1b<\\u9e1e<\\u9e75<\\u9e79\"\n");
  fprintf(out, "        \"<\\u9e7d<\\u9e81<\\u9e88<\\u9e8b<\\u9e8c<\\u9e92<\\u9e95<\\u9e91<\\u9e9d\"\n");
  fprintf(out, "        \"<\\u9ea5<\\u9ea9<\\u9eb8<\\u9eaa<\\u9ead<\\u9761<\\u9ecc<\\u9ece<\\u9ecf\"\n");
  fprintf(out, "        \"<\\u9ed0<\\u9ed4<\\u9edc<\\u9ede<\\u9edd<\\u9ee0<\\u9ee5<\\u9ee8<\\u9eef\"\n");
  fprintf(out, "        \"<\\u9ef4<\\u9ef6<\\u9ef7<\\u9ef9<\\u9efb<\\u9efc<\\u9efd<\\u9f07<\\u9f08\"\n");
  fprintf(out, "        \"<\\u76b7<\\u9f15<\\u9f21<\\u9f2c<\\u9f3e<\\u9f4a<\\u9f52<\\u9f54<\\u9f63\"\n");
  fprintf(out, "        \"<\\u9f5f<\\u9f60<\\u9f61<\\u9f66<\\u9f67<\\u9f6c<\\u9f6a<\\u9f77<\\u9f72\"\n");
  fprintf(out, "        \"<\\u9f76<\\u9f95<\\u9f9c<\\u9fa0<\\u582f<\\u69c7<\\u9059<\\u7464<\\u51dc\"\n");
  fprintf(out, "        \"<\\u7199\"\n");
}

