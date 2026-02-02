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
                "ADJUST_APP_TOKEN",
                project.property("ADJUST_APP_TOKEN").toString()
            )
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
            buildConfigField(
                "String",
                "ADJUST_APP_TOKEN",
                project.property("ADJUST_APP_TOKEN").toString()
            )
        }
        release {
            buildConfigField(
                "String",
                "ADJUST_APP_TOKEN",
                project.property("ADJUST_APP_TOKEN").toString()
            )
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
            buildConfigField(
                "String",
                "ADJUST_APP_TOKEN",
                project.property("ADJUST_APP_TOKEN").toString()
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
    implementation(libs.adjust)
    implementation(libs.appsflyer.sdk)
    implementation(libs.kochava.tracker)
    implementation(libs.kochava.events)
}
