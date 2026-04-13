## Overview

This page provides a specialized **AI Prompt** designed to be used with Large Language Models (such as ChatGPT, Claude, or Gemini).

By providing this prompt along with your existing Google Play Billing integration code, the AI acts as a migration engineer—automatically setting up **Build Flavors** and mirroring your existing architectural patterns using the **Aptoide Android Billing SDK**.

***

## AI Prompt

````markdown
# PROMPT: Adaptive Mirror-Integration of Aptoide Billing SDK

## Goal
Integrate the Aptoide Billing SDK into this Android project by strictly mirroring the existing Google Play Billing implementation. The objective is to have a "plug-and-play" version for Aptoide that follows the current project's coding patterns, naming conventions, and architecture, but uses the correct Aptoide-specific APIs.

## Reference Documentation
Use this for Aptoide-specific syntax: https://docs.connect.aptoide.com/docs/android-billing-sdk

## Step 1: Analysis of Current Implementation
1. Scan the project to identify how **Google Play Billing** is currently implemented.
2. Identify the key components:
   * Where the Google `BillingClient` is initialized.
   * Which wrapper classes (e.g., `BillingManager`, `BillingRepository`) handle the purchase flow, SKU/Product queries, and consumption.
   * How the UI or ViewModels observe purchase updates (e.g., Listeners, LiveData, Flows).
   * How Dependency Injection (if any) provides the Billing service.

## Step 2: Infrastructure Setup (Build Variants)
1. Define a `distribution` flavor dimension in the app-level `build.gradle` (or `build.gradle.kts`).
2. Create two flavors: `googlePlay` and `aptoide`.
3. Isolate dependencies:
   * Move Google Billing dependencies to `googlePlayImplementation`.
   * Add Aptoide Billing to `aptoideImplementation` using the latest version from Maven Central: `implementation("com.aptoide:android-aptoide-billing:1.+")`.
4. In a multi-modular project, add correctly the flavor dimensions to all the `build.gradle` files that use the Billing module.

## Step 3: Aptoide Public Key Configuration (CRITICAL)
1. In the root `gradle.properties` file, add a placeholder variable: `APTOIDE_PUBLIC_KEY="PASTE_YOUR_PUBLIC_KEY_HERE"`
2. In the app-level `build.gradle` `aptoide` flavor block, expose this to the app using `buildConfigField`:
   * Groovy: `buildConfigField "String", "APTOIDE_PUBLIC_KEY", project.property('APTOIDE_PUBLIC_KEY')`
   * Kotlin DSL: `buildConfigField("String", "APTOIDE_PUBLIC_KEY", project.property("APTOIDE_PUBLIC_KEY").toString())`
3. **Developer Note requirement:** In your final response to me, display a prominent warning reminding me to replace the placeholder in `gradle.properties` with the actual key from the Aptoide Console.

## Step 4: Mirror-Image Implementation (Source Sets)
1. **Source Set Separation:**
   * Move the existing Google Billing wrapper logic/classes into `src/googlePlay/java` (or `kotlin`).
   * Create identical class files and package structures in `src/aptoide/java` (or `kotlin`).
2. **Wrapper Class Parity:**
   * Inside `src/aptoide/`, rewrite the billing wrapper logic using the Aptoide SDK. You MUST keep the **exact same wrapper class names, custom method signatures, and custom return types** as the Google version (e.g., if Google has `class MyBillingManager`, Aptoide must have `class MyBillingManager`).
3. **Aptoide SDK Specifics (DO NOT USE GOOGLE IMPORTS IN APTOIDE):**
   * Use Aptoide imports: `com.aptoide.sdk.billing.*` (e.g., `AptoideBillingClient`, `Purchase`, `ProductDetails`, `BillingFlowParams`, `QueryProductDetailsParams`) and `com.aptoide.sdk.billing.listeners.*` for the listeners (e.g., `AptoideBillingClientStateListener`, `ConsumeResponseListener`). Here is a list of all the available classes, annotations and listeners in Aptoide Billing SDK:
      - com.aptoide.sdk.billing.AptoideBillingClient.BillingResponseCode
      - com.aptoide.sdk.billing.AptoideBillingClient.FeatureType
      - com.aptoide.sdk.billing.AptoideBillingClient.ProductType
      - com.aptoide.sdk.billing.UnfetchedProduct.StatusCode
      - com.aptoide.sdk.billing.AccountIdentifiers
      - com.aptoide.sdk.billing.AptoideBillingClient
      - com.aptoide.sdk.billing.BillingFlowParams
      - com.aptoide.sdk.billing.BillingFlowParams.Builder
      - com.aptoide.sdk.billing.BillingFlowParams.ProductDetailsParams
      - com.aptoide.sdk.billing.BillingFlowParams.ProductDetailsParams.Builder
      - com.aptoide.sdk.billing.BillingResult
      - com.aptoide.sdk.billing.ConsumeParams
      - com.aptoide.sdk.billing.ConsumeParams.Builder
      - com.aptoide.sdk.billing.ProductDetails
      - com.aptoide.sdk.billing.ProductDetails.OneTimePurchaseOfferDetails
      - com.aptoide.sdk.billing.ProductDetails.PricingPhase
      - com.aptoide.sdk.billing.ProductDetails.PricingPhases
      - com.aptoide.sdk.billing.ProductDetails.SubscriptionOfferDetails
      - com.aptoide.sdk.billing.ProductDetails.TrialDetails
      - com.aptoide.sdk.billing.Purchase
      - com.aptoide.sdk.billing.QueryProductDetailsParams
      - com.aptoide.sdk.billing.QueryProductDetailsParams.Builder
      - com.aptoide.sdk.billing.QueryProductDetailsParams.Product
      - com.aptoide.sdk.billing.QueryProductDetailsParams.Product.Builder
      - com.aptoide.sdk.billing.QueryProductDetailsResult
      - com.aptoide.sdk.billing.QueryPurchasesParams
      - com.aptoide.sdk.billing.QueryPurchasesParams.Builder
      - com.aptoide.sdk.billing.ReferralDeeplink
      - com.aptoide.sdk.billing.UnfetchedProduct
      - com.aptoide.sdk.billing.listeners.AptoideBillingClientStateListener
      - com.aptoide.sdk.billing.listeners.ConsumeResponseListener
      - com.aptoide.sdk.billing.ProductDetailsResponseListener
      - com.aptoide.sdk.billing.PurchasesResponseListener
      - com.aptoide.sdk.billing.PurchasesUpdatedListener
   * **Initialization:** You MUST replace `BillingClient` with `AptoideBillingClient`. It requires the Public Key in the Builder. Use exactly this pattern:
     ```kotlin
     val billingClient = AptoideBillingClient.newBuilder(context)
         .setListener(purchasesUpdatedListener)
         .setPublicKey(BuildConfig.APTOIDE_PUBLIC_KEY)
         .build()
     ```
   * Use `AptoideBillingClientStateListener` for the `startConnection` method.

## Step 5: Semantic Feature Mapping (SDK Differences)
Identify and bridge the functional gaps between the two SDKs to maintain signature parity:

1. **Threading differences (CRITICAL):** The Aptoide `launchBillingFlow()` method **MUST NOT** be called on the Main/UI Thread (this will cause a crash). Wrap the `launchBillingFlow()` call inside a background thread (e.g., Coroutines `Dispatchers.IO` or an RxJava background scheduler) in the Aptoide implementation.
2. **Acknowledge vs Consume:** Aptoide does not support a separate `acknowledgePurchase` step like Google does for subscriptions/non-consumables. If the Google implementation has an `acknowledge()` method, implement it as a "No-Op" (returning success immediately) in the Aptoide flavor, and ensure all purchases are finalized using Aptoide's `consumeAsync` function instead.
3. **Product Types:** Map Google’s `ProductType.SUBS` and `ProductType.INAPP` to `AptoideBillingClient.ProductType.SUBS` and `AptoideBillingClient.ProductType.INAPP`.
4. **General Logic:** Verify all other functional differences and apply the Aptoide SDK logic correctly while maintaining the original custom wrapper method signatures so the UI doesn't break.

## Step 6: Verification & Cleanliness
* The `src/main` folder should remain completely untouched.
* The UI and ViewModels should not know which SDK is being used; they should simply call the wrapper classes that now exist in both flavor source sets.

---

## PHASE 2: Validation Audit (Mandatory)

After completing the code generation, you must perform a self-audit and report on the following:

1. **Isolation Audit:** Confirm zero `com.aptoide.sdk.billing` imports exist in `src/main` or `src/googlePlay`. Conversely, confirm zero `com.android.billingclient.api` imports exist in `src/aptoide`.
2. **Threading Audit:** Explicitly verify that `AptoideBillingClient.launchBillingFlow()` is executed on a background thread.
3. **Builder Audit:** Explicitly verify that `AptoideBillingClient.newBuilder` uses `.setPublicKey(BuildConfig.APTOIDE_PUBLIC_KEY)`.
4. **Signature Check:** Confirm that the public API of your custom wrapper classes in `src/aptoide` is identical to the `src/googlePlay` version.
5. **Cleanliness Check:** Confirm that `src/main` (UI/ViewModels) remained untouched.

**Final Requirement:** If any discrepancies are found during your audit, fix them before providing the final code and report.
````

***

## How to use this Prompt

While you can copy/paste this prompt into a web-based AI (like ChatGPT or Claude), you will achieve the **highest success rate** by using an **AI-powered IDE or Agent** that has direct context of your repository.

### Recommended Approach: AI Code Editors (Cursor / Android Studio integrated AI)

Using a tool like **Cursor** or the **integrated AI Agent inside of the Android Studio** allows the AI to index your entire codebase. This provides several advantages:

* **Context Awareness:** The AI automatically finds your `BillingClient` initialization and `build.gradle` without you having to find and paste the files manually.
* **Automatic File Creation:** The AI can create the new source sets (`src/aptoide/java`) and move the files for you.
* **Direct PR Generation:** You can ask the AI to apply these changes directly to a new branch, which you can then review as a standard Pull Request.

***

## Post AI integration checks

While AI can significantly accelerate the migration process, billing is a critical system. **You should perform the following manual checks** before committing the generated code:

### 1. Placeholder Verification

The AI will use a placeholder for your **Aptoide Public Key**. You must manually replace it in your `build.gradle` or `BuildConfig` with the actual key found in your **Aptoide account**. Failure to do so will cause the SDK to fail processing the Purchase result with the [BillingResponseCode](/docs/billingresponsecode) `DEVELOPER_ERROR (5)`.

### 2. Dependency Versions

AI models may not always have the most recent version of the Aptoide SDK. Cross-reference the implementation version in your `build.gradle` with our [Official Maven Repository](https://mvnrepository.com/artifact/com.aptoide/android-aptoide-billing) to ensure you are using the latest stable release.

### 3. Consumption Logic Review

Aptoide uses a `consume` call to finalize transactions. Carefully review the AI-generated mapping in **Step 5**, specifically how it handles "Acknowledge" calls. Ensure that non-consumable items and subscriptions are handled according to your app's specific entitlement logic.

### 4. Flavor Synchronization

Verify that all resources (strings, drawables, or layouts) referenced in your billing classes exist in both the `src/googlePlay` and `src/aptoide` source sets or have been safely moved to `src/main`. A missing resource in one flavor will cause build failures only when that specific variant is selected.

### 5. Final Sandbox Test

Never assume the AI-generated logic is 100% accurate. Once the code is integrated, follow the [Sandbox Environment](/docs/testing-quality-assurance) guide to perform a full end-to-end test before release.

***

## Example of usage

Here is an example of how to use this prompt with the Android Studio incorporated AI Agent.

https://github.com/user-attachments/assets/3902cf77-43af-4c23-96d5-164dc921e740
