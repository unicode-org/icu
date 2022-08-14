add_executable(gendict gendict.cpp)
target_link_libraries(gendict PRIVATE icu_defaults icuuc toolutil)