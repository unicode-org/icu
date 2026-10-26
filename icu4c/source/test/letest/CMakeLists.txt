add_executable(letest)
target_link_libraries(letest icu_defaults)
target_sources(letest PRIVATE
    FontObject.cpp
    FontTableCache.cpp
    PortableFontInstance.cpp
    SimpleFontInstance.cpp
    cfonts.cpp
    cletest.c
    cmaps.cpp
    gendata.cpp
    letest.cpp
    letsutil.cpp
    testdata.cpp
    xmlreader.cpp)