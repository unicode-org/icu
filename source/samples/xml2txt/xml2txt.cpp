/******************************************************************************
 * Copyright (C) 2002, International Business Machines Corporation and
 * others. All Rights Reserved.
 ******************************************************************************/
#include "xml2txt.h"

static bool DTDFLAG = false;
static char*                    gTxtFile;
static char*                    gXmlFile;   
static const char               *sourceDir;
static const char               *destDir;
static bool                     gDoNamespaces          = false;
static bool                     gDoSchema              = false;
static bool                     gDoCreate              = false;
static XMLCh*                   gEncodingName          = 0;
static XMLFormatter::UnRepFlags gUnRepFlags            = XMLFormatter::UnRep_CharRef;
static DOMParser::ValSchemes    gValScheme             = DOMParser::Val_Auto;
static XMLFormatter*            gFormatter             = 0;



enum
{
   HELP,
   SOURCEDIR,
   DESTDIR,
};
//#define UOPTION_TXT          UOPTION_DEF("txt", 't', UOPT_NO_ARG)
//#define UOPTION_RES          UOPTION_DEF("res", 'r', UOPT_NO_ARG)

UOption options[]={
                      UOPTION_HELP_H,
                      UOPTION_SOURCEDIR,
                      UOPTION_DESTDIR,
                  };



#ifdef XP_MAC_CONSOLE
#include <console.h>
#endif


// ---------------------------------------------------------------------------
//
//  Usage()
//
// ---------------------------------------------------------------------------
void usage() 
{
    cout << "\nUsage: XML2TXT [OPTIONS] [FILES]\n\n"
            "This program is used to convert XML files to TXT files.\n"
            "Please refer to the following options. Options are not \n"
            "case sensitive.\n"
            "Options:\n"
            "\t-s or --sourcedir   \t source directory for files followed by path, default is current directory.\n"
            "\t-d or --destdir	   \t destination directory, followed by the path, default is current directory.\n"
            "\t-h or -? or --help  \t this usage text.\n"
            "\nAttention: \n"
            "\tThe text file's encoding is the same as the source file's.\n"
            
          <<  endl;
}

int main(int argC, char* argV[])
{
    int retval = 0;
    const char* arg=NULL;

    try
    {
        XMLPlatformUtils::Initialize();
    }

    catch(const XMLException& toCatch)
    {
        cerr << "Error during Xerces-c Initialization.\n"
             << "  Exception message:"
             << DOMString(toCatch.getMessage()) << endl;
        return 1;
    }

    #ifdef XP_MAC_CONSOLE

    argC = ccommand((char***)&argV);
    #endif

    argC = u_parseArgs(argC, argV, (int32_t)(sizeof(options)/sizeof(options[0])), options);

    if(argC<0) {
        cout << "error in command line argument" << argV[-argC] << endl;    
    } 

    // Watch for special case help request
    if(argC<2 || options[HELP].doesOccur) {
        usage();
        return argC < 0 ? U_ILLEGAL_ARGUMENT_ERROR : U_ZERO_ERROR;
    }

    if(options[SOURCEDIR].doesOccur) {
        sourceDir = options[SOURCEDIR].value;
    }
    else {
        #ifdef WIN32
            destDir = _getcwd(NULL, 0);
        #else
            destDir = getcwd(NULL, 0);
        #endif
    }

    if(options[DESTDIR].doesOccur) {
        destDir = options[DESTDIR].value;
    }
    else {
        #ifdef WIN32
            destDir = _getcwd(NULL, 0);
        #else
            destDir = getcwd(NULL, 0);
        #endif
    }

    for(int i = 1; i< argC; i++) {
        arg = getLongPathname(argV[i]);
    
        gXmlFile = CreateFile(arg, sourceDir);
    
        gTxtFile = CreateTxtName(arg, destDir); 
        
        
        retval = ProcessTxtFile();
    }
    XMLPlatformUtils::Terminate();
    return retval;
}



int ProcessTxtFile()
{
    int retval = 0;

    DOMParser*  parser;
    DOMTreeErrorReporter* errReporter;  
    parser = new DOMParser();
    errReporter = new DOMTreeErrorReporter();
    parser->setValidationScheme(gValScheme);
    parser->setDoNamespaces(true);
    parser->setDoSchema(gDoSchema);
    
    parser->setErrorHandler(errReporter);
    parser->setCreateEntityReferenceNodes(gDoCreate);
    parser->setToCreateXMLDeclTypeNode(true);

    //
    //  Parse the XML file, catching any XML exceptions that might propogate
    //  out of it.
    //
    bool errorsOccured = false;
    try
    {
        parser->parse(gXmlFile);
        int errorCount = parser->getErrorCount();
        if (errorCount > 0)
            errorsOccured = true;
    }

    catch (const XMLException& e)
    {
        
        
        cerr << "An error occured during parsing\n   Message: "
             << DOMString(e.getMessage()) << endl;
        errorsOccured = true;
    }


    catch (const DOM_DOMException& e)
    {
       cerr << "A DOM error occured during parsing\n   DOMException code: "
             << e.code << endl;
        errorsOccured = true;
    }

    catch (...)
    {
        cerr << "An error occured during parsing\n " << endl;
        errorsOccured = true;
    }

    if(!errorsOccured && !errReporter->getSawErrors())
    {
        DOM_Node document = parser->getDocument();
        Check(document); //if check fails, exit(0); else excute the following code
        if(DTDFLAG == false){
            cout << "DTD no assigned!" << endl;
            exit(0);
        }
    }

    // If the parse and doubt-check was successful, output the document data from the DOM tree
    if (!errorsOccured && !errReporter->getSawErrors())
    {
        DOM_Node doc = parser->getDocument();
        DOMPrintFormatTarget  *formatTarget = new DOMPrintFormatTarget(gTxtFile);
        
    

        if (gEncodingName == 0)
        {
            DOMString encNameStr("UTF-8");
            DOM_Node aNode = doc.getFirstChild();
            if (aNode.getNodeType() == DOM_Node::XML_DECL_NODE)
            {
                DOMString aStr = ((DOM_XMLDecl &)aNode).getEncoding();
                if (aStr != "")
                {
                    encNameStr = aStr;
                }
            }
            unsigned int lent = encNameStr.length();
            gEncodingName = new XMLCh[lent + 1];
            XMLString::copyNString(gEncodingName, encNameStr.rawBuffer(), lent);
            gEncodingName[lent] = 0;
        }


        try
        {
            gFormatter = new XMLFormatter(gEncodingName, formatTarget,
                                          XMLFormatter::NoEscapes, gUnRepFlags);
            ofstream ofile(gTxtFile, ios::trunc);
            cout << doc;
        }
        catch (XMLException& e)
        {
            cerr << "An error occurred during creation of output transcoder. Msg is:"
                 << endl
                 << DOMString(e.getMessage()) << endl;
            retval = 3;
        }
    delete formatTarget;
    delete gFormatter;
    }   
    delete errReporter;
    delete parser;
    parser = NULL;
    errReporter = NULL;
    delete gEncodingName;
    gEncodingName=NULL;
    return retval;
}

    

    


//----------------------------------------------------------------------------
//  double-check before DOM Tree PrintOut
//----------------------------------------------------------------------------
void Check( DOM_Node &document)
{   
    // Get the name and value out for convenience
    DOMString   nodeName = document.getNodeName(); //<tag name>, type
    DOMString   nodeValue = document.getNodeValue(); //<tag content>
    
    DOMString attributeKey, attributeVal; //(key/name)(val/filename)
    unsigned long lent = nodeValue.length();
    switch (document.getNodeType())
    {
        case DOM_Node::TEXT_NODE:
        {
            break;
        }

        case DOM_Node::PROCESSING_INSTRUCTION_NODE :
        {
          break;
        }


        case DOM_Node::DOCUMENT_NODE :
        {

            DOM_Node child = document.getFirstChild();
            while( child != 0)
            {
                Check(child);
                child = child.getNextSibling();
            }
            break;
        }

        case DOM_Node::ELEMENT_NODE :
        {
            DOM_NamedNodeMap attributes = document.getAttributes();
            int attrCount = attributes.getLength();
            int item_num=0;
            for (int i = 0; i < attrCount; i++)
            {
                DOM_Node  attribute = attributes.item(i);
                
                if(attribute.getNodeName().equals("key")||attribute.getNodeName().equals("name")){
                        attributeKey = attribute.getNodeValue();
                }
                else if(attribute.getNodeName().equals("val")||attribute.getNodeName().equals("filename")){
                    attributeVal = attribute.getNodeValue();
                    item_num = i;
                }
                else{
                    //call error report         
                    ErrorReport(document, 0);
                }
            }
            
            if(document.getParentNode().getNodeName().equals("array") && attributeKey!=NULL){
                    ErrorReport(document, 1); //ErrorType =1--the element in the array has name
                }
            else if(document.getParentNode().getNodeName().equals("table") && attributeKey==NULL){
                    ErrorReport(document, 2); //element in a table has no name
            }

            if(document.getNodeName().equals("table"))
            {
                //unsigned int Child_Num;
                if(document.hasChildNodes())
                {   
                    ChildName* cn = new ChildName();
                    cn->SetNext(NULL);
                    ChildName* head = CheckNameDuplicate(document, cn);
                    DelChildName(head);
                }
            }
            else if(document.getNodeName().equals("array")) {}
            else if(document.getNodeName().equals("resourceBundle")) {}

            else if(document.getNodeName().equals("str")||document.getNodeName().equals("importBin"))
            {
                CheckEscape(attributes, attributeVal, item_num);
            }

            else if(document.getNodeName().equals("intVector"))
            {
                DOMString ivstring;
                ivstring = CheckIntvector(attributeVal, document);
                if(ivstring !=NULL)
                    attributes.item(item_num).setNodeValue(ivstring);
            }

            else if(document.getNodeName().equals("int"))
            {
                CheckInt(attributeVal, document);
            }

            else if(document.getNodeName().equals("bin"))
            {
                CheckBin(attributeVal, document);
            }

            else if(document.getNodeName().equals("import")) {}
            else if(document.getNodeName().equals("alias")) {}
            else {
                ErrorReport(document, 6);
            }


            DOM_Node child = document.getFirstChild();
            if (child != 0)
            {
                while( child != 0)
                {
                    Check(child);
                    child = child.getNextSibling();
                }
            }
            break;
        }


        case DOM_Node::ENTITY_REFERENCE_NODE:
            {
                break;
            }


        case DOM_Node::CDATA_SECTION_NODE:
            {
            break;
        }


        case DOM_Node::COMMENT_NODE:
        {
            break;
        }


        case DOM_Node::DOCUMENT_TYPE_NODE:
        {
            DTDFLAG = true;
            break;
        }


        case DOM_Node::ENTITY_NODE:
        {
            break;
        }


        case DOM_Node::XML_DECL_NODE:
        {
            break;
        }


        default:
            cerr << "Unrecognized node type = "
                 << (long)document.getNodeType() << endl;
    }
}

void CheckEscape(DOM_NamedNodeMap attributes, DOMString attributeVal, int item_num)
{
    unsigned int len;
    char Escape[7] = {'\\', 'u', '0', '0', '2', '2', '\0'};
    len = attributeVal.length();
    DOMString fromStr;
    DOMString toStr;
    const XMLCh quote[] = {(unsigned short)0x22, (unsigned short) 0};
    if(len>0)
    {
        for(unsigned int i=0; i<len; i++)
        {
            fromStr = attributeVal.substringData (i,1);
            char* temp=fromStr.transcode();
            if(fromStr.equals(quote))
            {
                toStr.appendData(Escape);
            }
            else
                toStr.appendData(fromStr);
        }
        attributes.item(item_num).setNodeValue(toStr);
    }
}

DOMString getAttributeKey(DOM_Node CNode)
{
        DOM_NamedNodeMap attributes = CNode.getAttributes();
        int attrCount = attributes.getLength();
        DOMString attributeKey;

        for (int i = 0; i < attrCount; i++)
        {
            DOM_Node  attribute = attributes.item(i);
                
            if(attribute.getNodeName().equals("key"))
                attributeKey = attribute.getNodeValue();
        }
        return attributeKey;
}

void DelChildName(ChildName* cn)
{
    ChildName* temp = cn->Next;
    while(temp!=NULL)
    {
        delete cn;
        cn = NULL;
        cn = temp;
        temp = temp->Next;
    }
    delete cn;
}

ChildName* CheckNameDuplicate(DOM_Node document, ChildName* cn)
{
    DOM_Node CNode = document.getFirstChild();

    while(CNode!=NULL)
    {
        if(CNode.getNodeName().equals("string")||CNode.getNodeName().equals("bin")||CNode.getNodeName().equals("int")||CNode.getNodeName().equals("intvector")||CNode.getNodeName().equals("import")||CNode.getNodeName().equals("table")||CNode.getNodeName().equals("array"))
        {
            DOMString cname = getAttributeKey(CNode);
            char* string = cname.transcode();
            ChildName* temp = cn;
            while(temp->Next!=NULL)
            {
                if(cname.equals(temp->Name))
                {
                    DelChildName(cn);
                    ErrorReport(CNode, 5);   //name duplication
                }
                temp = temp ->Next; 
            }

            ChildName* childname = new ChildName();
            childname->SetName(cname);
            childname->SetNext(cn);
            cn = childname;
        }
        CNode = CNode.getNextSibling(); 
    }
    return cn;
}

unsigned int GetCNodeNum(DOM_Node document)
{
    unsigned int num=0;
    DOM_Node CNode = document.getFirstChild();
    while(CNode!=NULL)
    {
        if(CNode.getNodeName().equals("string")||CNode.getNodeName().equals("bin")||CNode.getNodeName().equals("int")||CNode.getNodeName().equals("intvector")||CNode.getNodeName().equals("import")||CNode.getNodeName().equals("table")||CNode.getNodeName().equals("array"))
            num++;
        CNode = CNode.getNextSibling(); 
    }
    return num;
}

void CheckBin(DOMString attributeVal, DOM_Node document)
{
    char *stopstring;
    char toConv[2] = {'\0', '\0'};
    char* string = attributeVal.transcode();
    int count = strlen(string);
    if(count > 0)
    {
        if((count % 2)==0)
        {
            for(int i=0; i<count; i++)
            {
                toConv[0]=string[i];
                int value = strtoul(toConv, &stopstring, 16);
                unsigned int len = stopstring-toConv;
                if(len!= strlen(toConv))
                {
                    ErrorReport(document, 4);  //invalid bin value
                }
            }
        }
        else
            ErrorReport(document, 4); //invalid bin value
    }
}


void CheckInt(DOMString attributeVal, DOM_Node document)
{
    char  *stopstring;
    char* string= attributeVal.transcode();
    long value = strtoul(string, &stopstring, 0);
    unsigned int len=stopstring-string;
    if(len!=strlen(string))
        ErrorReport(document, 3);  //invalid int value
}

DOMString CheckIntvector(DOMString attributeVal, DOM_Node document)
{
                DOMString ivstring;
                char* string ;
                if(attributeVal != NULL)
                {
                    string = attributeVal.transcode();
                    char integer[32];
                    char *stopstring;
                    int i,j;
                    int len = strlen(string);
                    int begin,end;
                    int value;
                    begin = end =0;
                    for(i = 0; i < len; i++)
                    {                   
                        if(string[i]==(char)32 && i!= (len-1)){
                            end = i+1;
                            for(j = begin; j < end; j++)
                                integer[j-begin] = string[j];
                            
                        
                            integer[end-begin]='\0';
                            ivstring.appendData(integer);
                            ivstring.appendData(",");

                            value = strtoul(integer, &stopstring, 0);
                            int l = stopstring - integer;
                            if((stopstring - integer)!=(end - begin -1))
                                ErrorReport(document, 3); //invalid int value
                            begin = end;
                        }
                    }
                    if(string[len-1]!=(char)32)
                    {
                        for(j = begin; j < len; j++)
                            integer[j-begin] = string[j];
                        integer[len-begin] = '\0';
                        ivstring.appendData(integer);

                        value = strtoul(integer, &stopstring, 0);
                        int l = stopstring - integer;
                        if((stopstring - integer)!=(len - begin))
                            ErrorReport(document, 3); 
                    }
                return ivstring;
                }
                else
                    return NULL;

}

// ---------------------------------------------------------------------------
//  ostream << DOM_Node
//
//  Stream out a DOM node, and, recursively, all of its children. 
// ---------------------------------------------------------------------------

ostream& operator<<(ostream& target, DOM_Node& toWrite)
{
    // Get the name and value out for convenience
    DOMString   nodeName = toWrite.getNodeName(); //<tag name>, type
    DOMString   nodeValue = toWrite.getNodeValue(); //<tag content>

    DOMString attributeKey, attributeVal; //(key/name)(val/filename)
    unsigned long lent = nodeValue.length();
    

    switch (toWrite.getNodeType())
    {
        case DOM_Node::TEXT_NODE:
        {
            gFormatter->formatBuf(nodeValue.rawBuffer(),
                                  lent, XMLFormatter::CharEscapes);
            break;
        }


        case DOM_Node::PROCESSING_INSTRUCTION_NODE :
        {
            break;
        }


        case DOM_Node::DOCUMENT_NODE :
        {

            DOM_Node child = toWrite.getFirstChild();
            while( child != 0)
            {
                target << child;
                child = child.getNextSibling();
            }
            break;
        }


        case DOM_Node::ELEMENT_NODE :
        {
            
            DOM_NamedNodeMap attributes = toWrite.getAttributes();
            int attrCount = attributes.getLength();
            for (int i = 0; i < attrCount; i++)
            {
                DOM_Node  attribute = attributes.item(i);
                
                if(attribute.getNodeName().equals("key")||attribute.getNodeName().equals("name")){
                    attributeKey = attribute.getNodeValue();
                }
                else if(attribute.getNodeName().equals("val")||attribute.getNodeName().equals("filename")){
                    attributeVal = attribute.getNodeValue();
                }
            }
            
            //Print Out
            if(nodeName.equals("resourceBundle"))
                *gFormatter << attributeKey;
            else 
            {
                if(nodeName.equals("bin") && attributeVal==NULL)
                    *gFormatter <<attributeKey << ":" <<  nodeName << chSpace<< "{" << chDoubleQuote <<attributeVal << chDoubleQuote; 
                else if(nodeName.equals("str"))
                    *gFormatter <<attributeKey << chSpace<< "{" << chDoubleQuote <<attributeVal << chDoubleQuote; 
                else if(nodeName.equals("intVector"))
                    *gFormatter <<attributeKey << ":" <<  "intvector" << chSpace<< "{" <<attributeVal ; 
                else if(nodeName.equals("importBin"))
                    *gFormatter <<attributeKey << ":" <<  "import" << chSpace<< "{" << chDoubleQuote <<attributeVal << chDoubleQuote ; 
                else
                    *gFormatter <<attributeKey << ":" <<  nodeName << chSpace<< "{" << attributeVal; 
            }
                
            
            attributeKey = attributeVal = NULL;
            
         
            DOM_Node child = toWrite.getFirstChild();
            if (child != 0)
            {
                while( child != 0)
                {
                    target << child;
                    child = child.getNextSibling();
                }
                if(!nodeName.equals("resourceBundle"))
                    *gFormatter << "}";
            }
            else
            {
                if(!nodeName.equals("resourceBundle"))
                    *gFormatter << "}";
            }
            break;
        }


        case DOM_Node::ENTITY_REFERENCE_NODE:
        {
                break;
        }


        case DOM_Node::CDATA_SECTION_NODE:
        {
                break;
        }


        case DOM_Node::COMMENT_NODE:
        {
            break;
        }


        case DOM_Node::DOCUMENT_TYPE_NODE:
        {
            DOM_DocumentType doctype = (DOM_DocumentType &)toWrite;
            break;
        }


        case DOM_Node::ENTITY_NODE:
        {
            break;
        }


        case DOM_Node::XML_DECL_NODE:
        {
            break;
        }


        default:
            cerr << "Unrecognized node type = " << (long)toWrite.getNodeType() << endl;
    }
    return target;
}

void ErrorReport(DOM_Node& toWrite, int ErrorType){

    DOM_NamedNodeMap attributes;
    DOM_Node attribute;
    int attrCount, i;

    cout << "\nerror occurs at:\n";
    DOMString ErrorMsg;
    while(toWrite.getParentNode()!=NULL){
    //do
    ErrorMsg.insertData(0, ")");

    attributes = toWrite.getAttributes();
    attrCount = attributes.getLength();
    
    if(attrCount!=0)
    {
        for (i = attrCount-1; i>=0; i--)
        {
            attribute = attributes.item(i);
            ErrorMsg.insertData(0, " ; ");  
            ErrorMsg.insertData(0, attribute.getNodeValue());       
        }
    }
    ErrorMsg.insertData(0, "(");
    ErrorMsg.insertData(0, toWrite.getNodeName());
    ErrorMsg.insertData(0, "==>");
    toWrite = toWrite.getParentNode();
    }
    ErrorMsg.appendData("\n");
    
    switch (ErrorType)
    {
    case 1:
        ErrorMsg.appendData("The element in the array can't have a name!\n");
        break;
    case 2:
        ErrorMsg.appendData("The element in the table should have a name!\n");
        break;
    case 3:
        ErrorMsg.appendData("Invalid integer value!\n");
        break;
    case 4:
        ErrorMsg.appendData("Invalid bin!\n");
        break;
    case 5:
        ErrorMsg.appendData("Name Duplication in the table!\n");
        break;
    case 6:
        ErrorMsg.appendData("Invalid element name! Remember to assign correct DTD file on the xml file.\n");
        break;
    }
    cout << ErrorMsg;
    exit(0);
}

char* CreateTxtName(const char* arg, const char* Dir)
{
    char* temp = CreateFile(arg, Dir);
    int len = strlen(temp);
    temp[len-1] = 't';
    temp[len-2] = 'x';
    temp[len-3] = 't';
    return temp;

    /*char drive[_MAX_DRIVE];
    char dir[_MAX_DIR];
    char fname[_MAX_FNAME];
    char ext[_MAX_EXT];
    _splitpath(gXmlFile, drive, dir, fname, ext);
    strcpy(gTxtFile, "\0");
    if (drive != NULL) {
        strcat(gTxtFile, drive);
    }
    if (dir != NULL) {
        strcat(gTxtFile, dir);
    }
    if (fname !=NULL) {
        strcat(gTxtFile, fname);
    }
    strcat(gTxtFile, "tempfile.txt");*/
}

char* CreateFile(const char* arg, const char* Dir)
{   char* temp = new char[256];
    char a[2]={'\\', '\0'};
    char* currdir;
    if(sourceDir!=NULL) {
        strcpy(temp, Dir);
        int len = strlen(temp);
        if(temp[len - 1]!='\\') 
            strcat(temp, a);
        strcat(temp, arg);
    }
    else {
        char drive[_MAX_DRIVE];
        char dir[_MAX_DIR];
        char fname[_MAX_FNAME];
        char ext[_MAX_EXT];
        _splitpath(arg, drive, dir, fname, ext);
        
        if(*drive == NULL && *dir == NULL) {
            #ifdef WIN32
            currdir = _getcwd(NULL, 0);
            #else
            currdir = getcwd(NULL, 0);
            #endif
            strcpy(temp, currdir);
            strcat(temp, a);
        }
        strcat(temp, arg);
    }
    return temp;
}


// ---------------------------------------------------------------------------
//  ostream << DOMString
//
//  Stream out a DOM string. Doing this requires that we first transcode
//  to char * form in the default code page for the system
// ---------------------------------------------------------------------------

ostream& operator<< (ostream& target, const DOMString& s)
{
    char *p = s.transcode();
    target << p;
    delete [] p;
    return target;
}


XMLFormatter& operator<< (XMLFormatter& strm, const DOMString& s)
{
    unsigned int lent = s.length();

    if (lent <= 0)
        return strm;

    XMLCh*  buf = new XMLCh[lent + 1];
    XMLString::copyNString(buf, s.rawBuffer(), lent);
    buf[lent] = 0;
    strm << buf;
    delete [] buf;
    return strm;
}


