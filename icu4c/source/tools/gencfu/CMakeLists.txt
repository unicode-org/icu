add_executable(gencfu gencfu.cpp)
target_link_libraries(gencfu PRIVATE icu_defaults icuuc toolutil icui18n)