add_executable(icupkg icupkg.cpp)
target_link_libraries(icupkg PRIVATE icu_defaults icuuc toolutil)