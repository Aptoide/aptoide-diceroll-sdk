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
        }
        release {
            buildConfigField(
                "String",
                "APPSFLYER_API_KEY",
                project.property("APPSFLYER_API_KEY").toString()
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
}
