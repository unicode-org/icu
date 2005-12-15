/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2005, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/* This file is the test code for test code wbnf.cpp
 * The wbnf.cpp is an extended BNF parser to generate string for ICU test code
 *
 * DO NOT include this file in any file other than at the tail of "wbnf.cpp"
 * Putting the test code here is to increase readability for "wbnf.cpp".
 */

#ifndef _WBNFTEST
#define _WBNFTEST

#define CALL(fun) \
    if (fun()){ \
        printf("Pass: " #fun "\n");\
    } else { \
        printf("FAILED: !!! " #fun " !!!\n"); \
    }

#define DUMP_R(fun, var, times) \
    {printf("\n========= " #fun " =============\n"); \
    for (int i=0; i<times; i++) { \
        const char * t = var.next();\
        fwrite(t,strlen(t),1,stdout); \
        printf("\n");   \
    }   \
    printf("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n");}



static UBool TestQuote(){
    const char *const str = "This ' A !,z| qq [] .new\tline";
    const char *const str_r = "This \\' A '!,'z'|' qq '[]' '.'new\tline";
    ////
    //// :(  we must quote our string to following C syntax
    ////     cannot type the literal here, it makes our code rather human unreadable
    ////     very very unconformable!
    ////
    ///* 
    //*/

    //const char *const s1    =   "ab'c";
    //const char (* s1_r1) [] = { "ab''c",    // ab''c
    //                            "ab\\'c",   // ab\'c
    //                           };//
    ///*
    // .      '.'     \.
    // ..     \.\.    '.'\.   '.'\.   '..'    // '.''.'  wrong
    //*/

    //const char *const s2    =   "a..'.b";       // a..'.b
    //const char (*s2_r) []   = { "a'..''.'b"     // a'..''.'b   
    //                           ,"a'..\\'.'b"    // a'..\'.'b
    //                           ,"a'..'\\''.'b"  // a'..'\''.'b
    //                          };//

    //const char *const s3    =   "a..\\.b";      // a..\.b
    //const char (*s3_r) []   = { "a'..\\\\.'b"   // a'..\\.'b
    //                           ,"a'..'\\\\'.'b" // a'..'\\'.'b
    //                          };//

    //                            // no catact operation, no choice, must be compact

    srand((unsigned)time( NULL ));
    
    //Escaper l(Escaper::NO, Escaper::NO, Escaper::RAND_ESC);
    Pick *p = new Literal(str);
    Quote q(*p);

    DUMP_R(TestQuote, (*p), 1);
    DUMP_R(TestQuote, q, 20);
    return FALSE;
}
static UBool TestLiteral(){
    const char * s = "test string99.";
    Literal n(s);
    const char * r = n.next();
    return strcmp(s,r) == 0;
}

static UBool TestSequence(){
    Sequence seq;
    seq.append(new Literal("abc "));
    seq.append(new Literal(", s"));

    return strcmp(seq.next(), "abc , s") == 0;
}
static UBool TestAlternation(){
    srand((unsigned)time( NULL ));
    Alternation alt;
    alt.append(new Literal("aaa_10%"),10);
    alt.append(new Literal("bbb_0%"),0);
    alt.append(new Literal("ccc_10%"),10);
    alt.append(new Literal("ddddddd_50%"),50);

    DUMP_R(TestAlternation, alt, 50);

    return FALSE;
}

static UBool TestBuffer(){
    Buffer_int t;
    t.append(1).append(0).append(5);
    int s = t.content_size();
    for (int i=0; i<s; ++i){
        printf("%d\n", t[i]);
    }
    return FALSE;
}

static UBool TestWeightedRand(){
    srand((unsigned)time( NULL ));
    Buffer_int t;
    t.append(1).append(0).append(5);
    WeightedRand wr(&Buffer_int().append(10).append(0).append(50),4);
//    WeightedRand wr(&t,3);
    for (int i=0; i< 50; ++i){
        printf("%d\n", wr.next());
    }
    return FALSE;
}

static UBool TestRepeat(){
    srand((unsigned)time( NULL ));
    Repeat rep(new Literal("aaa1-5 "), 1, 5);
    DUMP_R(TestRepeat, rep, 50);

    Repeat r2(new Literal("b{1,3}1%0%5% "), 1, 3, &Buffer_int().append(1).append(0).append(5));
    DUMP_R(TestRepeat, r2, 50);

    Repeat r3(new Literal("aaa5-5 "), 5, 5);
    DUMP_R(TestRepeat, r3, 50);

    return FALSE;
}

static UBool TestVariable(){
    SymbolTable tab;
    Pick * value = new Literal("string1");
    Variable var1(&tab, "x", value);

    Variable var2(&tab, "y");
//    tab.put(var2, value); // TOFIX: point alias/recursion problem
    Pick * value2 = new Literal("string2");
    tab.put(var2, value2);

    Pick * value3 = new Literal("string3");
    Variable var3(&tab, "z");
    tab.put("z", value3);

    UBool pass;
    pass = strcmp(var1.next(), value->next()) == 0;
    pass = pass && strcmp(var2.next(), value2->next()) == 0;
    pass = pass && strcmp(var3.next(), value3->next()) == 0;
    return pass;
}

static UBool TestSymbolTable(){
    Literal * n1 = new Literal("string1");
    Literal * n2 = new Literal("string2");
    SymbolTable t;
    t.put("abc", n1);
    t.put("$aaa", n2);
//    t.put("alias", n1);  // TOFIX: point alias/recursion problem
    t.put("bbb");

    UBool pass;
    pass = t.find(NULL) == SymbolTable::EMPTY;
    pass = pass && t.find("ccc") == SymbolTable::NO_VAR;
    pass = pass && t.find("bbb") == SymbolTable::NO_REF;
    pass = pass && t.find("abc") == SymbolTable::HAS_REF;
    pass = pass && t.find("$aaa") == SymbolTable::HAS_REF;

    t.reset();
    pass = pass && t.find("abc") == SymbolTable::NO_VAR;
    return pass;
}


int DumpScanner(Scanner & s, UBool dump = TRUE){
    int len = strlen(s.source);
    int error_start_offset = s.history - s.source;
    if (dump){
        printf("\n=================== DumpScanner ================\n");
        fwrite(s.source, len, 1, stdout);
        printf("\n-----parsed-------------------------------------\n");
        fwrite(s.source, s.history - s.source, 1, stdout);
        printf("\n-----current------------------------------------\n");
        fwrite(s.history, s.working - s.history, 1, stdout);
        printf("\n-----unparsed-----------------------------------\n");
        fwrite(s.working, (s.source + len - s.working), 1, stdout);
        printf("\n^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n");
    }
    return error_start_offset;
}

static UBool TestScanner(void){
    //const char str1[] = "$root = $command{0,5} $reset $mostRules{1,20};";
    //const char str1_r[][20] = {"$root", "=", "$command", "{", "0", ",", "5", "}", 
    //    "$reset", "$mostRules", "{", "1", ",", "20", "}", ";"};

    const char str2[] = "$p2 =(\\\\ $s $string $s)? 25%;";
    const char str2_r[][20] = {"$p2", "=", "(", "\\", "$s", "$string", "$s", ")", "?", "25", "%", ";"};

    const char *str = str2;
    const char (*str_r)[20] = str2_r;
    int tokenNum = sizeof(str2_r)/sizeof(char[20]);

    Scanner t(str);
    UBool pass = TRUE;
    t.getNextToken();
    int i = 0;
    while (pass){
        if (t.tokenType == STREAM_END){
            pass = pass? i == tokenNum : FALSE;
            break;//while
        } else if (t.tokenType == ERROR){
            pass = FALSE;
            break;//while
        } else {
            pass = strcmp( &(t.token[0]), str_r[i++]) == 0;
            t.getNextToken();
        }
    }

    //const char ts[] = "$commandList = '['"
    //" ( alternate ' ' $alternateOptions"
    //" | backwards ' 2'"
    //" | normalization ' ' $onoff "
    //" | caseLevel ' ' $onoff "
    //" | hiraganaQ ' ' $onoff"
    //" | caseFirst ' ' $caseFirstOptions"
    //" | strength ' ' $strengthOptions"
    //" ) ']';" ;

    //Scanner t2(ts);
    //pass = TRUE;
    //do {
    //    t2.getNextToken();
    //    if (t2.tokenType == ERROR){
    //        DumpScanner(t2);
    //        return FALSE;
    //    }
    //}while (t.tokenType != STREAM_END);

    return pass;
}

class TestParserT {
public:
UBool operator () (const char *const str, const int exp_error_offset = -1, const UBool dump = TRUE){
    Parser par(str);
    if (par.rules()){
        if ( exp_error_offset == -1){
            return TRUE;
        }else {
            DumpScanner(par.s,dump);
            return FALSE;
        }
    }else {
        return DumpScanner(par.s, dump) == exp_error_offset;
    }
}
};

UBool TestParser(){
    TestParserT test;

    UBool pass = TRUE;
    pass = pass && test ("$s = ' ' ? 50%;");
    pass = pass && test("$x = ($var {1,2}) 3%;");         // legal
    pass = pass && test("$x = $var {1,2} 3% | b 4%;");    // legal
    pass = pass && test("$x = $var {1,2} 3%;");           // legal
    pass = pass && test("$m = $c ? 2% 4% | $r 5% | $n 25%;"); // legal
    pass = pass && test("$a = b ? 2% | c 5%;");               // legal
    pass = pass && test("$x = A B 5% C 10% | D;", 8, FALSE);  // illegal 5%
    pass = pass && test("$x = aa 45% | bb 5% cc;", 19, FALSE);// illegal cc
    pass = pass && test("$x = (b 5%) (c 6%);");               // legal
    pass = pass && test("$x = (b 5%) c 6%;", 13, FALSE);      // illegal 6%
    pass = pass && test("$x = b 5% (c 6%);", 9, FALSE);       // illegal (c 6%)
    pass = pass && test("$x = b 5% c 6%;", 9, FALSE);         // illegal c 6%
    pass = pass && test("$x = b 5%;");                        // legal
    pass = pass && test("$x = aa 45% | bb 5% cc;", 19, FALSE);// illegal cc
    pass = pass && test("$x = a | b  | c 4% | d 5%;");        // legal
    pass = pass && test("$s = ' ' ? 50% abc;");               // legal
    pass = pass && test("$s =  a | c d | e f;");              // legal
    pass = pass && test( "$z = q 0% | p 1% | r 100%;");         // legal How to check parsed tree??

    pass = pass && test("$s = ' ' ? 50%;");
    pass = pass && test("$relationList = '<' | '<<' |  ';' | '<<<' | ',' | '=';");
    pass = pass && test("$p1 = ($string $s '|' $s)? 25%;");
    pass = pass && test("$p2 = (\\\\ $s $string $s)? 25%;");
    pass = pass && test("$rel2 = $p1 $string $s $p2;");
    pass = pass && test("$relation = $relationList $s ($rel1 | $rel2) $crlf;");
    pass = pass && test("$command = $commandList $crlf;");
    pass = pass && test("$reset = '&' $s ($beforeList $s)? 10% ($positionList 100% | $string 10%) $crlf;");
    pass = pass && test("$mostRules = $command 1% | $reset 5% | $relation 25%;");
    pass = pass && test("$root = $command{0,5} $reset $mostRules{1,20};");

    const char collationBNF[] =
    "$s = ' '? 50%;" 
    "$crlf = '\r\n';" 

    "$alternateOptions = non'-'ignorable | shifted;" 
    "$onoff = on | off;" 
    "$caseFirstOptions = off | upper | lower;" 
    "$strengthOptions = '1' | '2' | '3' | '4' | 'I';" 
    "$commandList = '['"
    " ( alternate ' ' $alternateOptions"
    " | backwards ' 2'"
    " | normalization ' ' $onoff "
    " | caseLevel ' ' $onoff "
    " | hiraganaQ ' ' $onoff"
    " | caseFirst ' ' $caseFirstOptions"
    " | strength ' ' $strengthOptions"
    " ) ']';" 
    "$command = $commandList $crlf;" 

    "$ignorableTypes = (tertiary | secondary | primary) ' ' ignorable;" 
    "$allTypes = variable | regular | implicit | trailing | $ignorableTypes;" 
    "$positionList = '[' (first | last) ' ' $allTypes ']';"

    "$beforeList = '[before ' ('1' | '2' | '3') ']';"

    "$relationList = ("
    "   '<'"
    " | '<<'"
    " | ';'" 
    " | '<<<'"
    " | ','" 
    " | '='"
    ");"
    "$string = $magic;" 
    "$rel1 = '[variable top]' $s;" 
    "$p1 = ($string $s '|' $s)? 25%;" 
    "$p2 = (\\\\ $s $string $s)? 25%;" 
    "$rel2 = $p1 $string $s $p2;" 
    "$relation = $relationList $s ($rel1 | $rel2) $crlf;" 

    "$reset = '&' $s ($beforeList $s)? 10% ($positionList 1% | $string 10%) $crlf;" 
    "$mostRules = $command 1% | $reset 5% | $relation 25%;"
    "$root = $command{0,5} $reset $mostRules{1,20};"
    ;
    
    pass = pass && test(collationBNF);


    return pass;
}
static UBool TestLanguageGenerator(){
    //LanguageGenerator g;
    //const char *const s = "$s = p 0% | q 1%;";
    //g.parseBNF(s, "$s");
    UBool pass;
    //= strcmp("q", g.next()) == 0;

    const char *const def = 
        //"$a = $b;"
        //"$b = $c;"
        //"$c = $t;"
        //"$t = abc $z{1,2};"
        //"$k = a | b | c | d | e | f | g ;"
        //"$z = q 0% | p 1% | r 1%;"
        "$x = a ? 0%;"
        ; // end of string
//    const char * s = "abczz";
//
//
    LanguageGenerator g;
    pass = g.parseBNF(def, "$x",TRUE);
////    LanguageGenerator g(collationBNF, "$root", "$magic", new MagicNode());
//  
    if (pass != LanguageGenerator::OK) return FALSE;
    
    DUMP_R(TestLanguageGenerator, g, 20);
    return pass;

    ////UBool pass = strcmp(s,r) == 0;

    //if (pass){
    //    printf("TestRandomLanguageGenerator passed.\n");
    //} else {
    //    printf("TestRandomLanguageGenerator FAILED!!!\n");
    //}
    //return pass;
}

static UBool TestMorph(){
    srand((unsigned)time( NULL ));

    Alternation * alt = new Alternation();

    (*alt)
    .append(new Literal("a")).append(new Literal("b")).append(new Literal("c"))
    .append(new Literal("d")).append(new Literal("e")).append(new Literal("f"))
    .append(new Literal("g")).append(new Literal("h")).append(new Literal("i"))
    .append(new Literal("j")).append(new Literal("k")).append(new Literal("l"))
    .append(new Literal("m")).append(new Literal("n")).append(new Literal("o"))
    ;

    Repeat * rep = new Repeat( alt ,5,5 );
    Morph m( *rep);

//    DUMP_R(TestMorph,(*rep),20);
    DUMP_R(TestMorph,m,100);

    return FALSE;
}

void TestWbnf(void){
    srand((unsigned)time( NULL ));

    //CALL(TestLiteral);
    //CALL(TestSequence);
    //CALL(TestSymbolTable);
    //CALL(TestVariable);

    //TestRepeat();
    //TestAlternation();
    //TestMorph();

    //TestQuote();
    //TestBuffer();
    //TestWeightedRand();

    //CALL(TestScanner);
    //CALL(TestParser);
    CALL(TestLanguageGenerator);
}

#endif /* _WBNFTEST */
