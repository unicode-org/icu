add_executable(genbrk genbrk.cpp)
target_link_libraries(genbrk PRIVATE icu_defaults icuuc toolutil)