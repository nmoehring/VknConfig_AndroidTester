# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.
cmake_minimum_required(VERSION 3.30)

project("VknConfig" LANGUAGES CXX)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
#
# In this top level CMakeLists.txt, ${CMAKE_PROJECT_NAME} is used to define
# the target library name; in the sub-module's CMakeLists.txt, ${PROJECT_NAME}
# is preferred for the same purpose.
#
# In order to load a library into your app from Java/Kotlin, you must call
# System.loadLibrary() and pass the name of the library defined here;
# for GameActivity/NativeActivity derived applications, the same library name must be
# used in the AndroidManifest.xml file.


#add_library(${CMAKE_PROJECT_NAME} SHARED
#        # List C/C++ source files with relative paths to this CMakeLists.txt.
#        native-lib.cpp)



# --- VknConfig Library ---
# Add your VknConfig library as a subdirectory.
# This assumes VknConfig/CMakeLists.txt defines a library target (e.g., VknConfigLib).
add_subdirectory(VknConfig_SubModule) # Path relative to this CMakeLists.txt

target_sources(VknConfig PRIVATE ${CMAKE_CURRENT_LIST_DIR}/native-lib.cpp

# --- Android System Libraries ---
# For __android_log_print (logging)
find_library(log-lib log android-lib android android-ndk-platform-lib)
target_link_libraries(VknConfig PRIVATE ${log-lib} ${android-lib} ${android-ndk-platform-lib} ${android} ${log})