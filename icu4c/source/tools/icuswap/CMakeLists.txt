add_executable(icuswap icuswap.cpp)
target_link_libraries(icuswap PRIVATE icu_defaults icuuc toolutil)