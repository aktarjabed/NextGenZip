cmake_minimum_required(VERSION 3.10.2)
project("llamainfer")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_subdirectory(llama_src)  # Add llama.cpp source tree here

add_library(llamainfer SHARED llamainfer.cpp)
add_library(llama_native SHARED llama_native.cpp)

find_library(log-lib log)

target_link_libraries(llamainfer ${log-lib})
target_link_libraries(llama_native llama ${log-lib})  # assuming llama defined in llama_src

# Include directories for llama
target_include_directories(llama_native PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/llama_src/include)
