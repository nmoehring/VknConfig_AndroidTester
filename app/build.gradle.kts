plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.nathanielmoehring.vknconfig"
    compileSdk = 35

    defaultConfig {
        // IMPORTANT: Change this to your unique application ID
        applicationId = "com.example.vknconfigtester"
        minSdk = 29 // Vulkan 1.1 core features are available from API 29.
        // Android Baseline Profile 2022 guarantees Vulkan 1.1.
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                // Arguments to pass to CMake.
                arguments.add("-DANDROID_STL=c++_shared") // Use shared C++ runtime

                // For vcpkg, if you were using it for other dependencies:
                // val vcpkgRoot = System.getenv("VCPKG_ROOT") ?: project.findProperty("vcpkg.root") as String?
                // if (vcpkgRoot != null) {
                //     arguments.add("-DCMAKE_TOOLCHAIN_FILE=$vcpkgRoot/scripts/buildsystems/vcpkg.cmake")
                //     // Example for a specific ABI, make this dynamic if needed
                //     // arguments.add("-DVCPKG_TARGET_TRIPLET=arm64-android")
                // } else {
                //     logger.warn("VCPKG_ROOT or vcpkg.root property not found. Vcpkg integration might be incomplete.")
                // }
            }
        }
        ndk {
            // Specify ABIs to build for. arm64-v8a for modern devices, x86_64 for emulators.
            abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false // Set to true for production releases if desired
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            externalNativeBuild {
                cmake {
                    // CMAKE_BUILD_TYPE is often set by CMake itself based on the Android build type
                    // (e.g., when building 'release' variant, CMake is usually configured for Release).
                    // You can explicitly pass it if needed:
                    // arguments.add("-DCMAKE_BUILD_TYPE=Release")
                }
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false // Typically false for debug builds
            isDebuggable = true
            externalNativeBuild {
                cmake {
                    // arguments.add("-DCMAKE_BUILD_TYPE=Debug")
                }
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            //version = "3.30"
        }
    }


    buildFeatures {
        viewBinding = true // Or false, depending on your preference for UI interaction
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.13" // Use a version compatible with your Kotlin and AGP version
    }
    ndkVersion = "27.0.12077973"
    // If your NDK path is not automatically found, or you want to specify a particular version:
    // ndkVersion = "26.1.10909125" // Example NDK version from SDK Manager
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.material3.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Jetpack Compose dependencies
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity.compose) // For ComponentActivity.setContent

}
