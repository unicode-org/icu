add_executable(genbrk)
icu_add_sources(genbrk)
target_link_libraries(genbrk PRIVATE icu_defaults icuuc toolutil)