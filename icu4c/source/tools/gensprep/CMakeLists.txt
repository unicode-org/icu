add_executable(gensprep)
icu_add_sources(gensprep)
target_link_libraries(gensprep PRIVATE icu_defaults icuuc toolutil)