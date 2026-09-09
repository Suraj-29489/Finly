# Changelog

All notable changes to the Finly (formerly Personal Expense Tracker) project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Established project foundation documentation and development roadmap.
- Initialized core configuration and context documents (`PROJECT_CONTEXT.md`, `DEVELOPMENT_PLAN.md`, `CHANGELOG.md`, `README.md`).
- Created 19 sequential development phase specification files in `phases/` (Phases 00 through 18).
- Implemented Phase 0 project foundation with Kotlin, Jetpack Compose, Material 3, and Gradle Kotlin DSL.
- Built minimal `MainActivity` application shell and verified build and deployment on Pixel 9 emulator.
- Implemented Phase 1 local persistence layer with Room SQLite, KSP, and Clean Architecture data/domain separation.
- Added `Expense` domain model (using `BigDecimal` for money and `Instant` for dates) and `ExpenseEntity` with minor currency unit integer storage (`amountInCents`).
- Implemented `ExpenseDao` with full CRUD, category filtering, date range queries, and total spending aggregation.
- Implemented thread-safe singleton `AppDatabase` and `ExpenseRepositoryImpl`.
- Added automated unit and Robolectric database integration tests with 100% passing results.
- Implemented Phase 8 Income and Balance system:
  - Room `IncomeEntity` with integer cents storage, `IncomeDao`, and `IncomeRepositoryImpl`.
  - Room Database Migration `MIGRATION_3_4` bumping schema to version 4.
  - Domain models `Income`, `IncomeSource`, `FinancialBalance`, `FinancialPeriodSummary`, `CashFlowAnalyticsReport`.
  - Use cases for full income CRUD (`AddIncomeUseCase`, `GetIncomesUseCase`, `GetIncomeByIdUseCase`, `UpdateIncomeUseCase`, `DeleteIncomeUseCase`, `GetTotalIncomeUseCase`).
  - Central financial calculation engine (`GetFinancialBalanceUseCase`, `GetPeriodCashFlowUseCase`, `GetCashFlowReportUseCase`).
  - Add & Edit Income Compose screens with dedicated validation and inflow styling (`+ $`, emerald accents).
  - Income History list with search, filter chips by source, and tap-to-edit navigation.
  - Dashboard integration featuring `TotalBalanceCard`, `CurrentMonthSummaryCard` (with cash flow navigation), and today inflow metrics.
  - Dedicated `CashFlowAnalyticsScreen` with period selection (Today, Week, Month, Last Month, Year, All Time) and 6-month historical monthly trends.
  - Complete integration & regression test suites (`FullFinancialIntegrationTest`, `Phase8CompleteRegressionTest`) covering all Scenarios A–J and Tests 1–18.
- Implemented Phase 9 Data Management (Export, Import, Backup, Restore):
  - Repository and transactional Room data foundation with atomic clear/reset operations.
  - RFC 4180-compliant CSV Export and Import for expenses, incomes, and unified ledger with schema validation and duplicate detection.
  - Structured JSON export and import with format versioning and deserialization safeguards.
  - Local binary backup archive system (`.finlybackup`) with magic header `FINLYBK1` and SHA-256 integrity verification.
  - Password-based encrypted backup and restore using AES-256-GCM and PBKDF2WithHmacSHA256 (65,536 iterations).
  - Material 3 Data Management UI (`DataManagementScreen`) with user confirmations, progress indicators, and comprehensive validation dialogues.
- Implemented Phase 10 Personalization (Themes, Currency & User Preferences):
  - Created domain models `AppCurrency` (USD, EUR, GBP, INR, JPY, CAD, AUD), `AppThemeMode` (System, Light, Dark), `AppDateFormat` (4 common patterns), and `UserPreferencesData`.
  - Implemented persistent preferences contract `UserPreferences` and `UserPreferencesImpl` using Android `SharedPreferences` with reactive Kotlin `StateFlow`/`Flow`.
  - Built universal formatters `CurrencyFormatter` and `DateFormatter`, ensuring display formatting is completely decoupled from database storage and calculations.
  - Provided `LocalUserPreferences` through Compose `CompositionLocalProvider` across all UI hierarchy.
  - Implemented `SettingsViewModel` and full Material 3 `SettingsScreen` supporting interactive Currency picker, Theme mode picker, Date Format picker, default application settings, Data Management entry point, and Reset to Defaults confirmation.
  - Integrated dynamic currency and date formatting across all app screens: Dashboard (`TotalBalanceCard`, `CurrentMonthSummaryCard`), Expense List, Income List, Cash Flow Analytics, Add/Edit Expense, Add/Edit Income, and Add/Edit Recurring Expense.
- Implemented Automatic Expense Capture (Notification-Based Expense Detection & Creation - Steps 1–16):
  - Created Android `FinlyNotificationListenerService` registered in `AndroidManifest.xml` with `BIND_NOTIFICATION_LISTENER_SERVICE` permission and intent filter.
  - Designed clean, sanitized data models (`RawNotificationData`, `ParsedTransaction`, `NotificationProcessingResult`) to ensure zero Android framework leakage into domain/data processing pipelines.
  - Implemented local financial notification detector (`FinancialNotificationDetector`) with keyword scoring, amount indicator detection, and strict OTP/promotional rejection.
  - Implemented transaction direction classifier (`TransactionDirectionClassifier`) ensuring only `DEBIT` transactions become expenses; `CREDIT` (e.g. salary, refunds, cashback) and ambiguous/failed/reversed transactions are safely filtered without errors.
  - Built transaction parser (`TransactionParser`) with support for diverse Indian currency formats (`₹`, `Rs`, `Rs.`, `INR`, comma/decimal separators), merchant/payee extraction, and transaction reference ID extraction.
  - Implemented source-aware parsing (`SourceAwareParser`) identifying Bank, UPI (GPay, PhonePe, Paytm, BHIM, Amazon Pay), Card (CRED), Wallet (MobiKwik, FreeCharge), and SMS notifications.
  - Built merchant-to-category resolver (`MerchantCategoryResolver`) mapping known merchants and keyword hints directly to Finly's existing built-in category system with safe fallback to `Other`.
  - Implemented duplicate transaction detector (`DuplicateTransactionDetector`) with reference ID matching, notification key tracking, and amount/merchant/time proximity tolerance (2-minute window) with auto-eviction.
  - Built end-to-end capture processor (`AutoExpenseCaptureProcessor`) seamlessly creating standard `Expense` records directly through the existing `ExpenseRepository`, immediately reflecting in Dashboard, Analytics, Budgets, and Expense List.
  - Integrated with `UserPreferences` (`autoCaptureExpenses` toggle) and added dedicated "AUTOMATIC EXPENSE CAPTURE" section in `SettingsScreen` with live Android Notification Access permission status indicator and one-tap deep linking to Android Notification Access settings (`ACTION_NOTIFICATION_LISTENER_SETTINGS`).
  - Strict privacy and security architecture: 100% on-device processing, zero cloud transmission, no full notification logging, and resilient non-crashing coroutine lifecycle (`SupervisorJob`).
  - Created comprehensive test suites (`NotificationListenerFoundationTest`, `FinancialNotificationDetectorTest`, `TransactionDirectionClassifierTest`, `TransactionParserTest`, `MerchantCategoryResolverTest`, `DuplicateTransactionDetectorTest`, `AutoExpenseCapturePipelineIntegrationTest`) achieving 100% pass rate across all 403+ unit and integration tests.
  - Verified live deployment and UI interaction on Pixel 9 emulator.


