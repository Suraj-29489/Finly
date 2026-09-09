# Finly - Project Context
(Formerly Personal Expense Tracker App)

## Project Overview
**Finly** (formerly Personal Expense Tracker App) is a personal, local-first mobile expense tracking application designed for Android using modern native technologies (Kotlin, Jetpack Compose, Room, Clean Architecture). The goal is to build a clean, professional, and easy-to-use application for recording expenses, managing personal finances, viewing spending patterns, and eventually adding intelligent features.

## Core Philosophy & Design Principles
- **Local-First & Privacy-Centric:** All personal and financial data resides strictly on the user's device by default. No mandatory cloud accounts or external dependencies are required for core operations.
- **Fast & Intuitive Logging:** Expense recording is streamlined for speed and minimal friction, allowing users to log expenses in seconds.
- **Clear Financial Visibility:** Visual dashboards, category breakdowns, budgets, and trends provide instant clarity into spending behavior.
- **Progressive & Modular Architecture:** Built with Clean Architecture principles (Presentation, Domain, Data layers) and Jetpack Compose to allow modular feature additions across sequential development phases.

## Target Platform & Tech Stack Baseline
- **Platform:** Android (API 26+)
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Local Database:** Room (SQLite)
- **Architecture:** Clean Architecture + MVVM + Unidirectional Data Flow (UDF)
- **State Management & Async:** Kotlin Coroutines & Flow, StateFlow, ViewModel

## Development Methodology
The project follows a strict **Sequential Phased Development Workflow** (Phases 0 through 18). Each phase is implemented, validated, and documented before transitioning to subsequent phases.
