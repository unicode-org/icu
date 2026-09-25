add_executable(gentest)
icu_add_sources(gentest)
target_link_libraries(gentest PRIVATE icu_defaults icuuc toolutil)