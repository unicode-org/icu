add_executable(gendict)
icu_add_sources(gendict)
target_link_libraries(gendict PRIVATE icu_defaults icuuc toolutil)