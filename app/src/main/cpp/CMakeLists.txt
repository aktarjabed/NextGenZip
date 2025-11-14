cmake_minimum_required(VERSION 3.10.2)
project("llamainfer")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(llamainfer SHARED llamainfer.cpp)

find_library(log-lib log)

target_link_libraries(llamainfer ${log-lib})
