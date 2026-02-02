plugins {
    alias(libs.plugins.compose.compiler)
    id("diceroll.android.library.compose")
}

android {
    namespace = "com.aptoide.diceroll.sdk.core.analytics"
    buildTypes {
        debug {
            buildConfigField(
                "String",
                "APPSFLYER_API_KEY",
                project.property("APPSFLYER_API_KEY").toString()
            )
            buildConfigField(
                "String",
                "KOCHAVA_APP_GUID",
                project.property("KOCHAVA_APP_GUID").toString()
            )
        }
        release {
            buildConfigField(
                "String",
                "APPSFLYER_API_KEY",
                project.property("APPSFLYER_API_KEY").toString()
            )
            buildConfigField(
                "String",
                "KOCHAVA_APP_GUID",
                project.property("KOCHAVA_APP_GUID").toString()
            )
        }
    }
    buildFeatures {
        buildConfig = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(libs.appsflyer.sdk)
    implementation(libs.kochava.tracker)
    implementation(libs.kochava.events)
}
