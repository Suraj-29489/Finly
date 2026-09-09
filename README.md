# Finly (formerly Personal Expense Tracker)

A clean, modern, and local-first personal finance and expense tracking Android application.

---

## Key Highlights

- **Local-First & Private:** Complete financial privacy with on-device SQLite storage.
- **Streamlined Logging:** Quick and frictionless expense recording.
- **Insightful Dashboard:** Real-time visibility into today's spending, monthly totals, and recent activity.
- **Budgeting & Categories:** Manage budgets with alerts and customizable categories.
- **Modular Evolution:** Phased development path from core persistence up to intelligent insights and natural language processing.

---

## Project Structure

```text
personal-expense-tracker/
├── PROJECT_CONTEXT.md
├── DEVELOPMENT_PLAN.md
├── CHANGELOG.md
├── README.md
├── phases/
│   ├── PHASE_00_PROJECT_FOUNDATION.md
│   ├── PHASE_01_LOCAL_DATABASE.md
│   ├── PHASE_02_EXPENSE_MANAGEMENT.md
│   ├── ...
│   └── PHASE_18_OPTIONAL_CLOUD_LAYER.md
├── app/
│   ├── src/main/java/com/personalexpensetracker/
│   │   ├── data/
│   │   ├── domain/
│   │   ├── ui/
│   │   └── navigation/
│   └── res/
├── build.gradle.kts
└── settings.gradle.kts
```

---

## Development Roadmap

The project follows a strict sequential phased development methodology. See [DEVELOPMENT_PLAN.md](DEVELOPMENT_PLAN.md) and the [phases/](phases/) directory for phase specifications and status.
