add_library(ctestfw)
target_link_libraries(ctestfw PRIVATE icu_defaults icuuc toolutil)
target_include_directories(ctestfw PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
target_compile_definitions(ctestfw PRIVATE T_CTEST_IMPLEMENTATION)
target_sources(ctestfw PRIVATE
    ctest.c
    datamap.cpp
    testdata.cpp
    tstdtmod.cpp
    ucln_ct.c
    uperf.cpp)