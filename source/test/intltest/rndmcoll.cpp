/*
 ******************************************************************************
 * Copyright (C) 2005-2005, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <time.h>
#include "rndmcoll.h"

#if !UCONFIG_NO_COLLATION

//Raymond: Following comments are copied from Java implementation
//
// each rule can be:
//      "[" command "]"
//      "& [" position "]"
//      "&" before chars
//      relation "[variable top]"
//      relation (chars "|")? chars ("/" chars)?
// plus, a reset must come before a relation

//Raymond: The grammar of "collation rule" can be defined use a modified-BNF.
//         We need a tool to
//  1. Parse the defination  and
//  2. Build an active object which can generate concrete collation rules
//
//Rammond:
//  The difference between standarad BNF and our modified-BNF is
//  1. Alternation item can has a "weight" now
//  2. Accept "? weight" as a new operation -- short form altheration
//  3. Accept "range" as a new operation -- repeat
//  We do not accept any EBNF grammar in our modified-BNF. 
//
//  Furthermore, the grammar of our modified-BNF itself can be defined using standard BNF
//  NOTE: Following characters are treated as literal in the definition
//        { } ? $ % , - ;
//
//  string          = 
//  alphabeta       =
//  digit           =
//  integer         = integer digit | digit
//  var             = var alphabet | var digit | $ alphabet
//  
//  var-defs        = var-defs var-def | var-def
//  var-def         = var '=' defination;
//  
//  defination      = simple | repeat | short-alt | sequence | alternation1 | alternation2
//  defination      = alternation1 | alternation2
//  
//  simple          = var | string | '(' defination ')'
//  repeat          = simple range
//  short-alt       = simple ? | simple ? weight
//  
//  item            = simple | repeat | shor-alt
//  sequence        = sequence item  | item item
//  
//  item1           = sequence
//  alternation1    = alternation1 '|' item1  | item1 '|' item1
//  
//  item2           = simple weight
//  alternation2    = alternation2 '|' item2 | item2
//  
//  range           = { integer , integer }
//  weight          = integer %
//
// Special-characters:
//         (sapce) contact operation, or separators to increase readability
// =       definition
// |       selection operation
// ( )     precedence select
// ' '     override special-character to plain character
//
/////////////////////////////////////////
// Completeness vs. Magic:
//  The modified-BNF definition of  "collation rule"  need not be complete.
//  It means following assertion is do acceptable:
//    o Some variables are undefined.     or 
//    o We cannot get a "collation rule" according the modified-BNF definition.
//  Let's explain:
//
//  Our target is to build an active object which can generate concrete collation rules.
//
//  In order to formalize the generating process, we used modified-BNF to describe it.
//  Then, the parser will help us to build an complex active object from basic active objects.
//
//  It's acceptable that some basice active object is defined outside the definition and magically injected into.
//
//  The magic power is got via empty variable defination. After parser pasing the definition,
//  we get a part-defined active object, then we inject some magic active objects to 
//  change the prat-defined active object to a complete active object.
//
// Following are copied from Java implementation with less modification.
static const char collationBNF[] =
    "$s = ' '? 50%;" 
    "$crlf = '\r\n';" 

    "$alternateOptions = non'-'ignorable | shifted;" 
    "$onoff = on | off;" 
    "$caseFirstOptions = off | upper | lower;" 
    "$strengthOptions = '1' | '2' | '3' | '4' | 'I';" 
    "$commandList = '['"
    " ( alternate ' ' $alternateOptions"
    " | backwards' 2'"
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
    "$p2 = ('/' $s $string $s)? 25%;" 
    "$rel2 = $p1 $string $s $p2;" 
    "$relation = $relationList $s ($rel1 | $rel2) $crlf;" 

    "$reset = '&' $s ($beforeList $s)? 10% ($positionList 1% | $string 10%) $crlf;" 
    "$mostRules = $command 1% | $reset 5% | $relation 25%;"
    "$root = $command{0,5} $reset $mostRules{1,20};";

// Document of class LiteralToEscape
//
// ATTENTION: 
// From http://icu.sourceforge.net/userguide/Collate_Customization.html.
// We get the precedence of escape/quote operations
//
//     (highest) 1. backslash               \
//               2. two single quotes       ''
//               3. quoting                 ' '
//
// ICU Collation should accept following as the same string.
//
// 1)  'ab'c        _
// 2)  a\bc          \
// 3)  a'b'\c        |- They are equal.
// 4)  abc          _/
//
// From "two single quotes", we have following deductions
//    D1. empty quoting is illgal. (obviously)
//    D2. no contact operation between two quotings   
//              '.''.'      is not ..   it is .'.
//    D3. "two single quotes" cannot contact two quoting simultaneously
//              '..''''.'   is not ..'. it is ..''.
//       NOTICE:
//        "two single quotes" can contact before one quoting
//              '''.'       is '.
//        "two single quotes" can literally contact after one quoting
//        But, from syntax, it's one quoting including a "two single quotes"
//              '.'''       is .'
//    D4. "two single quotes" cannot solely be included in quoting
//              ''''        is not '    it is ''
//       NOTICE:  These are legal
//              '.''.'      is .'.
//              '.'''       is .'
//
//                 dicision
//                    /\
//                   /__\
//      output buffer    input buffer
// 
// To make our dicision (within an atom operation) without caring input and output buffer,
// following calling pattern (within an atom operation) shall be avoided
//
//    P1 open_quoting()  then close_quoting()    (direct violation)   D1
//    P2 close_quoting() then open_quoting()     (direct violation)   D2
//    P3 empty open_quoting()                    (indirect violation) D1, D4
//    P4 empty close_quoting()                   (indirect violation) D2, D3
//    P5 open_quoting()  then two single quotes  (indirect violation) D4
//    P6 close_quoting() then two single quotes  (indirect violation) D3
//
// two single quotes escaping will not open_ or close_ quoting()
// The choice will not lose some quoing forms.
//
// For open_quoting(), 
// we may get this form quoting     '''         P5
// It may raise a bug               ''''x
// If we expect
//      '''.'       let the next char open the quoting
//      '.''.'      the quoting is already opened by preceding char
//
// For close_quoting()
// we will get this form quoting    '.'''       P6
// It may raise a bug               '.''''.'
// If we expect          
//      '.'''\.     let the next char close the quoting
//      '.''''.'    the expectation is wrong!  using  '.'\''.' instead
//
// It's a hard work to readjust generation opportunity for various escaping form.
// We just simply ignore it.


static const char DIGIT_CHAR[] = "0123456789";
static const char WHITE_SPACE[] = {'\t', ' ', '\r', '\n', 0};
static const char ALPHABET[] = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

static inline UBool isInList(const char c /*in*/, const char list[] /*in*/){
    const char * p = list;
    for (;*p != 0 && *p != c; p++);
    return *p?TRUE:FALSE;
}
static inline UBool isDigit(char c) {return isInList(c, DIGIT_CHAR);}
static inline UBool isWhiteSpace(char c) {return isInList(c, WHITE_SPACE);}
static inline UBool isAlphabet(char c) {return isInList(c, ALPHABET);}
static inline UBool isSpecialAsciiChar(char c) {
    	return (c >= 0x0021 && c <= 0x007E &&
		!((c >= 0x0030/*'0'*/ && c <= 0x0039/*'9'*/) ||
		(c >= 0x0041/*'A'*/ && c <= 0x005A/*'Z'*/) ||
		(c >= 0x0061/*'a'*/ && c <= 0x007A/*'z'*/)));
}

// Utility class, can be treated as an auto expanded array. no boundary check.
class Buffer_byte{
    typedef char byte;
    byte * start;
    byte * current;
    int buffer_size; // size unit is byte

public:
    inline int content_size(){return current - start;} // size unit is byte

private:
    inline void expand(int add_size = 100){ // size unit is byte
        int new_size = buffer_size + add_size;

        int cs_snap = content_size();         
        start = (byte *) realloc(start, new_size);   // may change the value of start
        current = start + cs_snap;

        memset(current, 0, add_size);
        buffer_size = new_size;
    }

    inline void expand_to(int size){
        int r = size - buffer_size;
        if (r > 0) {
            expand(r);  // simply expand, no block alignment
        }
    }
public:
    Buffer_byte():start(NULL),current(start),buffer_size(0){
        expand();
    }
    ~Buffer_byte(){
        free(start);
    }

    inline void reset(){
        start != NULL ? memset(start, 0, buffer_size) : 0;
        current = start;
    }

    // Using memory copy method to append a C array to buffer, 
    inline void append(const void * c, int size){ // size unit is byte
        expand_to(content_size() + size) ;
        memcpy(current, c, size);
        current = current + size;
    }
    void * operator &(){
        return start;
    }
};
    
//template<typename type>
//    class BUFFER{
#define BUFFER(type, name)\
    class name {\
    private:\
       Buffer_byte buf;\
    public:\
        void reset() {buf.reset();}\
        void append(type c) {buf.append(&c, sizeof(type));}\
        void append_array(const type * p, int size) {buf.append(p, sizeof(type)*size);}\
        type * operator &(){return (type *) &buf;}\
        type & operator [] (int i) { return operator&()[i];}\
        operator type *(){return operator&();}\
        int content_size(){return buf.content_size() / sizeof(type);}\
    }
    
class Node;
BUFFER(char, Buffer_char);
BUFFER(int, Buffer_int);
BUFFER(Node *, Buffer_pNode);

/* Helper class
    * Encoding a string literal to a valid collation escaping string.
    * See documents in anonymous.design
    */
class LiteralToEscape{
public:
    enum CHOICE {YES, NO, RAND};
    enum ESCAPE_FORM {BSLASH_ONLY, QUOTE_ONLY, QUOTE_AND_BSLAH, RAND_ESC};
private:
    class Bool{ // assigned or random value
    public:
        operator UBool() {   // conversion operator
            if (tag == RAND){
                return rand()%2 == 1;
            } else {
                return tag == YES ? TRUE : FALSE;
            }
        }
        Bool(CHOICE flag=RAND):tag(flag){}
    private:
        CHOICE tag;
    };
public:
    LiteralToEscape(CHOICE escapeLiteral = RAND,
        CHOICE twoQuotesEscape = RAND,
        ESCAPE_FORM escapeForm = RAND_ESC):
        escape_form(escapeForm),
        escape_literal(escapeLiteral),
        two_quotes_escape(twoQuotesEscape),
        is_quoting(FALSE){}
private:
    Buffer_char str;
    ESCAPE_FORM escape_form;
    Bool escape_literal;
    Bool two_quotes_escape;
    UBool quote_escape;
    UBool bslash_escape;
    UBool is_quoting;

    void set_options(){
        ESCAPE_FORM t = escape_form == RAND_ESC ? (ESCAPE_FORM) (rand()%3) : escape_form;
        switch (t){
                case BSLASH_ONLY :
                    bslash_escape = TRUE; quote_escape = FALSE; break;
                case QUOTE_ONLY:
                    bslash_escape = FALSE;quote_escape = TRUE;  break;
                case QUOTE_AND_BSLAH:
                    bslash_escape = TRUE; quote_escape = TRUE;  break;
                default:
                    ;// error
        }
    }

    void reset(){
        str.reset();
        is_quoting = FALSE;
    }

    inline void open_quoting(){ 
        if(is_quoting){
            // do nothing
        } else {
            str.append('\'');
            is_quoting = TRUE;
        }
    }
    inline void close_quoting(){
        if(is_quoting){
            str.append('\'');
            is_quoting = FALSE;
        } else {
            // do nothing
        }
    }

    // str  [in]    null-terminated c-string
    void append(const char * str){
        for(;*str != 0; str++){
            append(*str);
        }
    }

    inline void append(const char c){
        set_options();

        if (c == '\\'){
            quote_escape ? open_quoting() : close_quoting();
            //bslash_escape always true here
            str.append('\\');
            str.append('\\');
        } else if (c == '\''){
            if (two_quotes_escape){     // quoted using two single quotes
                // See documents in anonymous.design
                str.append('\'');
                str.append('\'');
            } else{
                quote_escape ? open_quoting() : close_quoting();
                //bslash_escape always true here
                str.append('\\');
                str.append('\'');
            }
        } else if (isSpecialAsciiChar(c) || isWhiteSpace(c)){
            quote_escape  ? open_quoting()   : close_quoting();
            if (bslash_escape) str.append('\\');
            str.append(c);
        } else { //if (isAlphabet(c) || isDigit(c) || TRUE){ // treat others as literal
            if (escape_literal){
                quote_escape  ? open_quoting()   : close_quoting();
                if (bslash_escape)  str.append('\\');
                str.append(c);
            } else {
                close_quoting();
                str.append(c);
            }
        }
    }

public:
    // Return a null-terminate c-string. The buffer is owned by callee.
    char * operator()(const char * literal /*c-string*/){
        str.reset();
        for(;*literal != 0; literal++){
            append(*literal);
        }
        close_quoting();    // P4 exception, to close whole quoting
        return str;
    }
};


enum TokenType {STRING, VAR, NUMBER, WEIGHT, STREAM_END, ERROR, QUESTION_MARK,RANG_START,RANG_END, LPAR, RPAR, SEMI, EQ, COMMA, BAR};

/* A simple complier scanner to get token from source string.
    * 
    * The result is put in this->tokenBuffer 
    * The buffer is owned by Scanner, and will be destoried in next call for getNextToken()
    */
class Scanner{
public:
    // source [in] null-terminated c-string
    Scanner(const char *source/*c-string*/):source(source), working(source), history(source){
    }

    char tokenBuffer[50];   //null terminated c-string. LIMITATION & ASSUMPTION here
    TokenType tokenType;
    
    /* this->working        [in]
        * this->tokenBuffer    [out]
        * this->tokenType      [out]
        */
    TokenType getNextToken(){
        history = working;
        p_b = tokenBuffer;  // for simplicity, no buffer overflow will be checked
        tokenType = ERROR;
        StateType state = START;
        while (state != DONE){
            char c = *working++;
            switch(state){
                case START:
                    if (isWhiteSpace(c)){
                        // do nothing, skip 
                    } else if (isDigit(c)){
                        *p_b++ = c; // no overflow check
                        state = IN_NUM;
                    } else if (isAlphabet(c)){
                        *p_b++ = c; // no overflow check
                        state = IN_STRING;
                    } else if (c == '$'){
                        *p_b++ = c; // no overflow check
                        state = IN_VAR;
                    } else if (c == '\''){
                        state = IN_QUOTE;
                    } else if (c == '\\'){
                        state = IN_BSLASH;
                    } else if (c == 0){
                        tokenType = STREAM_END;
                        state = DONE;
                        working--;
                    } else{
                        switch(c){
                            case '?': tokenType = QUESTION_MARK; break;
                            case '{': tokenType = RANG_START; break;
                            case '}': tokenType = RANG_END; break;
                            case '(': tokenType = LPAR; break;
                            case ')': tokenType = RPAR; break;
                            case ';': tokenType = SEMI; break;
                            case '=': tokenType = EQ; break;
                            case ',': tokenType = COMMA; break;
                            case '|': tokenType = BAR; break;
                            default:  tokenType = ERROR;
                        }
                        //Raymond: Can we gracefully remove the unnecessary test?
                        //     ==  Can we write a more beautiful 'switch' statement?
                        if (tokenType == ERROR){
                            working--;
                            *p_b = 0;
                        } else {
                            *p_b++ = c; // tokenBuffer[0], no overflow check 
                            *p_b++ = 0; // tokenBuffer[1], no overflow check
                        }
                        state = DONE;
                    }
                    break;//START
                case IN_NUM:
                    if (isDigit(c)){
                        *p_b++ = c; // no overflow check
                    } else if (c == '%'){ // no blank space between NUMBER and % symbol
                        *p_b++ = c;
                        *p_b = 0;
                        tokenType = WEIGHT;
                        state = DONE;
                    } else {
                        working--; // reset working point to current character
                        tokenType = NUMBER;
                        *p_b = 0;
                        state = DONE;
                    }
                    break;//IN_NUM
                case IN_VAR:
                    if (isAlphabet(c) || isDigit(c)){ // For simplicity, digit can be the leading char
                        *p_b++ = c; // no overflow check
                    } else {
                        working--;
                        *p_b = 0;
                        tokenType = VAR;
                        state = DONE;
                    }
                    break;//IN_VAR
                case IN_STRING:
                    if (c == '\''){
                        state = IN_QUOTE;
                    } else if (c =='\\'){ // NOTE: escaping for C language syntax here
                        state = IN_BSLASH;
                    } else if (isAlphabet(c) || isDigit(c)){
                        *p_b++ = c; // no overflow check
                    } else{
                        working--;
                        *p_b = 0;
                        tokenType = STRING;
                        state = DONE;
                    }
                    break;//IN_STRING
                case IN_QUOTE:
                    if (c == '\''){
                        state = IN_STRING; // Yes, IN_STRING
                    } else {
                        *p_b++ = c;  // no tokenBuffer overflow check !!!
                    }
                    break;//IN_QUOTE
                case IN_BSLASH:
                    if (c == 'n') {
                        *p_b++ = '\n'; // no tokenBuffer overflow check
                    } else if (c == 'r'){
                        *p_b++ = '\r'; // no tokenBuffer overflow check
                    } else if (c == 't'){
                        *p_b++ = '\t'; // no tokenBuffer overflow check
                    } else if (c == '\''){ // NOTE: escaping for C language syntax here
                        *p_b++ = '\''; // no tokenBuffer overflow check
                    } else {
                        working--;
                    }
                    state = IN_STRING; // Yes, IN_STRING
                    break;//IN_BSLASH
                case DONE:  /* should never happen */
                default:
                    working--;
                    *p_b = 0;
                    tokenType = ERROR;
                    state = DONE;
                    break;
            }//switch(state) 
        }//while (state != DONE)

        return tokenType;
    }

    inline void ungetToken(){
        working = history;
    }
    inline void dumpCurrentPoint(){
        printf("\n______________________________________________________________________________\n");
        fwrite(source, history - source, 1, stdout);
        printf("\n=====current token=====\n");
        fwrite(history, working - history, 1,stdout);
        printf("\n>>>>>current point>>>>>\n");
        //printf(working); // This function will consume some characters, for example  % 
        int len = strlen(working);
        fwrite(working, len, 1, stdout);
        printf("\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
    }
private:
    const char * source;
    const char * working;
    const char * history;
    char * p_b;
    enum StateType {START, IN_NUM, IN_VAR, IN_QUOTE,  IN_BSLASH, IN_STRING, DONE};
};//class Scanner
    

class Node{
public:
    // Return a null-terminated c-string. The buffer is owned by callee.
    virtual const char* getTargetString() = 0;
    virtual ~Node(){};
};

/* Helper class.
    * It's a mapping table between 'variable name' and its 'active Node object'
    */
class NodeSymbolTable{
private:
    Buffer_int   names;         // indexes in name_buffer
    Buffer_pNode refs;
    Buffer_char  name_buffer;   // var names storage space
    int get_var_name_index(const char *const var_name){
        int len = names.content_size();
        for (int i=0; i< len; i++){
            if (strcmp(var_name, &name_buffer + names[i]) == 0){
                return i;
            }
        }
        return -1;
    }

public:
    UBool is_var_exist(const char *const var_name /*c-string*/){
        return get_var_name_index(var_name) == -1? FALSE : TRUE;
    }
    UBool does_var_has_ref(const char *const var_name /*c-string*/){
        int i = get_var_name_index(var_name);
        if (i == -1){
            return FALSE;
        } else {
            return refs[i] == NULL ? FALSE : TRUE;
        }
    }
    Node * get_var_ref(const char *const var_name /*c-string*/){
        int i = get_var_name_index(var_name);
        if (i == -1){
            printf("name NOT exist: %s\n", var_name);
            return NULL;
        } else {
            if (refs[i]){
                //printf("name and ref exist: %s\n", var_name);
            } else {
                printf("name exist, ref NOT exist: %s\n", var_name);
            }
            return refs[i];
        }
    }

    void put_var(const char *const var_name, Node *const var_ref = NULL){
        int i = get_var_name_index(var_name);
        if (i == -1 && var_name !=NULL){ // new variable
            int offset = name_buffer.content_size();
            name_buffer.append_array(var_name, strlen(var_name) + 1);
            names.append(offset);
            refs.append(var_ref);
        } else {
            if(refs[i] == NULL && var_ref != NULL){ // exist variable, no ref
                refs[i] = var_ref;    // link definition with variable
            };
        }
    }
    void reset(){
        names.reset();
        name_buffer.reset();

        // release memory here
        int s = refs.content_size();
        for (int i=0; i < s; i++){
            delete refs[i];
        }
        refs.reset();
    }
};


class LiteralNode : public Node {
public:
    virtual const char* getTargetString(){
        return str;
    }
    LiteralNode(const char * s /*c-string*/){
        str.append_array(s, strlen(s) + 1);
    }
private:
    Buffer_char str; //null-terminated c-string
};

class VariableNode : public Node {
public:
    VariableNode(const char * var_name, NodeSymbolTable * symbols):symbols(*symbols){
        this->var_name.append_array(var_name, strlen(var_name) + 1);
        this->var_ref = NULL;
    }
    UBool link(){
        if (var_ref == NULL) {
            var_ref =  &symbols == NULL ? NULL : symbols.get_var_ref(var_name);
            return var_ref != NULL;
        }
        return TRUE;
    }
    virtual const char* getTargetString(){
        link();
        if (var_ref == NULL) {
            return "";  // constant string has global life-cycle
        }
        return var_ref->getTargetString();
    }
private:
    Buffer_char var_name;
    Node * var_ref;
    NodeSymbolTable & symbols;
};

class Magic_SelectOneChar : public Node{
public:
    virtual const char* getTargetString(){
        return &set + rand() % len;
    }

    Magic_SelectOneChar( const char * set /*char set*/): len(strlen(set)){
        this->set.append_array(set, len);
    }
private:
    Buffer_char set;
    const int len;
};

class MagicNode : public Node {
private:
    LiteralToEscape l;
    Buffer_char str;
    // randomly select a char from a set
    char select_an_char(){
        static const char *const set = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ[]&<";
        static const int len = strlen(set);
        int i = rand()%len;
        return set[i];
    }
    // compose a string with lenght {1, 5}
    const char * select_an_string(){
        int r = rand();
        r %= 5;
        r += 1; // shift 0..4 to 1..5

        str.reset();
        for (int i=0; i < r; i++){
            str.append(select_an_char());
        }
        str.append(0);
        return &str;
    }
public:
    virtual const char* getTargetString(){
        return "aaa";
        return l(select_an_string());
    }
};
        
class SequenceNode : public Node {
public:
    virtual const char* getTargetString(){
        str.reset();
        int l = items.content_size();
        for(int i=0; i < l; i++){
            const char * temp = items[i]->getTargetString();
            str.append_array(temp, strlen(temp));
        }
        str.append(0); // terminal null
        return str;
    }

    void append (Node * node){
        items.append(node);
    }

    virtual ~SequenceNode(){
        int l = items.content_size();
        for(int i=0; i < l; i++){
            //How can assure the item is got from heap?
            //Let's assume it.
            delete items[i];
        }
    }
private:
    Buffer_pNode items;
    Buffer_char  str; //null-terminated c-string
};

class RepeatNode : public Node {
private:
    Node * item;
    Buffer_char str;
    int min_count;
    int max_count;
    int select_a_count(){
        int t = max_count - min_count + 1;
        return min_count + rand()%(t);
    }
public:
    virtual const char* getTargetString(){
        str.reset();
        for(int i=0; i< select_a_count(); i++){
            const char * temp = item->getTargetString();
            str.append_array(temp, strlen(temp));
        }
        str.append(0);
        return str;
    }

    RepeatNode(Node * item, int min_count =0, int max_count = 1){
        this->item = item;
        this->min_count = min_count;
        this->max_count = max_count;
    }
    virtual ~RepeatNode(){
        delete item; // We assume its space is got from heap
    }
};
class AlternationNode : public Node {
public:
    virtual const char* getTargetString(){
        str.reset();
        int i = select_an_item();
        const char * temp = items[i]->getTargetString();
        str.append_array(temp, strlen(temp));
        str.append(0);
        return str;
    }
    virtual ~AlternationNode(){
        int l = items.content_size();
        for(int i=0; i < l; i++){
            delete items[i]; // We assume its space is got from heap
        }
    }
protected:
    Buffer_pNode items;
private:
    Buffer_char str; // null-terminated c-string
    // Select an item randomly and add it to target string
    virtual int select_an_item() = 0;
};

class Alternation1Node : public AlternationNode{
public:
    void append (Node * node){
        items.append(node);
    }
private:
    int select_an_item(){
        int entries = items.content_size();
        int i = rand()%entries;
        return i;
    }
};

class Alternation2Node : public AlternationNode{
public:
    void append (Node * node, int weight){
        items.append(node);
        weights.append(weight);
        total += weight;
    }
    Alternation2Node():total(0){}
private:
    Buffer_int weights;
    double total;
    
    // Select an item randomly. Hight weight item has more chance to be selected.
    //                 
    //  +____+_+___+______+   <- total weight
    //           ^mark   \__ one item
    // 
    // We use following method to select an item.
    // 1.locate a point in total weight randomly --> mark
    //     every weight has equal chance to be select
    // 2.mark can identify an item --> item
    //     hight weight has more chance to be selected.
    // 
    int select_an_item(){
        double reference_mark = (double)rand()/ (double)RAND_MAX;
        double mark = total * reference_mark;
        int i=0;
        for (;;){
            mark -= weights[i];
            if (mark <= 0)
                break;
            i++;
        }
        return i;
    }
};


class Parser{
private:
    Scanner s;
    TokenType token;
    NodeSymbolTable & symbols;
    
    UBool match(TokenType expected){
        if (token == expected) {
            token = s.getNextToken();
            return TRUE;
        } else {
            //s.dumpCurrentPoint();
            return FALSE;
        }
    }

    UBool weight(int & w){
        if (token == WEIGHT){
            w = atoi(s.tokenBuffer);
            match(WEIGHT);
            return TRUE;
        }
        return FALSE;
    }

    // get a 'simple node'
    UBool simple(Node* &node /*out*/){
        if (node != NULL) return FALSE;
        //assert node == NULL
        switch(token){
            case LPAR:
                match(LPAR);
                if(defination(node) && match(RPAR)){
                    return TRUE;
                }
                return FALSE;
            case VAR:
                node = new VariableNode(s.tokenBuffer, &symbols);
                match(VAR);
                return TRUE;
            case STRING:
                node = new LiteralNode(s.tokenBuffer);
                match(STRING);
                return TRUE;
            default:
                return FALSE;
        }
    }

    UBool alternation2(Node * &node /*in,out*/){
        if (node == NULL) return FALSE;
        //assert node != NULL, and is simple node

        int w;
        if (!weight(w)){
            delete node;
            node = NULL;
            return FALSE;
        }
        
        // Raymond: (For interest and study purpose)
        //   We accept alternation2 with only one item, although I do think it is meanfull.
        //
        //   Single item alternation2 should equal to "a simple without weight" rather than a short-alt
        //
        //   Another reasone is, we think 'weight' should be owned by alternation2 rather than item2 itself.

        Alternation2Node * t = new Alternation2Node();
        t->append(node, w);

        node = NULL;        // Logically, it has nothing
        Node * temp = NULL; // We can use 'node' as temp variable, but its name is uncomfortable

        while (token == BAR){
            match(BAR);
            if (simple(temp)){
                if (weight(w)){
                    t->append(temp, w);
                } else {
                    delete temp;
                    goto FAIL;
                }
                temp = NULL;    // Logically, it has nothing now
            } else {
                goto FAIL;
            }
        }

        if (token == SEMI || token == RPAR){
            node = t;   // A whole new node
            return TRUE;
        }
        // for example, this is illegal:  a 4% | b 5% c

FAIL:
        delete t;   // fall down...
        return FALSE;
    }

    UBool sequence_open(Node* &node){
        if (node == NULL) return FALSE;
        // assert node != NULL, and node is item (simple, repeat, or short-alt)

        SequenceNode* t = new SequenceNode();
        t->append(node);

        node = NULL;        // Logically, it has nothing
        Node * temp = NULL; // We can use 'node' as temp variable, but its name is uncomfortable

        while (token == VAR || token == STRING || token == LPAR){ // maybe a simple
            if (item(temp)){
                t->append(temp);
                temp = NULL;
            } else {
                goto FAIL;
            }
        }
        // ILLEGAL: a c 5%
        if (token == SEMI || token == RPAR || token == BAR){
            node = t;
            return TRUE;
        }
FAIL:
        delete t;
        return FALSE;

    }

    //upgrade a 'simple node' to 'repeat node'
    UBool repeat (Node* &node /*in,out*/){
        if (node == NULL) return FALSE;
        //assert node != NULL, node is simple

        if (match(RANG_START) && token == NUMBER){
            int min = atoi(s.tokenBuffer);
            match(NUMBER);
            if(match(COMMA) && token == NUMBER){
                int max = atoi(s.tokenBuffer);
                match(NUMBER);
                if(match(RANG_END)){
                    Node * t = node;
                    node = new RepeatNode(t, min, max);
                    return TRUE;
                }
            }
        }
        delete node;
        node = NULL;
        return FALSE;
    }

    //upgrade a 'simple node' to 'short-alt node'
    UBool short_alt (Node* &node /*in,out*/){
        if (node == NULL) return FALSE;
        //assert node != NULL, node is simple

        if (match(QUESTION_MARK)){
            int exist_weight = 50;
            if (token == WEIGHT){
                exist_weight = atoi(s.tokenBuffer);
                match(WEIGHT);
            }
            int null_weight = 100 - exist_weight;
            Node * t1 = node;
            Node * t2 = new LiteralNode("");
            Alternation2Node * t = new Alternation2Node();
            t->append(t1, exist_weight);
            t->append(t2, null_weight);
            node = t;
            return TRUE;
        }
        delete node;
        node = NULL;
        return FALSE;
    }

    UBool item(Node *& node /*out*/){
        if (node != NULL){
            // assert node is simple
            // go on
        } else {
            if (simple(node)){
                // go on
            } else {
                return FALSE;
            }
        }

        // assert node != NULL, node is simple
        switch (token){
            case RANG_START:
                return repeat(node);
            case QUESTION_MARK:
                return short_alt(node);
            default:
                return TRUE;  // bare simple
        }
    }


    UBool sequence(Node* &node){
        if (!item(node)) {
            return FALSE;
        }

        if (token == VAR || token == STRING || token == LPAR){ // maybe an item
            return sequence_open(node);
        } else { // just something with higher precedence.
            return TRUE;
        }
    }

    UBool alternation1_open(Node * &node){
        if (node == NULL) return FALSE;
        // assert node != NULL, and node is sequence or simpler thing

        Alternation1Node * t = new Alternation1Node();
        t->append(node);

        node = NULL;        // Logically, it has nothing
        Node * temp = NULL; // We can use 'node' as temp variable, but its name is uncomfortable

        // We can use either recursion (linking node) or loop (plain array) to create the list
        // Here, we chosse loop (plain array).
        while (token == BAR){
            match(BAR);
            if(sequence(temp)){
                t->append(temp);
                temp = NULL;
            } else {
                goto FAIL;
            }
        }

        if (token == SEMI || token == RPAR){
            node = t;
            return TRUE;
        }
FAIL:
        delete t;
        return FALSE;
    }

    UBool alternation1(Node * &node){
        if (!sequence(node)){
            return FALSE;
        }

        if (token == BAR){ // detected a real alternation1, create it.
            return alternation1_open(node);
        } else { // just something with higher precedence, not a alternation1
            return TRUE;
        }
    }


    UBool defination(Node* &node /*in,out*/){
        if (node != NULL) return FALSE;
        //assert node == NULL
        if (simple(node)){
            if (token == WEIGHT){
                return alternation2(node);
            } else {
                return alternation1(node);
            }
        }
        return FALSE;
    }

    UBool rule(){
        if (token == VAR){
            Buffer_char name;
            name.append_array(s.tokenBuffer, strlen(s.tokenBuffer));
            name.append(0);
            match(VAR);

            if (match(EQ)){
                Node * t = NULL;
                if(defination(t)){
                    symbols.put_var(name, t);
                    return match(SEMI);
                }
            }
        }
        return FALSE;
    }

    UBool rules(){
        symbols.reset();
        token = s.getNextToken();
        while (rule()){
        }
        if (token == STREAM_END){
            return TRUE;
        } else {
            s.dumpCurrentPoint();
            return FALSE;
        }
    }

public:
    Parser(const char * source, NodeSymbolTable * symbols):s(source), symbols(*symbols){
    }
    UBool parse(){
        return rules();
    }
}; // class Parser

class RandomLanguageGenerator{
public:
    void put_magic(const char *const magic_name, Node *const magic_ref){
        symbols.put_var(magic_name, magic_ref);
    }

    // Return a null-terminated c-string. The buffer is owned by callee.
    const char * get_a_string(){
        return root->getTargetString();
    }

    //NOTE: start cannot be a magic node
    RandomLanguageGenerator(const char *const bnf_definition, 
                            const char *const start,
                            const char *const magic_name = NULL,
                            Node *const magic_ref = NULL){

        srand((unsigned)time( NULL ));  
        // our random sequence is start from here. 
        // side effect: It's a global C function!

        Parser p(bnf_definition, &symbols);
        if (!p.parse()) {return;}     // how can we break when encounter error?
        root = symbols.get_var_ref(start);
        put_magic(magic_name, magic_ref);
    }

private:
    Node * root;
    NodeSymbolTable symbols;
};

UBool TestScanner(void){
    //const char str1[] = "$root = $command{0,5} $reset $mostRules{1,20};";
    //const char str1_r[][20] = {"$root", "=", "$command", "{", "0", ",", "5", "}", 
    //    "$reset", "$mostRules", "{", "1", ",", "20", "}", ";"};

    const char str2[] = "$p2 =('\\' $s $string $s)? 25%;";
    const char str2_r[][20] = {"$p2", "=", "(", "\\", "$s", "$string", "$s", ")", "?", "25%", ";"};

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
            pass = strcmp(t.tokenBuffer, str_r[i++]) == 0 ;
            t.getNextToken();
        }
    }
    if (pass){
        printf("TestScanner passed.\n");
    } else {
        printf("TestScanner FAILED!!!\n");
        t.dumpCurrentPoint();
    }
    return pass;
}

UBool TestLiteralizer(){
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
    
    //LiteralToEscape l(LiteralToEscape::NO, LiteralToEscape::NO, LiteralToEscape::RAND_ESC);
    LiteralToEscape l;

    printf("\n========TestLiteralier start=======\n");
    printf(str);
    printf("\n-----------------------------------\n");
    //printf(r);
    for (int i=0; i<10; i++){
        const char * s = l(str);
        fwrite(s, strlen(s), 1, stdout);
        printf("\n");
    }
    printf("\n~~~~~~~~TestLiteralier end~~~~~~~~~~\n");

    // UBool pass = strcmp(str_r,l(str)) == 0;

    //if (pass){
    //     printf("TestLiteralier passed.\n");
    // } else {
    //     printf("TestLiteralier FAILED!!!\n");
    // }
    // return pass;
    return FALSE;
}
UBool TestLiteralNode(){
    const char * s = "test string99.";
    LiteralNode n(s);
    const char * r = n.getTargetString();

    UBool pass = strcmp(s,r) == 0;

    if (pass){
        printf("TestLiteralNode passed.\n");
    } else {
        printf("TestLiteralNode FAILED!!!\n");
    }
    return pass;
}

UBool TestMagicNode(){
    MagicNode n;
    
    printf("\n========TestMagicNode start=======\n");
    for (int i=0; i < 10 ; i++){
    printf(n.getTargetString());
    printf("\n------------------\n");
    }
    printf("\n~~~~~~~~TestMagicNode end~~~~~~~~~~\n");
    return FALSE;
}
UBool TestSequenceNode(){
    SequenceNode n;
    LiteralNode * n1 = new LiteralNode("abc ");
    LiteralNode * n2 = new LiteralNode(", s");
    n.append(n1);
    n.append(n2);
    const char * r = n.getTargetString();
    const char * s = "abc , s";

    UBool pass = strcmp(s,r) == 0;

    if (pass){
        printf("TestSequenceNode passed.\n");
    } else {
        printf("TestSequenceNode FAILED!!!\n");
    }
    return pass;
}

UBool TestAlternation1Node(){
    srand((unsigned)time( NULL ));
    Alternation1Node n;
    LiteralNode * a = new LiteralNode("a");
    LiteralNode * b = new LiteralNode("b");
    LiteralNode * c = new LiteralNode("c");
    LiteralNode * d = new LiteralNode("c");
    n.append(a);
    n.append(b);
    n.append(c);
    n.append(d);
    printf("\n========= TestAlternation1Node =============\n");
    for(int i=0; i<10; i++){
        printf(n.getTargetString());
        printf("\n");
    }
    printf("~~~~~~~~~ TestAlternation1Node ~~~~~~~~~~~~~\n");
    return FALSE;
}
UBool TestAlternation2Node(){
    srand((unsigned)time( NULL ));
    Alternation2Node n;
    LiteralNode * n1 = new LiteralNode("boy");
    LiteralNode * n2 = new LiteralNode("gggirl");
    n.append(n1,10);
    n.append(n2,20);
    printf("\n========= TestAlternation2Node = 10, 20 =====\n");
    for(int i=0; i<10; i++){
        printf(n.getTargetString());
        printf("\n");
    }
    printf("~~~~~~~~~ TestAlternation2Node ~~~~~~~~~~~~~\n");
    return FALSE;
}

UBool TestRepeatNode(){
    srand((unsigned)time( NULL ));
    LiteralNode * n1 = new LiteralNode("abc ");
    RepeatNode n(n1, 1, 4);
    printf("\n========= TestRepeatNode =============\n");
    for(int i=0; i<10; i++){
        printf(n.getTargetString());
        printf("\n");
    }
    printf("~~~~~~~~~ TestRepeatNode ~~~~~~~~~~~~~\n");
    return FALSE;
}
UBool TestVariableNode(){
    printf("\n========TestVariableNode===========\n");
    VariableNode n("aaa", NULL);
    printf(n.getTargetString());
    printf("\n~~~~~~~~~ TestVariableNode ~~~~~~~~~~~~~\n");
    return FALSE;
}
UBool TestSymbolTable(){
    LiteralNode * n1 = new LiteralNode("uvw");
    LiteralNode * n2 = new LiteralNode("xyz");
    NodeSymbolTable t;
    t.put_var("abc", n1);
    t.put_var("$aaa", n2);
    t.put_var("bbb");

    UBool pass;
    pass = t.is_var_exist("abc");
    pass = pass && t.is_var_exist("$aaa");
    pass = pass && t.is_var_exist("bbb");
    pass = pass && !t.is_var_exist("ccc");
    pass = pass && t.does_var_has_ref("abc");
    pass = pass && t.does_var_has_ref("$aaa");
    pass = pass && !t.does_var_has_ref("bbb");
    pass = pass && !t.does_var_has_ref("zz");

    t.reset();
    pass = pass && !t.does_var_has_ref("abc");
    if (pass){
        printf("TestSymbolTable passed.\n");
    } else {
        printf("TestSymbolTable FAILED!!!\n");
    }
    return pass;
}

UBool TestParser1(){
    const char *const str1 = 
        "$s = ' ' ? 50%;"
        //"$relationList = '<' | '<<' |  ';' | '<<<' | ',' | '=';"
        "$p1 = ($string $s '|' $s)? 25%;"
        "$p2 = ('\\' $s $string $s)? 25%;"
        "$rel2 = $p1 $string $s $p2;"
        "$relation = $relationList $s ($rel1 | $rel2) $crlf;"
        "$command = $commandList $crlf;"
        //Raymond: Test code in Java source should be fixed to adapt current syntax
        "$reset = '&' $s ($beforeList $s)? 10% ($positionList 100% | $string 10%) $crlf;"
        "$mostRules = $command 1% | $reset 5% | $relation 25%;"
        "$root = $command{0,5} $reset $mostRules{1,20};"

        //"$x = ($var {1,2}) 3%;"         // legal.
        //"$x = $var {1,2} 3% | b 4%;"    // illegal. 3%
        //"$x = $var {1,2} 3%;"           // illegal. 3%
        //"$m = $c ? 2% 4% | $r 5% | $n 25%;"     // should failed at '4%'
        //"$a = b ? 2% | c 5%;"                   // should failed at '5%' 
        //"$x = A B 5% C 10% | D;"        // illegal. 5%
        //"$x = aa 45% | bb 5% cc;"       // illegal. cc
        //"$x = (b 5%) (c 6%);"           // legal.
        //"$x = (b 5%) c 6%;"             // legal? illegal.
        //"$x = b 5% (c 6%);"             // legal? illegal.
        //"$x = b 5% c 6%;"               // legal? illegal, should failed at 'c'
        //"$x = b 5%;"                    // legal
        //"$x = aa 45% | bb 5% cc;"       // should failed at 'cc'
        //"$x = a | b  | c 4% | d 5%;"    // should failed at '4%'
        //"$s = ' ' ? 50% abc;"           // legal.
    ;
    NodeSymbolTable symbol_table;

    Parser p(str1, &symbol_table);

    UBool pass = p.parse();

    symbol_table.reset();
    if (pass){
        printf("TestParser passed.\n");
    } else {
        printf("TestParser FAILED!!!\n");
    }
    return pass;

}
UBool TestRandomLanguageGenerator(){
    const char *const def = 
        "$a = $b;"
        "$b = $c;"
        "$c = $t;"
        "$t = abc z{2,2};"
        "$k = a | b | c | d | e | f | g ;"
        "$z = a 0% | b 1% | c 10%;"
        ; // end of string
    const char * s = "abczz";


    //RandomLanguageGenerator g(def, "$a");
    RandomLanguageGenerator g(collationBNF, "$root", "$magic", new MagicNode());
    
    printf("\n_________ TestRandomLanguageGenerator _____________\n");
    for (int i= 0; i< 5; i++){
        //for (int j = 0; j < 99999999; j++);
        const char * r = g.get_a_string();
        fwrite(r, strlen(r), 1, stdout);
        printf("_____________________________________________________\n");
    }
    printf("~~~~~~~~~ TestRandomLanguageGenerator ~~~~~~~~~~~~~\n");
    return FALSE;

    ////UBool pass = strcmp(s,r) == 0;

    //if (pass){
    //    printf("TestRandomLanguageGenerator passed.\n");
    //} else {
    //    printf("TestRandomLanguageGenerator FAILED!!!\n");
    //}
    //return pass;
}

void RandomCollatorTest::Test2(){
    TestScanner();
    TestLiteralizer();
    TestLiteralNode();
    TestMagicNode();
    TestSequenceNode();
    TestAlternation1Node();
    TestAlternation2Node();
    TestRepeatNode();
    TestVariableNode();
    TestSymbolTable();
    TestParser1();
    TestRandomLanguageGenerator();
}


void RandomCollatorTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* ){
    if (exec) logln("TestSuite RandomCollatorTest: ");
    switch (index) {
        TESTCASE(0, Test);
        default: name = ""; break;
    }
}

void RandomCollatorTest::Test(){
    RandomLanguageGenerator test_rule(collationBNF, "$root", "$magic", new MagicNode());

    //class TestColltorCompare{
    //public:
    //    UBool operator()(Collator &coll, int count = 1000){
    //        UnicodeString a(test_string.get_a_string());
    //        UnicodeString b(test_string.get_a_string());
    //        UnicodeString c(test_string.get_a_string());
    //        do{
    //            if (check_transitivity(coll, a, b, c)){
    //                a = b;
    //                b = c;
    //                c = UnicodeString(test_string.get_a_string());
    //            }
    //        }while(count-- >= 0 );

    //        return FALSE;
    //    }
    //    TestColltorCompare():test_string("$s = $c{1,8};", "$s", "$c", new Magic_SelectOneChar("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ[]&<")){
    //    }
    //private:
    //    UBool check_transitivity(const Collator & coll, const UnicodeString &a, const UnicodeString &b, const UnicodeString &c){
    //        int ab = coll.compare(a,b), ba = coll.compare(b,a);
    //        int bc = coll.compare(b,c), cb = coll.compare(c,b);
    //        int ca = coll.compare(c,a), ac = coll.compare(a,c);
    //        //       a
    //        //      / \ 
    //        //     b - c
    //        //
    //        if (//counter-clockwise, maximum
    //              (ab >=0 && bc >=0 && ac <0)
    //            ||(bc >=0 && ca >=0 && ba <0)
    //            ||(ca >=0 && ab >=0 && cb <0)

    //            //counter-clockwise, minimum
    //            ||(ab <=0 && bc <=0 && ca >0)
    //            ||(bc <=0 && ca <=0 && ba >0)
    //            ||(ca <=0 && ab <=0 && cb >0)
    //            ){
    //                return FALSE;
    //            }
    //          return TRUE;
    //    }

    //    RandomLanguageGenerator test_string;
    //} coll_test;


    static const int CONSTRUCT_RANDOM_COUNT = 10;
    int i;
    for (i=0; i < CONSTRUCT_RANDOM_COUNT; i++){
        const char * rule = test_rule.get_a_string();
        logln("\n-----------------------------------%d\n",i);
        logln(UnicodeString(rule, strlen(rule)));

        UnicodeString newRule(rule);    // potential bug
        UErrorCode status = U_ZERO_ERROR;
        Collator * c = new RuleBasedCollator(newRule,status);

        if (U_FAILURE(status)) {
            errln( "Could not create Collator for rules at %d. Error: %s\nRule is: %s\n", i, u_errorName(status), rule);
            return;
        }

        delete c;
    }
}

#endif /* #if !UCONFIG_NO_COLLATION */

