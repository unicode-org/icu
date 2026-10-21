add_executable(makeconv)
icu_add_sources(makeconv)
target_link_libraries(makeconv PRIVATE icu_defaults icuuc toolutil)