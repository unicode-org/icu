#include "tblprint.h"

char *formatElementString(uint32_t CE, char *buffer) {
    char temp[1024];
    UBool firstPrim = FALSE;
    sprintf(buffer, "[");
    if(UCOL_PRIMARYORDER(CE)>>8 != 0x02) {
        sprintf(temp, "%02X ", UCOL_PRIMARYORDER(CE)>>8);
        strcat(buffer, temp);
        firstPrim = TRUE;
    }

    if((UCOL_PRIMARYORDER(CE)&0xFF) != 0x02 || firstPrim == TRUE) {
        sprintf(temp, "%02X", UCOL_PRIMARYORDER(CE)&0xFF);
        strcat(buffer, temp);
    }
    firstPrim = FALSE;

    strcat(buffer, ",");

    if(UCOL_SECONDARYORDER(CE) != 0x02) {
        sprintf(temp, " %02X", UCOL_SECONDARYORDER(CE));
        strcat(buffer, temp);
    }

    strcat(buffer, ",");

    if((UCOL_TERTIARYORDER(CE)&0x7F) != 0x02) {
        sprintf(temp, " %02X", UCOL_TERTIARYORDER(CE)&0x7F);
        strcat(buffer, temp);
    }

    strcat(buffer, "]");

    return buffer;
}

void printExp(uint32_t CE, uint32_t oldCE, char* primb, char* secb, char *terb, UBool *printedCont) {
    char temp[1024];
    if(CE<UCOL_NOT_FOUND) {
        if(*printedCont == FALSE) {
            fprintf(stdout, "%s ", formatElementString(oldCE, temp));
        } else {
            oldCE &= 0x0FFFFFFF;
            if(UCOL_PRIMARYORDER(oldCE) > 0xFF) {
                sprintf(temp, "%02X ", UCOL_PRIMARYORDER(oldCE)>>8);
                strcat(primb, temp);
            }

            if(UCOL_PRIMARYORDER(oldCE) != 0) {
                sprintf(temp, "%02X ", UCOL_PRIMARYORDER(oldCE)&0xFF);
                strcat(primb, temp);
            }
            if(UCOL_SECONDARYORDER(oldCE) != 0) {
                sprintf(temp, "%02X ", UCOL_SECONDARYORDER(oldCE));
                strcat(secb, temp);
            }
            if(UCOL_TERTIARYORDER(oldCE) != 0) {
                sprintf(temp, "%02X ", UCOL_TERTIARYORDER(oldCE));
                strcat(terb, temp);
            }
            fprintf(stdout, "[%s, %s, %s] ", primb, secb, terb);
            *primb = *secb = *terb = *temp = 0;
        }
        *printedCont = FALSE;
    } else { /* this is a contiunation, process accordingly */
        if(*printedCont == TRUE) {
            oldCE &= 0x0FFFFFFF;
        }
        if(UCOL_PRIMARYORDER(oldCE) > 0xFF) {
            sprintf(temp, "%02X ", UCOL_PRIMARYORDER(oldCE)>>8);
            strcat(primb, temp);
        }

        if(UCOL_PRIMARYORDER(oldCE) != 0) {
            sprintf(temp, "%02X ", UCOL_PRIMARYORDER(oldCE)&0xFF);
            strcat(primb, temp);
        }
        if(UCOL_SECONDARYORDER(oldCE) != 0) {
            sprintf(temp, "%02X ", UCOL_SECONDARYORDER(oldCE));
            strcat(secb, temp);
        }
        if(UCOL_TERTIARYORDER(oldCE)&0x7F != 0) {
            sprintf(temp, "%02X ", UCOL_TERTIARYORDER(oldCE)&0x7F);
            strcat(terb, temp);
        }
        *printedCont = TRUE;
    }
}

void printOutTable(UCATableHeader *myData, UErrorCode *status) {
    if(U_FAILURE(*status)) {
        return;
    }
    int32_t i = 0, j = 0;
    int32_t CE = 0;
    uint32_t *address = NULL;
    uint8_t size = 0;
    char buffer[1024];
    for(i = 0; i<=0xFFFF; i++) {
        CE = ucmp32_get(myData->mapping, i);
        if(CE != UCOL_NOT_FOUND) {
            fprintf(stdout, "%04X; ", i);
            if(CE < UCOL_NOT_FOUND) {
                fprintf(stdout, "%c; %s ", (UCOL_TERTIARYORDER(CE)&0x80)>>7?'L':'S', formatElementString(CE, buffer));
            } else {
                int32_t tag = (CE&UCOL_TAG_MASK)>>UCOL_TAG_SHIFT;
                if(tag == SURROGATE_TAG) {
                    // do surrogates
                }
                if(tag == THAI_TAG) {
                    address = ((uint32_t*)myData+((CE&0x00FFFFF0)>>4));
                    CE = *(address);
                    fprintf(stdout, "%c; %s ", (UCOL_TERTIARYORDER(CE)&0x80)>>7?'L':'S', formatElementString(CE, buffer));
                    fprintf(stdout, "THAI - from %08X to %08X (offset %05X) ", CE, address, ((CE&0x00FFFFF0)>>4));
                }
                if(tag == CONTRACTION_TAG) {
                    int16_t hasBackward = 0;
                    char conChars[1024];
                    char temp[1024];
                    sprintf(conChars, "%04X", i);
                    UChar *contractionCP = (UChar *)myData+getContractOffset(CE);
                    hasBackward = *(contractionCP); /* skip backward */
                    UBool printSeq = FALSE;
                    address = (uint32_t *)((uint8_t*)myData+myData->contractionCEs)+(contractionCP - (UChar *)((uint8_t*)myData+myData->contractionIndex));
                    while(*contractionCP != 0xFFFF) {
                        if(printSeq == TRUE) {
                            fprintf(stdout, "\n%s;",conChars);
                        }
                        CE = *(address);
                        fprintf(stdout, "%c; %s ", (UCOL_TERTIARYORDER(CE)&0x80)>>7?'L':'S', formatElementString(CE, buffer));
                        fprintf(stdout, "Contraction ");
                        if(hasBackward != 0) {
                          fprintf(stdout, "Back = %i ", hasBackward);
                        }

                        contractionCP++;
                        address++;
                        sprintf(temp, " %04X", *contractionCP);
                        strcat(conChars, temp);
                        printSeq = TRUE;
                    }


                }
                if(tag == EXPANSION_TAG) {
                    char primb[1024], secb[1024], terb[1024], temp[1024];
                    UBool printedCont = FALSE;
                    uint32_t oldCE;
                    *primb = *secb = *terb = *temp = 0;
                    size = CE&0xF;
                    address = ((uint32_t*)myData+((CE&0x00FFFFF0)>>4));
                    CE = *(address++);
                    fprintf(stdout, "%c; ", (UCOL_TERTIARYORDER(CE)&0x80)>>7?'L':'S');

                    if(size != 0) {
                        for(j = 1; j<size; j++) {
                            oldCE = CE;
                            CE = *(address++);
                            printExp(CE, oldCE, primb, secb, terb, &printedCont);
                        }
                    } else {
                        while(*address != 0) {
                            oldCE = CE;
                            CE = *(address++);
                            printExp(CE, oldCE, primb, secb, terb, &printedCont);
                        }
                    }
                    printExp(CE, CE, primb, secb, terb, &printedCont);
                    if(*primb != '\0' || *secb != '\0' || *terb != '\0') {
                        fprintf(stdout, "[%s, %s, %s] ", primb, secb, terb);
                    }
                }

                if(tag == CHARSET_TAG) {
                    ;
                }
            }
            /*
            UCAElements *e = (UCAElements *)uhash_get(elements, (void *)i);
            fprintf(stdout, "%s", e->comment);
            */
        }
    }
}

