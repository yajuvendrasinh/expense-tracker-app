# Expense Tracker

Expense Tracker is a premium, automated Android application designed to simplify personal finance management. By leveraging intelligent SMS parsing and a modern, high-performance UI, it helps users track their spending with minimal manual effort.

## 🚀 Key Features

* **Automated SMS Parsing**: Intelligently detects and log expenses from bank and UPI notifications.
  * **Vendor Extraction**: Automatically identifies merchants from transaction messages.
  * **UPI ID Support**: Extracts and stores UPI IDs for precise transaction tracking.
  * **OTP Filtering**: Automatically excludes OTP messages to keep your financial data clean.
* **Magic Wand (SMS Scanner)**: Quick-access tool to scan your SMS inbox and import existing transactions with one tap.
* **Sleek & Compact UI**: Designed with Jetpack Compose for a premium feel.
  * **Dynamic Date Strip**: Fast navigation through your daily expenses.
  * **Smart Numpad**: Optimized for fast amount entry with context-aware "Next" and "Done" actions.
  * **Details & Categories**: Easy organization with pre-defined categories and sub-categories.
* **AI Camera Integration**: Dedicated entry point for future AI-powered receipt and invoice scanning.
* **Cloud Sync**: Powered by Firebase Firestore for real-time synchronization and data persistence.

## 🛠 Technical Stack

* **UI Framework**: [Jetpack Compose](https://developer.android.com/compose) (Material 3)
* **Language**: [Kotlin](https://kotlinlang.org/)
* **Dependency Injection**: [Hilt](https://dagger.dev/hilt/)
* **Processing**: [KSP (Kotlin Symbol Processing)](https://kotlinlang.org/docs/ksp-overview.html)
* **Backend**: [Firebase Firestore](https://firebase.google.com/docs/firestore)
* **Build Tool**: [Gradle](https://gradle.org/) (9.3.1) with [Android Gradle Plugin](https://developer.android.com/studio/releases/gradle-plugin) (8.7.3)

## 🏗 Project Configuration

The project is configured for maximum stability and follows modern Android development best practices:

* **Target SDK**: 35
* **Min SDK**: 30
* **Build Optimization**: Standardized on stable AGP and Kotlin versions to ensure a warning-free and optimized build environment.

## 📦 Getting Started

### Prerequisites

* **Android Studio Ladybug (or newer)**
* **JDK 17 or 21**
* **Firebase Project**: You will need to add your `google-services.json` to the `app/` directory to enable cloud features.

### Installation

1. Clone the repository.
2. Open the project in Android Studio.
3. Sync Gradle dependencies.
4. Run the application on a device or emulator (API 30+ recommended).

## 📄 License

This project is for personal use and internal development.
