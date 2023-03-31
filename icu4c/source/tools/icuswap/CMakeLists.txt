add_executable(icuswap)
icu_add_sources(icuswap)
target_link_libraries(icuswap PRIVATE icu_defaults icuuc toolutil)