// XMLConverter.hpp

class XMLConverter
{
public :
    XMLConverter(FILE* inputFile, const char* encodingType, FILE* outputFile);
    ~XMLConverter();
    int convert();
private:
    FILE* fInputFile;
    FILE* fOutputFile;
    const char* fEncodingType;
}
