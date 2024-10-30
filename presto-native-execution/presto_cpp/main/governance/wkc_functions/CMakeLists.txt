add_library(presto_magen QaagFunctionsRegistration.cpp)

# xsimd is installed into the system and can be found.
# Previously, it was only bundled by Velox and had
# no problem.
find_package(xsimd)
target_link_libraries(presto_magen magen_cpp xsimd ${GFLAGS_LIBRARIES})
