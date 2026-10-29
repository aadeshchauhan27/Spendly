# Spendly 💳📊

A modern, privacy-first personal finance and expense tracking Android application built with **Jetpack Compose** and **Kotlin**. Spendly automatically syncs and parses financial SMS messages directly on your device, delivering real-time spending insights, bank attribution graphs, category breakdowns, and smart spam filtering without any data leaving your phone.

![Spendly Showcase](Spendly%20Finance%20App%20UI%20Showcase.png)

---

## ✨ Features

### 📩 Real-Time SMS Sync & Intelligent Parsing
- **Local SMS Sync**: Reads and parses transactional SMS messages with full user consent.
- **Robust Pattern Engine**: Accurately extracts debit/credit amounts, account numbers, merchant names, reference IDs, and timestamps across major Indian banks (**HDFC, SBI, ICICI, Axis, Kotak, Yes Bank, IDFC First, PNB, IndusInd**) and payment apps (**Google Pay, PhonePe, Paytm, CRED, Kiwi, BHIM**).
- **Available Balance & Limit Protection**: Differentiates between actual transaction amounts and available credit limits or remaining balances to eliminate inflated statistics.

### 🛡️ Smart Spam & Promotional Offer Filter
- Detects pre-approved loan offers, disbursement consent prompts, EMI conversion advertisements, and credit card limit alerts.
- Automatically isolates promotional spam into a dedicated `SPAM` category, ensuring your net spending, income, and cash flow reflect only real, verified transactions.

### 📊 Bank-Wise UPI Outflow & Analytics
- **UPI Spends Breakdown**: Visual bar graph attributing UPI and card spends across each banking institution (HDFC, Axis, Kiwi, ICICI, SBI, etc.).
- **Category Drill-Down**: Interactive category breakdowns with percentage allocations. Tap any category to inspect every underlying transaction.
- **Custom Categories & Unique Icons**: Users can create custom categories assigned with unique icons and color badges.

### 🏠 Overview & Dashboard
- **Cash Flow Tracking**: 7-day, 30-day, and all-time summaries of total income, spend, and net balance.
- **Recent Transactions**: Filterable feed with support for loading additional transaction batches dynamically.

### 🔒 100% Private & Offline
- Zero cloud dependency for transaction processing.
- Everything is stored and processed locally using **Room Database (SQLite)**.

---

## 🛠️ Tech Stack & Architecture

- **UI**: 100% [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 Design
- **Language**: Kotlin 2.0+
- **Architecture**: Clean Architecture with MVVM (Model-View-ViewModel) and unidirectional data flow (StateFlow)
- **Database**: Room Database with SQLite for fast, offline persistence
- **Async & Concurrency**: Kotlin Coroutines & Flow
- **Navigation**: Jetpack Compose Navigation
- **Tooling**: Android Gradle Plugin, KSP (Kotlin Symbol Processing)

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug | 2024.2.1 or newer
- JDK 17
- Android SDK 36 (targetSdk 36, minSdk 26)
- Physical Android device or Emulator (SMS reading requires SMS permission on a device with SMS history)

### Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/aadeshchauhan27/Spendly.git
   cd Spendly
   ```

2. **Open in Android Studio:**
   - Launch Android Studio.
   - Select **Open** and choose the `Spendly` project directory.
   - Wait for Gradle sync to complete.

3. **Build & Run:**
   - Connect your Android device via USB (with USB Debugging enabled).
   - Press **Run ▶** in Android Studio or execute via Gradle:
     ```bash
     ./gradlew assembleDebug
     adb install -r app/build/outputs/apk/debug/app-debug.apk
     ```

---

## 📂 Project Structure

```
app/src/main/java/com/spendly/app/
├── core/
│   ├── components/         # Reusable Compose UI components (cards, badges, buttons)
│   └── theme/              # Color palette, Typography, Shapes
├── data/
│   ├── local/              # Room Database, Type Converters, and DAOs
│   └── repository/         # Repository implementations
├── domain/
│   ├── engine/             # Financial calculation & SMS parsing engines
│   ├── model/              # Domain entities (Transaction, Category, BankStats)
│   └── repository/         # Repository contracts
├── sms/                    # Android SMS content provider reader & sync manager
└── ui/
    ├── analytics/          # Analytics & Category breakdown screens
    ├── home/               # Dashboard, balance cards, recent transactions
    ├── navigation/         # Compose navigation graph & routes
    ├── transactions/       # Detailed transactions list & filters
    └── upi/                # UPI spends bar chart & bank breakdown
```

---

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.
