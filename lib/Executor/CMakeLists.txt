cmake_minimum_required (VERSION 3.0)

if (${WIN32})
  message ("Build on Windows not (yet) supported.")
endif (${WIN32})

# set compiler and default options
if (${UNIX})
  # FIXME: not sure when required: -stdlib=libc++
  if (${APPLE})
    set (CMAKE_CXX_COMPILER "clang++" CACHE STRING "CXX Compiler.")
    set (CMAKE_CXX_FLAGS "-std=c++11 -DGTEST_USE_OWN_TR1_TUPLE=1 -stdlib=libc++ -Wall -Wextra"
      CACHE STRING "Flags used by the compiler during all build types.")
  else (${APPLE})
    set (CMAKE_CXX_COMPILER "clang++" CACHE STRING "CXX Compiler.")
    set (CMAKE_CXX_FLAGS "-std=c++11 -stdlib=libc++ -Wall -Wextra -pthread"
      CACHE STRING "Flags used by the compiler during all build types.")
  endif (${APPLE})
endif (${UNIX})

project (Executor)

option (BUILD_SHARED_LIBS "Build shared libraries (DLLs)." ON)

if (NOT CMAKE_BUILD_TYPE)
  set (CMAKE_BUILD_TYPE Release)
endif (NOT CMAKE_BUILD_TYPE)

# see:
# https://github.com/KhronosGroup/OpenCL-Headers
# https://github.com/KhronosGroup/OpenCL-ICD-Loader
find_package (OpenCL REQUIRED)
find_package (JNI)

include_directories (
  "${PROJECT_SOURCE_DIR}/include/executor"
  "${PROJECT_SOURCE_DIR}/include"
  "${JNI_INCLUDE_DIRS}")

set (SOURCES
  src/Core.cpp
  src/Device.cpp
  src/DeviceBuffer.cpp
  src/DeviceID.cpp
  src/DeviceList.cpp
  src/DeviceProperties.cpp
  src/Event.cpp
  src/Executor.cpp
  src/GlobalArg.cpp
  src/JNIHandle.cpp
  src/Kernel.cpp
  src/KernelArg.cpp
  src/LocalArg.cpp
  src/PlatformID.cpp
  src/Source.cpp
  src/ValueArg.cpp

  src/util/Assert.cpp
  src/util/Logger.cpp

  src/jni/Handle.cpp
  src/jni/opencl_executor_Executor.cpp
  src/jni/opencl_executor_GlobalArg.cpp
  src/jni/opencl_executor_JNIHandle.cpp
  src/jni/opencl_executor_Kernel.cpp
  src/jni/opencl_executor_LocalArg.cpp
  src/jni/opencl_executor_ValueArg.cpp
  )


add_library (executor-jni ${SOURCES})
target_link_libraries (executor-jni OpenCL::OpenCL ${JNI_LIBRARIES} pthread)


