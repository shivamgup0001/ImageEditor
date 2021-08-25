cmake_minimum_required(VERSION 3.6)

add_library(imageEditor SHARED imageEditor.c)

target_link_libraries(imageEditor android)