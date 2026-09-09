You are continuing development of the Finly Android app.

==================================================
PROJECT STATUS
==================================================

Phase 0 — Project Foundation: COMPLETE
Phase 1 — Local Database: COMPLETE
Phase 2 — Expense Management: COMPLETE
Phase 3 — Dashboard: IN PROGRESS

Phase 3 Step 1 — Dashboard Foundation: COMPLETE

IMPORTANT:
The entire Phase 3 scope is defined below, but you MUST execute it
ONE STEP AT A TIME.

Do NOT implement all steps in one task.

After completing each step:
1. Run the relevant tests.
2. Build the app.
3. Verify the implementation.
4. Report exactly what was completed.
5. STOP.
6. Wait for my instruction before starting the next step.

Do NOT automatically continue to the next step.

==================================================
PHASE 3 — DASHBOARD
==================================================

OBJECTIVE:

Create the main Finly dashboard showing spending summaries,
current-month spending, today's spending, and recent transactions.

EXPECTED RESULT:

Users should be able to open Finly and immediately understand
their current spending from the home screen.

==================================================
STEP 1 — DASHBOARD FOUNDATION
==================================================

STATUS:
COMPLETE

The existing implementation includes:
- Dashboard screen
- Dashboard ViewModel/UI state
- Real Room expense data
- Dashboard navigation
- Basic spending summary
- Recent transactions
- Empty state
- Tap recent transaction → Edit Expense

Do NOT unnecessarily rewrite this existing implementation.

==================================================
STEP 2 — FINANCIAL SUMMARY
==================================================

OBJECTIVE:

Ensure the dashboard provides reliable financial summary metrics.

Implement/verify:

1. Current-month spending
   - Total amount spent during the current calendar month.
   - Must use real Room data.
   - No hardcoded/demo values.

2. Today's spending
   - Total amount spent today.
   - Use the device's local date correctly.

3. Current-month transaction count
   - Number of expenses recorded during the current month.

4. Ensure calculations are correct for:
   - Multiple expenses
   - Same-day expenses
   - Different dates
   - Zero expenses
   - Edited expenses
   - Deleted expenses

5. Add focused unit tests for all calculations.

6. Preserve the existing architecture.

STOP after Step 2.

==================================================
STEP 3 — RECENT TRANSACTIONS
==================================================

OBJECTIVE:

Create a useful recent transaction section on the dashboard.

Requirements:

- Show the latest expenses.
- Display:
  - Title/description
  - Category
  - Date
  - Amount
- Sort newest first.
- Limit the dashboard list to a reasonable number of recent items.
- Tapping a transaction should open Edit Expense.
- Do not create a second expense data source.
- Reuse the existing Expense model/repository/use cases.
- Ensure deleted expenses disappear.
- Ensure edited expenses display their updated information.

Add focused tests for ordering and displayed data.

STOP after Step 3.

==================================================
STEP 4 — REACTIVE DASHBOARD
==================================================

OBJECTIVE:

Make the dashboard fully reactive to database changes.

Requirements:

1. Add Expense:
   - Dashboard updates automatically.

2. Edit Expense:
   - Dashboard updates automatically.

3. Delete Expense:
   - Dashboard updates automatically.

4. No manual refresh button should be required.

5. Reuse Room Flow/reactive architecture already present.

6. Do not introduce unnecessary polling or duplicate refresh logic.

Test the reactive behavior.

STOP after Step 4.

==================================================
STEP 5 — EMPTY & EDGE STATES
==================================================

OBJECTIVE:

Make the dashboard behave correctly when data is missing or unusual.

Handle:

- No expenses at all.
- No expenses today.
- No expenses during the current month.
- Recent transactions unavailable.
- Zero spending.

Requirements:

- Do not display fake transactions.
- Do not display misleading financial values.
- Empty states should be clear and useful.
- Provide an appropriate Add Expense action where useful.
- Preserve normal dashboard behavior when data exists.

Add focused tests for these states.

STOP after Step 5.

==================================================
STEP 6 — DASHBOARD UI POLISH
==================================================

OBJECTIVE:

Polish the dashboard so it feels like the real Finly home screen.

UI direction:

- Dark-mode friendly.
- Clean.
- Minimal.
- Modern.
- Colorful but restrained.
- Strong visual hierarchy.
- Good spacing.
- Easy to scan quickly.

Avoid:

- Unnecessary decorative icons.
- Excessive badges.
- Random colors.
- Large illustrations.
- Excessive borders.
- Excessive gradients.
- Visual clutter.

Use the existing Finly theme/design system.

Do NOT redesign the entire application.

The Dashboard, Expenses, Add Expense, and Edit Expense screens
should feel like the same application.

Verify that UI polish does not break functionality.

STOP after Step 6.

==================================================
STEP 7 — FULL PHASE 3 VERIFICATION
==================================================

OBJECTIVE:

Perform final validation of the complete Phase 3 implementation.

Run:

- All unit tests.
- Relevant integration/regression tests.
- Debug build.

Verify on the existing Pixel 9 emulator:

1. Dashboard opens correctly.
2. Current-month spending is correct.
3. Today's spending is correct.
4. Transaction count is correct.
5. Recent transactions are correct.
6. Add Expense updates Dashboard.
7. Edit Expense updates Dashboard.
8. Delete Expense updates Dashboard.
9. Empty state works.
10. App restart preserves data.
11. Existing Phase 2 functionality still works:
    - Add
    - Edit
    - Delete
    - Search
    - Filter
    - Sort

If any regression appears, fix only what is necessary.

==================================================
ARCHITECTURE & SAFETY RULES
==================================================

Throughout ALL Phase 3 steps:

- Kotlin + Jetpack Compose.
- Preserve existing Room database.
- Preserve existing repository architecture.
- Preserve existing ViewModels/use cases.
- Do not create duplicate models.
- Do not create duplicate database systems.
- Do not add unnecessary dependencies.
- Do not change package name:
  com.personalexpensetracker
- Do not unnecessarily rename existing classes/files.
- Do not delete working functionality.
- Do not reset the database.
- Do not introduce fake/demo financial data.

==================================================
GIT RULE
==================================================

DO NOT perform ANY Git operations.

Do not:
- git add
- git commit
- git push
- git pull
- git reset
- git checkout
- git branch
- modify Git configuration

Git will be handled separately.

==================================================
MOST IMPORTANT EXECUTION RULE
==================================================

Even though all seven steps are documented here:

IMPLEMENT ONLY THE NEXT UNCOMPLETED STEP.

Do not implement future steps early.

After completing the step, test it, build it, verify it,
provide a concise report, and STOP.

Wait for my explicit instruction before proceeding.





*PHASE 4*

You are continuing development of the Finly Android app.

==================================================
PROJECT STATUS
==================================================

Phase 0 — Project Foundation: COMPLETE
Phase 1 — Local Database: COMPLETE
Phase 2 — Expense Management: COMPLETE
Phase 3 — Dashboard: IN PROGRESS
Phase 4 — Categories & Organization: NEXT

IMPORTANT:
The complete Phase 4 scope is defined below.

However, you MUST execute Phase 4 ONE STEP AT A TIME.

Do NOT implement all Phase 4 steps in a single task.

After completing each step:
1. Run relevant tests.
2. Build the app.
3. Verify the implementation.
4. Report exactly what was completed.
5. STOP.
6. Wait for my explicit instruction before starting the next step.

Do NOT automatically continue to the next step.

==================================================
PHASE 4 — CATEGORIES & ORGANIZATION
==================================================

OBJECTIVE:

Improve how expenses are categorized, organized, searched,
filtered, and managed inside Finly.

The category system should be consistent throughout the
application and should provide a foundation for future
analytics, budgets, and expense intelligence.

EXPECTED RESULT:

Users can easily categorize expenses, find expenses,
filter them by category, and keep their transactions
organized.

==================================================
STEP 1 — CATEGORY FOUNDATION
==================================================

OBJECTIVE:

Establish a centralized and reliable built-in category system.

FIRST:
Inspect the existing Expense model, Add Expense screen,
Edit Expense screen, Expense List, ViewModels, repository,
and existing category filtering implementation.

IMPORTANT:
Phase 2 already has category functionality.

Do NOT unnecessarily rewrite working functionality.

Implement:

1. Create one centralized source of truth for built-in categories.

Initial categories:

- Food
- Transportation
- Shopping
- Bills
- Entertainment
- Health
- Education
- Travel
- Other

2. Add Expense must use this centralized category list.

3. Edit Expense must use the same category list.

4. Existing category filtering must use the same source.

5. Dashboard must continue displaying categories correctly.

6. Existing expenses must remain intact.

7. Do NOT reset or recreate the Room database.

8. Do NOT create a separate category database/table yet
unless the existing architecture genuinely requires it.

9. Do not create different hardcoded category lists
inside individual screens.

10. Add focused tests for the centralized category system.

STOP after Step 1.

==================================================
STEP 2 — CATEGORY SELECTION UX
==================================================

OBJECTIVE:

Improve the way users select categories when adding
or editing an expense.

Requirements:

- Make category selection easy to understand.
- Use the existing Finly visual language.
- Categories should be clearly distinguishable.
- Keep the interface compact and clean.
- Do not use excessive decorative icons.
- Do not introduce unnecessary animations.
- Do not redesign the entire Add/Edit screens.
- Maintain accessibility and reasonable touch targets.

Verify:

- Add Expense category selection.
- Edit Expense category selection.
- Existing selected category remains selected when editing.
- Saving preserves the selected category.

Add/update relevant UI tests where practical.

STOP after Step 2.

==================================================
STEP 3 — CATEGORY FILTERING
==================================================

OBJECTIVE:

Make category-based expense filtering reliable and
easy to use.

Requirements:

- Expense List must allow filtering by category.
- Use the centralized category source.
- Preserve existing search functionality.
- Preserve existing date filters.
- Preserve existing sorting.
- Category filter must work together with:
  - Search
  - Date filter
  - Sorting

Examples:

Food + Today
Food + Search
Transportation + This Month
Food + Amount High to Low

Requirements:

- Correct matching.
- Correct empty/no-results state.
- Reset filters must clear the category filter as well.
- Existing Phase 2 filtering behavior must not regress.

Add focused ViewModel tests for combinations of
category/search/date/sort where necessary.

STOP after Step 3.

==================================================
STEP 4 — CATEGORY ORGANIZATION
==================================================

OBJECTIVE:

Improve the overall organization of categories throughout
the application.

Requirements:

- Use consistent category names everywhere.
- Ensure capitalization is consistent.
- Ensure Add Expense, Edit Expense, Expense List,
  Dashboard, and filters display the same category names.
- Prevent accidental category spelling variations.
- Prevent duplicate built-in category definitions.
- Keep category-related logic in an appropriate layer,
  not scattered across composables.

Review the codebase for existing hardcoded category values.

Consolidate them where appropriate without breaking
existing functionality.

STOP after Step 4.

==================================================
STEP 5 — CUSTOM CATEGORIES FOUNDATION
==================================================

OBJECTIVE:

Allow users to create their own categories.

IMPORTANT:
This is the first step that may require persistent
category data.

Before implementation:

- Inspect the existing database architecture.
- Determine the safest way to introduce custom categories.
- Do not destroy or migrate existing expense data incorrectly.
- Preserve all existing built-in category values.

Implement:

1. Custom category creation.

2. User-provided category name.

3. Basic validation:
   - Cannot be empty.
   - Cannot consist only of spaces.
   - Reasonable maximum length.
   - Prevent duplicate category names.

4. Custom categories should become available in:
   - Add Expense
   - Edit Expense
   - Category filtering

5. Built-in categories must remain available.

6. Do not allow users to accidentally delete or corrupt
   built-in categories.

7. Use Room migration if a schema change is actually required.

8. Add database/repository/ViewModel tests.

IMPORTANT:
Do not implement advanced category management yet.

STOP after Step 5.

==================================================
STEP 6 — CATEGORY MANAGEMENT
==================================================

OBJECTIVE:

Provide a simple place for users to manage custom categories.

Users should be able to:

- View their categories.
- Create a custom category.
- Rename a custom category where safe.
- Delete a custom category where safe.

IMPORTANT DATA RULE:

Deleting or renaming a category must NEVER silently
destroy expense records.

Before implementing deletion behavior, determine how
existing expenses using that category should be handled.

A safe approach may be:

- Prevent deletion while expenses use the category, OR
- Require reassignment to another category, OR
- Move affected expenses to "Other".

Choose the approach that best fits the existing architecture
and document the behavior.

Built-in categories must remain protected.

Add tests for:

- Create.
- Rename.
- Duplicate prevention.
- Delete.
- Expenses associated with a custom category.
- Built-in category protection.

STOP after Step 6.

==================================================
STEP 7 — CATEGORY POLISH & INTEGRATION
==================================================

OBJECTIVE:

Make the complete category system feel consistent
throughout Finly.

Review:

- Dashboard
- Expense List
- Add Expense
- Edit Expense
- Filters
- Category management

Ensure:

- Same category names everywhere.
- Same visual language everywhere.
- No unnecessary category icons.
- No excessive colors.
- No duplicated UI logic.
- No broken navigation.
- No regression in existing expense functionality.

Keep Finly:

- Dark-mode friendly.
- Minimal.
- Modern.
- Colorful but restrained.
- Easy to scan.

Do NOT redesign the whole application.

STOP after Step 7.

==================================================
STEP 8 — FULL PHASE 4 TESTING & VERIFICATION
==================================================

OBJECTIVE:

Perform complete validation of Phase 4.

Run:

- All unit tests.
- Relevant UI tests.
- Database tests/migration tests if applicable.
- Debug build.

Verify on the Pixel 9 emulator:

1. Add expense with built-in category.
2. Edit expense category.
3. Filter by category.
4. Combine category + search.
5. Combine category + date filter.
6. Combine category + sorting.
7. Reset filters.
8. Create custom category.
9. Use custom category in Add Expense.
10. Edit an expense to a custom category.
11. Filter by custom category.
12. Rename custom category if implemented.
13. Delete custom category if implemented.
14. Verify associated expenses remain safe.
15. Restart the application.
16. Verify category data persists.
17. Verify existing expenses remain intact.
18. Verify Dashboard still works.
19. Verify Phase 2 functionality still works.
20. Verify Phase 3 Dashboard functionality still works.

Check for:

- Crashes.
- Incorrect totals.
- Broken filters.
- Duplicate categories.
- Data loss.
- Database migration issues.
- Navigation regressions.

If something fails, fix only what is necessary.

STOP after Step 8.

==================================================
ARCHITECTURE & SAFETY RULES
==================================================

Throughout ALL Phase 4 steps:

- Kotlin.
- Jetpack Compose.
- Room.
- Existing repository architecture.
- Existing ViewModel/use-case architecture.
- Existing Finly theme/design system.

Do not introduce unnecessary dependencies.

Do not create duplicate models.

Do not create duplicate category systems.

Do not create duplicate database systems.

Do not change package name:

com.personalexpensetracker

Do not unnecessarily rename existing files/classes.

Do not delete working functionality.

Do not reset the database.

Do not use fake/demo financial data.

Existing Phase 1–3 functionality must remain functional.

==================================================
GIT RULE
==================================================

DO NOT perform ANY Git operations.

Do not:

- git add
- git commit
- git push
- git pull
- git reset
- git checkout
- git branch
- modify Git configuration

Git will be handled separately.

==================================================
MOST IMPORTANT EXECUTION RULE
==================================================

The complete Phase 4 roadmap contains 8 steps.

BUT:

IMPLEMENT ONLY THE NEXT UNCOMPLETED STEP.

Do not implement future steps early.

After completing the current step:

- Test it.
- Build it.
- Verify it.
- Report the result.
- STOP.

Wait for my explicit instruction before proceeding
to the next Phase 4 step.

DO NOT automatically continue.



# **PHASE 5 **



You are continuing development of the Finly Android app.

==================================================
PROJECT STATUS
==================================================

Phase 0 — Project Foundation: COMPLETE
Phase 1 — Local Database: COMPLETE
Phase 2 — Expense Management: COMPLETE
Phase 3 — Dashboard: IN PROGRESS
Phase 4 — Categories & Organization: NEXT
Phase 5 — Analytics & Insights: PLANNED

IMPORTANT:
The complete Phase 5 scope is defined below.

However, you MUST execute Phase 5 ONE STEP AT A TIME.

Do NOT implement all Phase 5 steps in a single task.

After completing each step:
1. Run relevant tests.
2. Build the app.
3. Verify the implementation.
4. Report exactly what was completed.
5. STOP.
6. Wait for my explicit instruction before starting the next step.

Do NOT automatically continue to the next step.

==================================================
PHASE 5 — ANALYTICS & INSIGHTS
==================================================

OBJECTIVE:

Add spending analytics that help users understand their
spending behavior through category breakdowns, monthly
trends, daily/weekly spending, and historical comparisons.

EXPECTED RESULT:

Users can understand where their money is going, how
their spending changes over time, and how different
periods compare.

==================================================
STEP 1 — ANALYTICS FOUNDATION
==================================================

OBJECTIVE:

Create the foundation for the analytics system using
the existing real expense data.

FIRST inspect:

- Expense model
- Room database
- DAO
- Repository
- Existing use cases
- Dashboard ViewModel
- Category system
- Existing date/time handling

Do NOT create a second database or duplicate expense system.

Create an appropriate analytics/domain layer if needed.

The analytics layer should be capable of calculating:

- Total spending
- Spending by category
- Spending by day
- Spending by week
- Spending by month
- Transaction counts

All calculations must use real stored expenses.

Do NOT use fake/demo data.

Add focused unit tests.

STOP after Step 1.

==================================================
STEP 2 — CATEGORY SPENDING BREAKDOWN
==================================================

OBJECTIVE:

Show users how their spending is distributed across
categories.

Implement:

- Category spending totals.
- Category transaction counts where useful.
- Percentage/share of total spending.
- Sort categories by spending amount where appropriate.
- Handle categories with zero spending correctly.
- Respect the selected date period.

Create a clean category breakdown visualization.

Prefer a simple, readable visualization rather than
overloading the screen.

The visualization must be driven entirely by real
expense data.

Add tests for:

- Correct category totals.
- Correct percentages.
- Multiple categories.
- One category.
- Empty data.
- Categories with no transactions.

STOP after Step 2.

==================================================
STEP 3 — MONTHLY SPENDING TRENDS
==================================================

OBJECTIVE:

Show how total spending changes across months.

Implement:

- Monthly spending totals.
- Chronological ordering.
- Appropriate historical range.
- Current month included.
- Months with no spending represented correctly
  where appropriate.

Add a clear trend visualization.

The chart should make it easy to answer:

"Am I spending more or less than previous months?"

Requirements:

- Use real expense dates.
- Correctly handle month/year boundaries.
- Do not assume all months have transactions.
- Do not use fake data.

Add focused tests for monthly aggregation.

STOP after Step 3.

==================================================
STEP 4 — DAILY & WEEKLY SPENDING
==================================================

OBJECTIVE:

Allow users to understand shorter-term spending behavior.

Implement daily spending analytics.

Support a useful recent period such as:

- Last 7 days
- Current week

Use the existing date/time conventions in the app.

Also implement weekly aggregation where appropriate.

Requirements:

- Correct day boundaries.
- Correct week boundaries.
- Correct handling of days with zero spending.
- Correct chronological ordering.
- Real database data only.

Provide appropriate visualizations for daily/weekly
spending.

Do not create unnecessary duplicate screens.

Add tests for:

- Daily totals.
- Weekly totals.
- Empty days.
- Week boundaries.
- Date transitions.

STOP after Step 4.

==================================================
STEP 5 — PERIOD SELECTION
==================================================

OBJECTIVE:

Allow users to choose which time period analytics
should represent.

Provide a clean period selection mechanism.

At minimum support:

- Today
- This week
- This month
- Last month
- Recent months / historical view

Use the existing Finly design language.

When the selected period changes:

- Summary values update.
- Category breakdown updates.
- Relevant trend data updates.
- No manual database refresh should be necessary.

Avoid creating unnecessary duplicated queries.

Add tests for period selection and date ranges.

STOP after Step 5.

==================================================
STEP 6 — HISTORICAL COMPARISONS
==================================================

OBJECTIVE:

Help users compare spending between meaningful periods.

Implement useful comparisons such as:

- This month vs last month.
- Current period vs previous equivalent period.

Show:

- Current spending.
- Previous spending.
- Absolute difference.
- Percentage change where mathematically meaningful.

Correctly handle:

- Previous period = zero.
- Current period = zero.
- Both periods = zero.

Do NOT present misleading percentage changes.

Keep comparison information simple and understandable.

Add focused unit tests for comparison calculations.

STOP after Step 6.

==================================================
STEP 7 — ANALYTICS UI & INSIGHTS POLISH
==================================================

OBJECTIVE:

Turn the analytics functionality into a clean,
usable Finly analytics experience.

UI requirements:

- Dark-mode friendly.
- Modern.
- Minimal.
- Colorful but restrained.
- Consistent with Dashboard and Expense screens.
- Clear hierarchy.
- Easy-to-read numbers.
- Clear chart labels.
- Good spacing.

Avoid:

- Excessive charts.
- Decorative illustrations.
- Excessive colors.
- Random icons.
- Unnecessary gradients.
- Visual clutter.
- Tiny unreadable charts.
- Charts that communicate no useful information.

Every visualization must answer a useful financial question.

Examples:

"Where did I spend the most?"

"How much did I spend this month?"

"Is my spending increasing?"

"How does this month compare with last month?"

"How much am I spending each day?"

Do NOT redesign the entire application.

STOP after Step 7.

==================================================
STEP 8 — FULL PHASE 5 TESTING & VERIFICATION
==================================================

OBJECTIVE:

Perform complete validation of the Analytics & Insights
phase.

Run:

- All unit tests.
- Analytics tests.
- Relevant UI tests.
- Database tests where applicable.
- Debug build.

Verify on the Pixel 9 emulator:

1. Analytics screen opens correctly.
2. Category totals are correct.
3. Category percentages are correct.
4. Monthly trend is correct.
5. Daily spending is correct.
6. Weekly spending is correct.
7. Period selection works.
8. This month vs last month works.
9. Zero-spending periods behave correctly.
10. Add Expense updates analytics.
11. Edit Expense updates analytics.
12. Delete Expense updates analytics.
13. Category changes update analytics.
14. App restart preserves data and analytics.
15. Dashboard still works.
16. Expense List still works.
17. Search still works.
18. Filtering still works.
19. Sorting still works.
20. Phase 2 and Phase 3 functionality has no regressions.

Check for:

- Incorrect totals.
- Incorrect date boundaries.
- Incorrect category aggregation.
- Incorrect percentages.
- Incorrect comparisons.
- Crashes.
- Empty-state problems.
- Performance problems.
- Database issues.
- UI regressions.

If anything fails, fix only what is necessary.

STOP after Step 8.

==================================================
ARCHITECTURE & SAFETY RULES
==================================================

Throughout ALL Phase 5 steps:

- Kotlin.
- Jetpack Compose.
- Room.
- Existing repository architecture.
- Existing ViewModel/use-case architecture.
- Existing Finly theme/design system.

Reuse existing expense data.

Do not create a duplicate database.

Do not create duplicate Expense models.

Do not create duplicate category systems.

Do not hardcode financial results.

Do not use fake/demo financial data.

Do not unnecessarily add dependencies.

Do not change package name:

com.personalexpensetracker

Do not unnecessarily rename existing files/classes.

Do not delete working functionality.

Do not reset the database.

Preserve all completed functionality from previous phases.

==================================================
GIT RULE
==================================================

DO NOT perform ANY Git operations.

Do not:

- git add
- git commit
- git push
- git pull
- git reset
- git checkout
- git branch
- modify Git configuration

Git will be handled separately.

==================================================
MOST IMPORTANT EXECUTION RULE
==================================================

The complete Phase 5 roadmap contains 8 steps.

BUT:

IMPLEMENT ONLY THE NEXT UNCOMPLETED STEP.

Do not implement future steps early.

After completing the current step:

- Test it.
- Build it.
- Verify it.
- Report the result.
- STOP.

Wait for my explicit instruction before proceeding
to the next Phase 5 step.

DO NOT automatically continue.



# PHASE 6 



# FINLY — PHASE 6 MASTER IMPLEMENTATION PROMPT
# Budget System
# Execute Step 1 → Stop → Wait → Step 2 → Stop → ... → Step 8

You are working on the Finly Personal Expense Tracker Android application.

PHASE 6:
Budget System

OBJECTIVE:
Add monthly and category budgets, budget progress, remaining amounts, and overspending warnings.

EXPECTED RESULT:
Users can set spending limits and clearly track their spending progress against those limits.

==================================================
IMPORTANT EXECUTION RULE
==================================================

This is a MASTER PROMPT containing all Phase 6 steps.

DO NOT implement the entire phase at once.

Execute ONLY ONE STEP at a time.

After completing the current step:

1. Implement only that step.
2. Run relevant unit/tests.
3. Build the Android application.
4. Fix any issues caused by the current step.
5. Verify the result.
6. Update relevant project documentation if necessary.
7. Report exactly what was implemented and verified.
8. STOP.

Do NOT continue to the next step automatically.

Wait for my explicit instruction before starting the next step.

==================================================
PROJECT SAFETY RULES
==================================================

- Preserve the existing Finly architecture.
- Do not rewrite working Phase 1–5 functionality.
- Reuse existing Room/database, repository, use-case, ViewModel, and UI architecture where appropriate.
- Follow existing Kotlin + Jetpack Compose + Material 3 conventions.
- Keep the app local-first.
- Do not introduce cloud/backend functionality.
- Do not add unnecessary dependencies.
- Do not change the application package unnecessarily.
- Do not rename existing database entities unnecessarily.
- Preserve existing expense functionality.
- Existing Add/Edit/Delete/Search/Filter/Sort/Dashboard/Analytics functionality must continue working.
- Budget calculations must use real stored expense data.
- Avoid duplicated business logic.
- Keep financial calculations deterministic and testable.
- Use proper date boundaries and the existing time/date abstraction where applicable.
- Handle zero budgets and zero spending safely.
- Avoid crashes from missing, deleted, or invalid budget data.
- Do not perform Git operations.
- Do not commit, push, reset, checkout, rebase, or modify Git history.
- Keep implementation changes small and controlled.

==================================================
PHASE 6 STEPS
==================================================


##################################################
STEP 1 — BUDGET FOUNDATION
##################################################

Objective:
Create the foundational budget data model and architecture.

First inspect the existing Phase 1–5 architecture.

Determine how expenses, categories, repositories, Room database, use cases, and date handling are currently structured.

Design the minimum required budget model.

Support at minimum:

- Monthly budget
- Category budget
- Budget amount
- Budget period
- Category association where applicable
- Created/updated information if required by the existing architecture

Create the necessary:

- Room entity/entities
- DAO
- repository layer
- domain model if the architecture uses one
- use cases where appropriate

Add the required Room migration safely if the database schema changes.

Do NOT build the complete budget UI yet.

Budget persistence must survive:

- app restart
- screen recreation
- process recreation where Room persistence applies

Add focused unit/database tests.

Verify:

- Budget records can be created.
- Budget records can be retrieved.
- Budget records can be updated.
- Budget records can be deleted if deletion is part of the model.
- Existing expense functionality still works.
- Database migration succeeds.

Build and test.

STOP after Step 1.


##################################################
STEP 2 — MONTHLY BUDGET
##################################################

Objective:
Allow the user to create and manage an overall monthly spending budget.

Build the monthly budget functionality using the foundation from Step 1.

Support:

- Set monthly budget
- View current monthly budget
- Edit monthly budget
- Remove/disable monthly budget if appropriate
- Validate budget amount
- Prevent invalid or negative budget values

The monthly budget must apply to the correct calendar month.

Use the existing date/time architecture rather than hardcoding dates.

Create the necessary:

- ViewModel/state
- use cases
- Compose UI

Keep the UI consistent with Finly's existing design language.

Do not implement category budgets yet unless required by the shared foundation.

Add tests for:

- Creating a monthly budget
- Updating a monthly budget
- Invalid amount
- Correct month association
- Persistence
- Restart/reload behavior

Build and verify on the Pixel 9 emulator if practical.

STOP after Step 2.


##################################################
STEP 3 — CATEGORY BUDGETS
##################################################

Objective:
Allow users to assign spending limits to individual categories.

Use the centralized category system created in Phase 4.

Support:

- Select category
- Set category budget
- Edit category budget
- Remove category budget
- View existing category budgets
- Prevent duplicate budgets for the same category and period
- Validate budget amounts

Category budgets must use the same category definitions as:

- Add Expense
- Edit Expense
- Expense filtering
- Dashboard
- Analytics

Do not create a second independent category system.

Add tests for:

- Creating category budgets
- Updating category budgets
- Duplicate prevention
- Invalid amounts
- Correct category association
- Correct month/period association
- Persistence

Build and verify.

STOP after Step 3.


##################################################
STEP 4 — BUDGET CALCULATIONS & PROGRESS
##################################################

Objective:
Calculate real-time spending against budgets.

Create the budget calculation layer.

For each applicable budget calculate:

- Budget limit
- Amount spent
- Remaining amount
- Percentage used
- Overspent amount when applicable
- Budget status

At minimum support statuses equivalent to:

- Not started
- On track
- Near limit
- Overspent

Use real expense data from the existing database.

Monthly budget spending must calculate the user's actual spending for that month.

Category budget spending must calculate actual spending for that category within the applicable period.

Calculations must react to:

- New expenses
- Edited expenses
- Deleted expenses
- Category changes
- Budget changes

Handle edge cases:

- No expenses
- No budget
- Zero spending
- Budget exactly reached
- Budget exceeded
- Invalid/zero budget
- Month with no transactions

Do not duplicate calculation logic across screens.

Create focused unit tests covering the calculations thoroughly.

Build and verify.

STOP after Step 4.


##################################################
STEP 5 — BUDGET DASHBOARD / PROGRESS UI
##################################################

Objective:
Make budget progress visible to the user.

Integrate budget information into the existing Finly UI.

Show the current month's overall budget when one exists.

Display useful information such as:

- Budget amount
- Spent amount
- Remaining amount
- Percentage used
- Progress indicator
- Overspending state

Also show category budget progress where appropriate.

The UI should be:

- Clean
- Minimal
- Dark-mode compatible
- Easy to scan
- Consistent with the existing Finly design
- Information-focused

Avoid excessive decorative elements.

Do not redesign unrelated screens.

Ensure progress indicators correctly represent:

- Low usage
- Moderate usage
- Near-limit usage
- Full usage
- Overspending

Use accessible text in addition to visual indicators so the information is understandable without relying only on color.

Add relevant UI/ViewModel tests if the existing project structure supports them.

Build and verify on Pixel 9.

STOP after Step 5.


##################################################
STEP 6 — OVERSPENDING WARNINGS
##################################################

Objective:
Clearly warn users when they are approaching or exceeding their budgets.

Implement budget warning logic.

Support warnings for:

- Nearing monthly budget limit
- Exceeding monthly budget
- Nearing category budget limit
- Exceeding category budget

Use sensible thresholds while keeping the threshold logic centralized and configurable.

Do not scatter hardcoded warning logic across multiple screens.

Warnings should clearly communicate:

- What budget is affected
- How much has been spent
- How much remains, if applicable
- How much the user is over budget, if applicable

Avoid aggressive or annoying notification behavior.

For the initial implementation, keep warnings inside the application UI unless an existing project requirement explicitly requires system notifications.

Add tests for warning thresholds and edge cases.

Build and verify.

STOP after Step 6.


##################################################
STEP 7 — BUDGET MANAGEMENT & UI POLISH
##################################################

Objective:
Create a complete and consistent budget management experience.

Add or refine the Budget screen/section so users can:

- View all active budgets
- View monthly budget
- View category budgets
- Create budgets
- Edit budgets
- Remove budgets
- Understand progress
- Identify warnings

Ensure budget-related UI is consistent across:

- Dashboard
- Budget screen/section
- Category selection
- Expense data
- Analytics where applicable

Handle empty states:

- No budgets created
- No category budgets
- No spending yet
- Budget exceeded
- No current-month budget

Keep the design aligned with Finly:

- Minimal
- Clean
- Dark mode compatible
- Restrained accent colors
- No unnecessary icons/decorations
- Clear typography
- Good spacing
- Clear hierarchy

Do not add unrelated features.

Build and verify on Pixel 9.

STOP after Step 7.


##################################################
STEP 8 — FULL PHASE 6 TESTING & VERIFICATION
##################################################

Objective:
Perform complete Phase 6 verification.

Run the complete project test suite.

Verify:

1. Existing Phase 1 functionality
   - Database
   - Expense persistence

2. Existing Phase 2 functionality
   - Add expense
   - Edit expense
   - Delete expense
   - Search
   - Filter
   - Sort

3. Existing Phase 3 functionality
   - Dashboard
   - Monthly spending
   - Today's spending
   - Recent transactions

4. Existing Phase 4 functionality
   - Categories
   - Custom categories
   - Category filtering
   - Category management

5. Existing Phase 5 functionality
   - Analytics
   - Category breakdown
   - Trends
   - Period selection
   - Comparisons

6. Phase 6 functionality
   - Monthly budgets
   - Category budgets
   - Budget persistence
   - Budget editing
   - Budget deletion/removal
   - Spending calculations
   - Remaining amounts
   - Progress indicators
   - Near-limit warnings
   - Overspending warnings

Perform integration/regression scenarios:

TEST A:
Create a monthly budget → add expense → verify spent and remaining values.

TEST B:
Create category budget → add expense in that category → verify category progress.

TEST C:
Edit an expense → verify budget calculations update.

TEST D:
Delete an expense → verify budget calculations update.

TEST E:
Exceed a budget → verify overspending state.

TEST F:
Approach a budget limit → verify warning state.

TEST G:
Restart application → verify budgets persist.

TEST H:
Change expense category → verify the correct category budget updates.

TEST I:
Move across month boundaries/date conditions → verify calculations use the correct month.

TEST J:
Use multiple category budgets simultaneously → verify each budget remains independent.

Verify database migrations from the existing project state.

Build the release/debug APK successfully.

Install and run on the Pixel 9 emulator.

Check for:

- crashes
- incorrect calculations
- stale UI
- incorrect month boundaries
- duplicate budgets
- broken category references
- broken existing functionality
- layout issues
- dark-mode issues

Only after all verification succeeds should Phase 6 be considered COMPLETE.

Update:

- phases/PHASE_06_BUDGET_SYSTEM.md
- CHANGELOG.md
- DEVELOPMENT_PLAN.md

Mark Phase 6 as COMPLETE only if all required tests and verification pass.

Do NOT perform Git operations.

STOP and provide the final Phase 6 verification report.

==================================================
FINAL RULE
==================================================

ONE STEP ONLY PER EXECUTION.

Never automatically proceed from Step N to Step N+1.

After each step:
IMPLEMENT → TEST → BUILD → VERIFY → REPORT → STOP.

Wait for my explicit instruction before continuing.



# PHASE 7

# FINLY — PHASE 7 MASTER IMPLEMENTATION PROMPT
# Recurring Expenses
# Execute Step 1 → Stop → Wait → Step 2 → Stop → ... → Step 8

You are working on the Finly Personal Expense Tracker Android application.

PHASE 7:
Recurring Expenses

OBJECTIVE:
Add support for recurring expenses such as rent, subscriptions, bills, memberships, and other regular payments.

EXPECTED RESULT:
Users can create recurring expenses once and Finly can manage their recurring schedule and automatically generate the corresponding expense transactions when they become due.

==================================================
IMPORTANT EXECUTION RULE
==================================================

This is a MASTER PROMPT containing all Phase 7 steps.

DO NOT implement the entire phase at once.

Execute ONLY ONE STEP at a time.

After completing the current step:

1. Implement only that step.
2. Run relevant tests.
3. Build the Android application.
4. Fix issues caused by the current step.
5. Verify the result.
6. Update relevant project documentation if necessary.
7. Report exactly what was implemented and verified.
8. STOP.

Do NOT continue automatically to the next step.

Wait for my explicit instruction before starting the next step.

==================================================
PROJECT SAFETY RULES
==================================================

- Preserve the existing Finly architecture.
- Do not rewrite working Phase 1–6 functionality.
- Reuse existing Room/database, repository, use-case, ViewModel, and UI architecture.
- Follow existing Kotlin + Jetpack Compose + Material 3 conventions.
- Keep the app local-first.
- Do not introduce cloud/backend functionality.
- Do not add unnecessary dependencies.
- Do not change the application package.
- Do not unnecessarily rename existing entities/classes.
- Preserve existing expense functionality.
- Preserve existing budget functionality.
- Recurring expenses must integrate with the existing Expense system.
- Automatically generated expenses must be normal expenses in the existing database.
- Avoid duplicated business logic.
- Make recurrence calculations deterministic and testable.
- Use the existing date/time abstraction where available.
- Handle month/year boundaries correctly.
- Prevent duplicate automatic expense generation.
- Do not generate future expenses prematurely unless explicitly required by the recurrence design.
- Do not perform Git operations.
- Do not commit, push, reset, checkout, rebase, or modify Git history.

==================================================
PHASE 7 STEPS
==================================================


##################################################
STEP 1 — RECURRING EXPENSE FOUNDATION
##################################################

Objective:
Create the data foundation for recurring expenses.

First inspect the existing Phase 1–6 architecture.

Understand:

- Expense entity
- Category system
- Room database
- Repository
- Use cases
- Date/time handling
- Budget calculations
- Existing navigation/UI structure

Design a recurring expense model that can support at minimum:

- Title/description
- Amount
- Category
- Notes if supported by the existing expense model
- Recurrence frequency
- Start date
- Next occurrence date
- Active/inactive status
- Optional end date
- Reference to the recurring definition where appropriate

Supported recurrence frequencies should be designed for:

- Daily
- Weekly
- Monthly
- Yearly

Do not build the complete UI yet.

Create the necessary:

- Room entity
- DAO
- Domain model where appropriate
- Repository
- Use cases where appropriate

Add database migration if required.

Ensure recurring definitions persist across app restarts.

Add focused tests.

Verify:

- Create recurring definition
- Read recurring definition
- Update recurring definition
- Delete/deactivate recurring definition
- Persistence
- Database migration

Build and test.

STOP after Step 1.


##################################################
STEP 2 — CREATE & EDIT RECURRING EXPENSES
##################################################

Objective:
Allow users to create and edit recurring expense definitions.

Create a clean Compose UI for:

- Adding recurring expense
- Editing recurring expense

Fields should include:

- Description/title
- Amount
- Category
- Frequency
- Start date
- Optional end date
- Active status where appropriate
- Notes if supported

Frequency selector should clearly display:

- Daily
- Weekly
- Monthly
- Yearly

Validate:

- Required title
- Valid positive amount
- Valid category
- Valid start date
- Valid end date if provided
- End date cannot be before start date

Use the existing centralized category system from Phase 4.

Do not duplicate category definitions.

Add tests for:

- Valid creation
- Invalid amount
- Missing title
- Invalid dates
- Frequency selection
- Editing existing recurring expense
- Persistence

Build and verify on Pixel 9.

STOP after Step 2.


##################################################
STEP 3 — RECURRENCE SCHEDULING ENGINE
##################################################

Objective:
Create the core logic that determines when a recurring expense becomes due.

Implement a deterministic recurrence calculation engine.

For every recurring expense, calculate:

- Current occurrence
- Next occurrence
- Whether an occurrence is due
- Next scheduled date after generation

Support:

- Daily recurrence
- Weekly recurrence
- Monthly recurrence
- Yearly recurrence

Correctly handle:

- Month boundaries
- Different month lengths
- Leap years
- Year boundaries
- Start dates
- End dates

Example:

A monthly expense starting on January 15 should calculate future occurrences based on the defined recurrence rule.

Do not rely on fragile fixed millisecond calculations for calendar recurrence when calendar-based date operations are more appropriate.

Keep recurrence calculations independent from the UI.

Add extensive unit tests.

Test:

- Daily
- Weekly
- Monthly
- Yearly
- Month-end dates
- Leap years
- Year transitions
- End dates
- Inactive recurring expenses
- Future start dates

Build and test.

STOP after Step 3.


##################################################
STEP 4 — AUTOMATIC EXPENSE GENERATION
##################################################

Objective:
Automatically create normal expenses when recurring payments become due.

Implement the generation mechanism using the existing expense architecture.

When a recurring expense becomes due:

1. Determine the scheduled occurrence.
2. Create a normal Expense record.
3. Preserve title/amount/category/notes.
4. Associate it with the recurring definition where appropriate.
5. Advance the recurring definition to its next occurrence.
6. Persist the updated schedule.

Generated transactions must appear in:

- Expense list
- Dashboard
- Analytics
- Budgets
- Search/filter/sort

They must behave exactly like normal expenses after creation.

CRITICAL DUPLICATE-PREVENTION RULE:

The same recurring occurrence must NEVER generate multiple expense records.

Generation must be idempotent.

If the application is opened multiple times on the same day, it must not repeatedly create the same occurrence.

Use a reliable occurrence identifier/date/reference strategy.

Do not create a new duplicate expense every time the app launches.

Add tests covering:

- First generation
- Repeated generation attempt
- Multiple overdue occurrences
- Future occurrence
- Inactive recurring expense
- End date
- Duplicate prevention
- Correct expense data

Build and test.

STOP after Step 4.


##################################################
STEP 5 — RECURRING EXPENSE MANAGEMENT UI
##################################################

Objective:
Create a dedicated recurring expenses management experience.

Add a Recurring Expenses screen/section.

Users should be able to see:

- Recurring expense title
- Amount
- Category
- Frequency
- Next occurrence
- Active/inactive state

Users should be able to:

- Add recurring expense
- Edit recurring expense
- Pause/deactivate recurring expense
- Resume recurring expense
- Delete recurring expense

Clearly distinguish:

- Recurring definition
- Generated expense transaction

Deleting or pausing a recurring definition must NOT accidentally delete previously generated normal expenses unless explicitly designed and confirmed.

Provide useful empty states when no recurring expenses exist.

Keep the UI consistent with Finly:

- Minimal
- Clean
- Dark-mode compatible
- Clear hierarchy
- Restrained colors
- No unnecessary decorative elements

Build and verify on Pixel 9.

STOP after Step 5.


##################################################
STEP 6 — SCHEDULER / APP-LIFECYCLE INTEGRATION
##################################################

Objective:
Ensure recurring expenses are processed automatically without requiring the user to manually create them.

Inspect the current Android architecture and choose the most appropriate reliable local scheduling mechanism.

Use Android-supported scheduling/background mechanisms appropriate for the project.

The scheduler should:

- Check recurring expenses at appropriate times
- Generate due expenses
- Update next occurrence
- Avoid duplicate generation
- Respect inactive recurring definitions
- Respect end dates

Do not create an aggressive background process.

Do not continuously run a service.

Do not drain battery unnecessarily.

The implementation should tolerate:

- App closed
- App reopened
- Device restart where applicable
- Missed scheduled execution
- Multiple scheduler executions

IMPORTANT:

The scheduler is a trigger.

The actual recurrence/generation logic must remain in the deterministic application/domain layer created in earlier steps.

The scheduler must NOT contain duplicated business logic.

Add tests for the generation workflow and scheduler-related integration where practical.

Build and verify.

STOP after Step 6.


##################################################
STEP 7 — INTEGRATION WITH FINLY
##################################################

Objective:
Integrate recurring expenses throughout the existing application.

Verify that generated recurring transactions correctly affect:

Dashboard:

- Today's spending
- Current-month spending
- Recent transactions

Expenses:

- Expense list
- Search
- Category filter
- Date filter
- Sorting

Categories:

- Correct category assignment
- Category spending

Analytics:

- Category breakdown
- Daily spending
- Weekly spending
- Monthly trends
- Historical comparisons

Budgets:

- Monthly budget progress
- Category budget progress
- Remaining budget
- Overspending warnings

Ensure recurring expenses do not create a separate parallel financial system.

A generated recurring payment must become part of Finly's normal financial data.

Also ensure editing/deleting a generated expense behaves like a normal expense and does not corrupt the recurring schedule.

Test scenarios such as:

TEST A:
Create monthly rent → generate occurrence → verify dashboard and budget update.

TEST B:
Create subscription → generate occurrence → verify analytics.

TEST C:
Pause recurring expense → verify no new occurrences are generated.

TEST D:
Resume recurring expense → verify future occurrence processing works.

TEST E:
Delete recurring definition → verify old generated expenses remain.

TEST F:
Edit recurring definition → verify future occurrences use the updated definition.

TEST G:
Generate multiple due occurrences → verify each valid occurrence is created exactly once.

Build and verify.

STOP after Step 7.


##################################################
STEP 8 — FULL PHASE 7 TESTING & VERIFICATION
##################################################

Objective:
Perform complete Phase 7 verification and regression testing.

Run the complete project test suite.

Verify all previous phases:

PHASE 1:
- Room database
- Persistence
- Migration

PHASE 2:
- Add expense
- Edit expense
- Delete expense
- Search
- Filter
- Sort

PHASE 3:
- Dashboard
- Monthly spending
- Today's spending
- Recent transactions

PHASE 4:
- Built-in categories
- Custom categories
- Category management
- Category filtering

PHASE 5:
- Analytics
- Category breakdown
- Monthly trends
- Daily/weekly spending
- Period selection
- Historical comparisons

PHASE 6:
- Monthly budgets
- Category budgets
- Progress
- Remaining amounts
- Overspending warnings

PHASE 7:
- Recurring expense creation
- Editing
- Deletion
- Pause/resume
- Daily recurrence
- Weekly recurrence
- Monthly recurrence
- Yearly recurrence
- Next occurrence calculation
- Automatic generation
- Duplicate prevention
- Scheduler integration
- Generated expense integration
- End dates
- Future start dates

Perform regression scenarios:

TEST 1:
Create recurring expense → restart app → verify definition remains.

TEST 2:
Generate recurring expense → restart app → verify same occurrence is NOT duplicated.

TEST 3:
Create recurring expense → generate → edit generated expense → verify recurring definition remains valid.

TEST 4:
Create recurring expense with category budget → generate → verify category budget updates.

TEST 5:
Create monthly recurring expense → generate → verify monthly analytics update.

TEST 6:
Create recurring expense near monthly budget limit → generate → verify warning state.

TEST 7:
Pause recurring expense → trigger processing → verify no expense is generated.

TEST 8:
Set an end date → process after end date → verify no further expense is generated.

TEST 9:
Create multiple recurring expenses → process them together → verify each generates independently.

TEST 10:
Use month-end/year-end dates → verify recurrence remains correct.

TEST 11:
Force/repeat scheduler execution → verify no duplicate transactions.

TEST 12:
Delete recurring definition → verify generated historical expenses remain intact.

Verify database migrations from the current project state.

Run the full test suite.

Build the APK successfully.

Install and run on the Pixel 9 emulator.

Check for:

- Crashes
- Duplicate expenses
- Incorrect dates
- Incorrect recurrence calculations
- Incorrect month boundaries
- Incorrect category assignment
- Incorrect budget calculations
- Incorrect analytics
- Stale UI
- Broken navigation
- Broken existing expense functionality
- Dark-mode UI issues
- Persistence problems

Only mark Phase 7 COMPLETE when all required tests and verification succeed.

Update:

- phases/PHASE_07_RECURRING_EXPENSES.md
- CHANGELOG.md
- DEVELOPMENT_PLAN.md

Mark Phase 7 as COMPLETE only after successful verification.

Do NOT perform Git operations.

STOP and provide the final Phase 7 verification report.

==================================================
FINAL EXECUTION RULE
==================================================

ONE STEP ONLY PER EXECUTION.

Never automatically proceed from Step N to Step N+1.

After each step:

IMPLEMENT
→ TEST
→ BUILD
→ VERIFY
→ REPORT
→ STOP

Wait for my explicit instruction before continuing.





PHASE 8 



# FINLY — PHASE 8 MASTER IMPLEMENTATION PROMPT
# Income and Balance
# Execute Step 1 → Stop → Wait → Step 2 → Stop → ... → Step 8

You are working on the Finly Personal Expense Tracker Android application.

PHASE 8:
Income and Balance

OBJECTIVE:
Add income tracking and calculate overall financial balance, income, expenses, and cash flow.

WHAT WE BUILD:
- Income transaction logging
- Total income calculation
- Total expense calculation
- Overall financial balance
- Cash flow reporting
- Income history
- Integration with Dashboard, Analytics, Budgets, and Recurring Expenses

EXPECTED RESULT:
Finly tracks both money coming in and money going out and provides a clear view of the user's overall financial position.

==================================================
IMPORTANT EXECUTION RULE
==================================================

This is a MASTER PROMPT containing all Phase 8 steps.

DO NOT implement the entire phase at once.

Execute ONLY ONE STEP at a time.

After completing the current step:

1. Implement only that step.
2. Run relevant tests.
3. Build the Android application.
4. Fix issues caused by the current step.
5. Verify the result.
6. Update relevant project documentation if necessary.
7. Report exactly what was implemented and verified.
8. STOP.

Do NOT continue automatically to the next step.

Wait for my explicit instruction before starting the next step.

==================================================
PROJECT SAFETY RULES
==================================================

- Preserve the existing Finly architecture.
- Do not rewrite working Phase 1–7 functionality.
- Reuse the existing Room/database, repository, use-case, ViewModel, and UI architecture.
- Follow existing Kotlin + Jetpack Compose + Material 3 conventions.
- Keep the app local-first.
- Do not introduce cloud/backend functionality.
- Do not add unnecessary dependencies.
- Do not change the application package.
- Do not unnecessarily rename existing entities/classes.
- Preserve all existing expense functionality.
- Preserve all existing budget functionality.
- Preserve recurring expense functionality.
- Income must be represented as a proper financial transaction concept.
- Do not incorrectly treat income as a negative expense.
- Avoid duplicated financial calculation logic.
- Keep financial calculations deterministic and testable.
- Use the existing date/time abstraction where available.
- Handle dates, months, and year boundaries correctly.
- Handle negative/zero values safely.
- Do not perform Git operations.
- Do not commit, push, reset, checkout, rebase, restore, or modify Git history.

==================================================
PHASE 8 STEPS
==================================================


##################################################
STEP 1 — INCOME DATA FOUNDATION
##################################################

Objective:
Create the database and domain foundation for income transactions.

First inspect the existing architecture from Phases 1–7.

Understand:

- Expense entity
- Expense repository
- Room database
- Categories
- Recurring expenses
- Budget system
- Analytics
- Date/time handling

Design the minimum required income model.

Income should support at minimum:

- Amount
- Description/title
- Income category/source
- Date
- Notes if appropriate
- Created timestamp if the existing architecture uses timestamps

Possible income sources may include:

- Salary
- Freelance
- Business
- Investment
- Gift
- Other

Use the existing architectural conventions.

Create the necessary:

- Room entity
- DAO
- Domain model
- Mapper
- Repository
- Use cases where appropriate

Add a safe Room migration if required.

Income records must persist across app restarts.

Do NOT build the complete income UI yet.

Add focused tests for:

- Create income
- Read income
- Update income
- Delete income
- Persistence
- Database migration

Build and test.

STOP after Step 1.


##################################################
STEP 2 — ADD & EDIT INCOME
##################################################

Objective:
Allow users to manually record income.

Create a clean Compose UI for:

- Add Income
- Edit Income

Fields should include:

- Amount
- Description/title
- Income source/category
- Date
- Notes if supported

Validate:

- Amount must be greater than zero
- Required title/source fields
- Valid date

Use a centralized income-category/source system rather than scattering hardcoded strings throughout the UI.

Keep the UI consistent with Finly's existing Add/Edit Expense experience.

Income should be clearly distinguishable from expenses.

Add tests for:

- Valid income creation
- Invalid amount
- Missing required fields
- Date selection
- Editing existing income
- Persistence

Build and verify on Pixel 9.

STOP after Step 2.


##################################################
STEP 3 — INCOME LIST & MANAGEMENT
##################################################

Objective:
Create an income history and management experience.

Add an Income screen/section where users can:

- View income transactions
- Add income
- Edit income
- Delete income
- Search income
- Filter income where appropriate
- Sort income

Display useful information such as:

- Description
- Source/category
- Date
- Amount

Use the same interaction principles as the existing Expense List.

Support appropriate empty states:

- No income recorded
- No search results
- No matching filter results

Ensure deleting an income transaction does not affect expense transactions.

Ensure editing income updates the existing record rather than creating a duplicate.

Add relevant tests.

Build and verify on Pixel 9.

STOP after Step 3.


##################################################
STEP 4 — FINANCIAL CALCULATIONS
##################################################

Objective:
Create the central financial calculation layer.

Implement reusable calculations for:

- Total income
- Total expenses
- Net balance
- Net cash flow
- Current-month income
- Current-month expenses
- Current-month balance
- Today's income
- Today's expenses
- Today's net cash flow

Core balance calculation:

Balance = Total Income - Total Expenses

Cash flow for a period:

Cash Flow = Income During Period - Expenses During Period

Do NOT duplicate these formulas across multiple ViewModels or screens.

Create a centralized domain/use-case calculation layer.

Calculations must react to:

- New income
- Edited income
- Deleted income
- New expense
- Edited expense
- Deleted expense

Handle:

- No income
- No expenses
- Income only
- Expenses only
- Equal income and expenses
- Income greater than expenses
- Expenses greater than income
- Zero values

Use exact monetary calculations appropriate to the existing application architecture.

Add comprehensive unit tests.

Build and test.

STOP after Step 4.


##################################################
STEP 5 — DASHBOARD BALANCE & CASH FLOW
##################################################

Objective:
Integrate income and financial balance into the Finly Dashboard.

Update the Dashboard to clearly show:

- Total/current-period income
- Total/current-period expenses
- Current balance
- Cash flow

At minimum, the current month should have a clear financial summary.

Also provide today's relevant values where appropriate.

Example conceptual structure:

Income
Expenses
Balance

Avoid cluttering the Dashboard.

The user should immediately understand:

- How much money came in
- How much went out
- What remains/net balance

Ensure the dashboard updates automatically when:

- Income is added
- Income is edited
- Income is deleted
- Expense is added
- Expense is edited
- Expense is deleted

Do not break existing:

- Recent transactions
- Spending metrics
- Dashboard navigation

Keep the existing Finly visual language:

- Minimal
- Clean
- Dark-mode compatible
- Clear typography
- Restrained accent colors
- No unnecessary decorative elements

Add relevant ViewModel/UI tests.

Build and verify on Pixel 9.

STOP after Step 5.


##################################################
STEP 6 — CASH FLOW & HISTORICAL REPORTING
##################################################

Objective:
Provide useful historical income, expense, and cash-flow information.

Extend the analytics architecture from Phase 5.

Support financial reporting for appropriate periods such as:

- Today
- This week
- This month
- Last month
- Recent months
- Historical periods supported by existing analytics

For each period calculate:

- Income
- Expenses
- Net cash flow
- Balance where applicable

Monthly reporting should allow the user to understand financial movement over time.

Where appropriate, provide a visual representation of:

- Income trend
- Expense trend
- Net cash-flow trend

Ensure zero-activity periods are handled correctly.

Do not duplicate analytics infrastructure unnecessarily.

Reuse the existing Phase 5 period-selection and analytics architecture wherever appropriate.

Add tests for:

- Period boundaries
- Current month
- Previous month
- Month/year transitions
- Empty periods
- Income-only periods
- Expense-only periods
- Mixed periods

Build and test.

STOP after Step 6.


##################################################
STEP 7 — FULL FINANCIAL INTEGRATION
##################################################

Objective:
Integrate income into all existing financial systems.

Verify the relationship between income and:

### Dashboard

- Income
- Expenses
- Balance
- Cash flow

### Expenses

Existing expense functionality remains unchanged.

### Analytics

Income and expenses should coexist correctly in financial reporting.

### Budgets

IMPORTANT:

Budgets remain spending limits.

Income must NOT incorrectly increase the user's spending budget.

For example:

Income = ₹50,000
Budget = ₹20,000
Expenses = ₹10,000

Budget remaining should be based on:

Budget limit - eligible spending

not:

Income - spending

### Recurring Expenses

Existing recurring expenses remain expenses.

Generated recurring expenses must reduce financial balance and cash flow as normal expenses.

### Categories

Income categories/sources must not corrupt the existing expense category system.

If the architecture can safely support shared category infrastructure, reuse it appropriately without mixing incompatible semantics.

### Balance

Verify:

Balance = Income - Expenses

### Editing/Deleting

Changing or deleting an income transaction must immediately update relevant:

- Dashboard
- Analytics
- Balance
- Cash flow

Perform integration scenarios:

TEST A:
Add income → verify balance increases.

TEST B:
Add expense → verify balance decreases.

TEST C:
Add income + expense → verify net balance.

TEST D:
Edit income → verify balance updates.

TEST E:
Delete income → verify balance updates.

TEST F:
Create recurring expense → generate it → verify balance decreases.

TEST G:
Create budget → add income → verify budget limit does not incorrectly change.

TEST H:
Add income and expenses in different months → verify each month's calculations remain isolated.

TEST I:
Add multiple income transactions → verify total income.

TEST J:
Add income with analytics → verify historical financial reporting.

Build and verify.

STOP after Step 7.


##################################################
STEP 8 — FULL PHASE 8 TESTING & VERIFICATION
##################################################

Objective:
Perform complete Phase 8 verification and regression testing.

Run the complete project test suite.

Verify all previous phases:

PHASE 1:
- Database
- Persistence
- Migration

PHASE 2:
- Add expense
- Edit expense
- Delete expense
- Search
- Filter
- Sort

PHASE 3:
- Dashboard
- Monthly spending
- Today's spending
- Recent transactions

PHASE 4:
- Categories
- Custom categories
- Category management
- Category filtering

PHASE 5:
- Analytics
- Category breakdown
- Monthly trends
- Daily/weekly spending
- Period selection
- Historical comparisons

PHASE 6:
- Monthly budgets
- Category budgets
- Progress
- Remaining amounts
- Overspending warnings

PHASE 7:
- Recurring expenses
- Scheduling
- Automatic generation
- Duplicate prevention
- Pause/resume
- Generated transaction integration

PHASE 8:
- Income creation
- Income editing
- Income deletion
- Income history
- Income categories/sources
- Total income
- Total expenses
- Balance
- Cash flow
- Historical financial reporting
- Dashboard integration

Perform complete regression scenarios.

TEST 1:
Fresh database → add income → verify persistence.

TEST 2:
Add income → restart app → verify income remains.

TEST 3:
Add income + expense → verify balance.

TEST 4:
Edit income → verify balance updates.

TEST 5:
Delete income → verify balance updates.

TEST 6:
Add multiple income transactions → verify total.

TEST 7:
Add income and expenses across different months → verify period isolation.

TEST 8:
Add income → verify budget does not incorrectly increase.

TEST 9:
Generate recurring expense → verify balance decreases.

TEST 10:
Edit/delete recurring-generated expense → verify balance updates.

TEST 11:
Verify analytics correctly report income, expenses, and cash flow.

TEST 12:
Verify dashboard values match the underlying database.

TEST 13:
Restart the application → verify all financial data persists.

TEST 14:
Test zero-income and zero-expense states.

TEST 15:
Test income greater than expenses.

TEST 16:
Test expenses greater than income.

TEST 17:
Test income exactly equal to expenses.

TEST 18:
Test month-end/year-end date boundaries.

Verify database migration from the existing project state.

Run the full test suite.

Build the APK successfully.

Install and run on the Pixel 9 emulator.

Check for:

- Crashes
- Incorrect balance calculations
- Incorrect cash-flow calculations
- Duplicate income records
- Incorrect date boundaries
- Incorrect monthly totals
- Incorrect dashboard values
- Incorrect analytics
- Budget calculation regressions
- Recurring expense regressions
- Broken navigation
- Broken existing expense functionality
- Persistence problems
- Dark-mode UI issues
- Layout problems

Only mark Phase 8 COMPLETE when all required tests and verification succeed.

Update:

- phases/PHASE_08_INCOME_AND_BALANCE.md
- CHANGELOG.md
- DEVELOPMENT_PLAN.md

Mark Phase 8 as COMPLETE only after successful verification.

Do NOT perform Git operations.

STOP and provide the final Phase 8 verification report.

==================================================
FINAL EXECUTION RULE
==================================================

ONE STEP ONLY PER EXECUTION.

Never automatically proceed from Step N to Step N+1.

After each step:

IMPLEMENT
→ TEST
→ BUILD
→ VERIFY
→ REPORT
→ STOP

Wait for my explicit instruction before continuing.




PHASE 9 

# FINLY — PHASE 9 MASTER IMPLEMENTATION PROMPT
# Data Management
# Execute Step 1 → Stop → Wait → Step 2 → Stop → ... → Step 8

You are working on the Finly Personal Expense Tracker Android application.

PHASE 9:
Data Management

OBJECTIVE:
Add data export, import, backup, and restore functionality so users maintain control over their financial data.

WHAT WE BUILD:
- CSV export
- JSON export
- CSV import
- JSON import
- Local encrypted backup
- Database backup
- Database restore
- Import validation
- Backup/restore safety
- Data integrity protection

EXPECTED RESULT:
Users can safely move, backup, and restore their financial information without losing or corrupting data.

==================================================
IMPORTANT EXECUTION RULE
==================================================

This is a MASTER PROMPT containing all Phase 9 steps.

DO NOT implement the entire phase at once.

Execute ONLY ONE STEP at a time.

After completing the current step:

1. Implement only that step.
2. Run relevant tests.
3. Build the Android application.
4. Fix issues caused by the current step.
5. Verify the result.
6. Update relevant project documentation if necessary.
7. Report exactly what was implemented and verified.
8. STOP.

Do NOT continue automatically to the next step.

Wait for my explicit instruction before starting the next step.

==================================================
PROJECT SAFETY RULES
==================================================

- Preserve the existing Finly architecture.
- Do not rewrite working Phase 1–8 functionality.
- Reuse the existing Room/database architecture.
- Preserve existing expense, category, budget, recurring expense, income, dashboard, and analytics functionality.
- Keep the app local-first.
- Do not introduce cloud/backend functionality.
- Do not add unnecessary dependencies.
- Do not change the application package.
- Do not unnecessarily rename existing entities/classes.
- Do not delete existing financial data.
- Do not silently overwrite user data.
- Treat import/export as user-controlled operations.
- Validate imported data before modifying the database.
- Prevent malformed data from corrupting the database.
- Use transactional database operations where appropriate.
- Prefer atomic operations for restore/import.
- Handle errors gracefully.
- Keep file operations secure.
- Do not expose sensitive financial data unnecessarily.
- Follow existing Kotlin and Jetpack Compose conventions.
- Maintain dark-mode compatibility.
- Do NOT perform any Git operations.

==================================================
PHASE 9 STEPS
==================================================


##################################################
STEP 1 — DATA MANAGEMENT FOUNDATION
##################################################

Objective:
Create the architecture required for data export, import, backup, and restore.

First inspect the existing project architecture from Phases 1–8.

Understand:

- Expense database
- Income database
- Categories
- Budgets
- Recurring expenses
- Room database version
- DAOs
- Repositories
- Domain models
- Mappers
- Existing date/time handling
- Existing application storage

Design a clean Data Management layer.

Create appropriate abstractions for:

- Export
- Import
- Backup
- Restore

Do not implement the complete UI yet.

Ensure the architecture can safely support all current financial data.

The design must account for:

- Expenses
- Income
- Categories
- Budgets
- Recurring expenses
- Any other persistent user-owned financial data already present

Add focused unit tests for the new data-management abstractions.

Build and test.

STOP after Step 1.


##################################################
STEP 2 — CSV EXPORT
##################################################

Objective:
Allow users to export their financial data in CSV format.

Implement CSV export for the user's financial records.

At minimum support:

- Expenses
- Income

Include appropriate fields such as:

Expenses:
- ID if appropriate
- Amount
- Description
- Category
- Date
- Notes
- Other existing relevant fields

Income:
- ID if appropriate
- Amount
- Description
- Source/category
- Date
- Notes
- Other existing relevant fields

Use a stable CSV format.

Handle:

- Empty datasets
- Special characters
- Commas inside descriptions
- Quotes
- Newlines
- Unicode text
- Decimal amounts
- Dates

Do not manually concatenate CSV strings in an unsafe way.

Use Android's file-sharing/storage mechanisms appropriately.

Allow the user to choose where/how to save or share the exported file using standard Android mechanisms.

Add tests for:

- Normal export
- Empty export
- Special characters
- Multiple records
- Unicode text
- Correct headers
- Correct values

Build and verify on Pixel 9.

STOP after Step 2.


##################################################
STEP 3 — JSON EXPORT
##################################################

Objective:
Add structured JSON export.

Implement JSON export containing the user's financial data.

The JSON structure must be:

- Stable
- Explicit
- Versionable
- Easy to validate during future imports

Include appropriate metadata such as:

- Export format version
- Export timestamp if appropriate
- App/data schema version where useful

Include all important persistent financial information supported by the application.

At minimum consider:

- Expenses
- Income
- Categories
- Budgets
- Recurring expenses

Do not expose internal database implementation details unnecessarily.

Ensure the exported JSON can later be imported by Finly.

Handle:

- Empty data
- Unicode
- Decimal values
- Dates
- Optional fields
- Missing optional values

Add serialization/deserialization tests.

Verify that:

EXPORT → PARSE → RECONSTRUCT

produces equivalent financial data.

Build and verify.

STOP after Step 3.


##################################################
STEP 4 — CSV & JSON IMPORT
##################################################

Objective:
Allow users to import financial data from CSV and JSON files.

Implement safe import functionality.

Support:

- CSV import
- JSON import

The import process must:

1. Select a file.
2. Detect/validate its format.
3. Parse the contents.
4. Validate every record.
5. Detect malformed records.
6. Detect invalid amounts/dates/required fields.
7. Prevent duplicate records where appropriate.
8. Present an import summary.
9. Ask for user confirmation before committing data.
10. Insert data safely using a database transaction.

Import must NOT partially corrupt the database if an error occurs.

Handle malformed files gracefully.

Examples:

- Invalid amount
- Missing required field
- Invalid date
- Unknown category
- Duplicate record
- Invalid JSON
- Invalid CSV structure
- Empty file
- Unsupported file format

Provide useful error messages.

Do not silently discard invalid records.

If partial import is supported, clearly report:

- Imported records
- Skipped records
- Failed records
- Reasons

Otherwise use all-or-nothing transactional import.

Add comprehensive tests.

Build and verify on Pixel 9.

STOP after Step 4.


##################################################
STEP 5 — LOCAL BACKUP SYSTEM
##################################################

Objective:
Create a reliable local backup mechanism.

Implement local backup creation containing all required Finly user data.

The backup should preserve:

- Expenses
- Income
- Categories
- Budgets
- Recurring expenses
- Relevant preferences if appropriate
- Required metadata/schema information

Use a versioned backup format.

Backup files should be distinguishable from ordinary exports.

Implement backup creation through Android's standard file/storage APIs.

The user should be able to choose a backup destination where supported.

Handle:

- Empty database
- Large datasets
- File creation failure
- Storage errors
- Interrupted operations

Add backup integrity checks where appropriate.

Do not overwrite an existing backup unexpectedly.

Add tests for backup creation and data completeness.

Build and verify on Pixel 9.

STOP after Step 5.


##################################################
STEP 6 — ENCRYPTED BACKUP & RESTORE
##################################################

Objective:
Add secure local backup and restoration.

Implement encrypted backup protection using Android-supported secure cryptographic mechanisms.

Do NOT invent cryptographic algorithms.

Use established Android/Java cryptography APIs.

The backup should not expose financial data as plain text when encryption is enabled.

Design an appropriate user-controlled protection mechanism.

Do not store encryption secrets insecurely.

Implement restore functionality.

Restore must:

1. Select backup.
2. Validate backup format.
3. Verify integrity.
4. Decrypt if encrypted.
5. Validate schema/version.
6. Validate contained data.
7. Show restore information/summary.
8. Require explicit user confirmation.
9. Safely replace or restore existing data.
10. Complete atomically.

IMPORTANT:

Never destroy the existing database before the replacement backup has been validated successfully.

If restore fails:

- Existing data must remain intact.
- Show a clear error.
- Do not leave the database in a partially restored state.

Handle:

- Wrong password/key
- Corrupted backup
- Unsupported backup version
- Invalid data
- Missing fields
- Database errors
- Interrupted restore

Add tests for:

- Encrypt/decrypt round trip
- Correct password/key
- Incorrect password/key
- Corrupted backup
- Valid restore
- Failed restore
- Database integrity

Build and verify carefully.

STOP after Step 6.


##################################################
STEP 7 — DATA MANAGEMENT UI & USER SAFETY
##################################################

Objective:
Create a clean user-facing Data Management section.

Add a settings/data-management screen containing actions such as:

- Export CSV
- Export JSON
- Import CSV
- Import JSON
- Create Backup
- Restore Backup

Use Finly's existing design language:

- Minimal
- Clean
- Dark-mode compatible
- Clear typography
- Restrained accent colors
- No unnecessary decorative elements

Before destructive operations such as restore, provide clear confirmation.

Clearly explain what will happen.

Show appropriate states:

- Processing
- Success
- Failure
- Empty data
- File unavailable
- Invalid file
- Backup created
- Restore completed

For imports, show useful summaries.

Example:

Imported:
25 expenses
4 income records

Skipped:
2 records

For restore, clearly warn that existing data may be replaced or merged depending on the implemented restore model.

Do not make destructive behavior ambiguous.

Ensure navigation works correctly.

Add relevant UI/ViewModel tests.

Build and verify on Pixel 9.

STOP after Step 7.


##################################################
STEP 8 — FULL PHASE 9 TESTING & VERIFICATION
##################################################

Objective:
Perform complete Phase 9 verification and regression testing.

Run the complete project test suite.

Verify all previous phases:

PHASE 1:
- Room database
- Persistence
- Migrations

PHASE 2:
- Add expense
- Edit expense
- Delete expense
- Search
- Filter
- Sort

PHASE 3:
- Dashboard
- Spending totals
- Recent transactions

PHASE 4:
- Categories
- Custom categories
- Category management

PHASE 5:
- Analytics
- Trends
- Comparisons

PHASE 6:
- Monthly budgets
- Category budgets
- Progress
- Overspending warnings

PHASE 7:
- Recurring expenses
- Scheduling
- Automatic generation
- Duplicate prevention

PHASE 8:
- Income
- Balance
- Cash flow
- Financial reporting

PHASE 9:
- CSV export
- JSON export
- CSV import
- JSON import
- Backup
- Encryption
- Restore
- Data validation
- Data integrity

Perform complete data round-trip tests.

TEST 1:
Create expenses and income.

Export to CSV.

Verify exported data.

TEST 2:
Create financial data.

Export to JSON.

Import the JSON into a controlled test database.

Verify equivalent data.

TEST 3:
Create data.

Create backup.

Clear/use a controlled database.

Restore backup.

Verify all expected records.

TEST 4:
Create encrypted backup.

Restore with correct credentials.

Verify successful restoration.

TEST 5:
Attempt restore with incorrect credentials.

Verify existing data remains untouched.

TEST 6:
Corrupt backup.

Attempt restore.

Verify failure is handled safely and existing data remains intact.

TEST 7:
Import malformed CSV.

Verify invalid records are rejected safely.

TEST 8:
Import malformed JSON.

Verify import fails without database corruption.

TEST 9:
Test duplicate imports.

Verify duplicate prevention behavior.

TEST 10:
Test special characters and Unicode.

TEST 11:
Test empty database export.

TEST 12:
Test large datasets.

TEST 13:
Test backup/restore across app restart.

TEST 14:
Test month/year/date preservation.

TEST 15:
Verify categories, budgets, recurring expenses, income, and expenses remain logically consistent after restore.

TEST 16:
Verify dashboard after restore.

TEST 17:
Verify analytics after restore.

TEST 18:
Verify budgets after restore.

TEST 19:
Verify recurring expenses after restore.

TEST 20:
Verify income/balance after restore.

TEST 21:
Verify no existing Phase 1–8 functionality regressed.

Also verify:

- No crashes
- No data loss
- No partial imports
- No partial restores
- No duplicate records
- Correct date handling
- Correct monetary values
- Correct Unicode handling
- Correct file handling
- Correct encryption/decryption
- Correct error handling
- Correct navigation
- Dark-mode UI consistency
- Pixel 9 compatibility

Run the complete test suite.

Build the APK successfully.

Install and run on the Pixel 9 emulator.

Only mark Phase 9 COMPLETE when all required tests and verification succeed.

Update:

- phases/PHASE_09_DATA_MANAGEMENT.md
- CHANGELOG.md
- DEVELOPMENT_PLAN.md

Mark Phase 9 as COMPLETE only after successful verification.

Do NOT perform any Git operations.

STOP and provide the final Phase 9 verification report.

==================================================
FINAL EXECUTION RULE
==================================================

ONE STEP ONLY PER EXECUTION.

Never automatically proceed from Step N to Step N+1.

After each step:

IMPLEMENT
→ TEST
→ BUILD
→ VERIFY
→ REPORT
→ STOP

Wait for my explicit instruction before continuing.



PHASE 10


# FINLY — PHASE 10 MASTER EXECUTION PROMPT
# Personalization

You are continuing development of the Finly Personal Expense Tracker Android application.

Read and follow:
- PROJECT_CONTEXT.md
- DEVELOPMENT_PLAN.md
- phases/PHASE_10_PERSONALIZATION.md
- Existing project architecture and implementation

==================================================
PHASE 10 — PERSONALIZATION
==================================================

OBJECTIVE

Add user preferences such as:
- Currency
- Theme
- Dark mode
- Date preferences
- Default settings
- Customizable application behavior

The implementation must integrate cleanly with the existing Finly application.

==================================================
CRITICAL EXECUTION RULE
==================================================

Execute ONLY ONE STEP per execution.

NEVER automatically continue from Step N to Step N+1.

After completing the current step:

IMPLEMENT
→ TEST
→ BUILD
→ VERIFY
→ REPORT
→ STOP

Wait for my explicit instruction before starting the next step.

==================================================
GIT SAFETY — ABSOLUTE RULE
==================================================

DO NOT perform ANY Git operation.

DO NOT run:

git status
git add
git commit
git push
git pull
git fetch
git reset
git checkout
git restore
git merge
git rebase
git clean
git update-index

DO NOT modify, repair, recreate, delete, or inspect anything inside `.git/`.

DO NOT use Git to verify your work.

DO NOT attempt to fix Git-related problems.

Git is completely outside the scope of this task.

Focus ONLY on application development, testing, building, and verification.

==================================================
ARCHITECTURE SAFETY
==================================================

Before implementing anything, inspect the existing architecture.

Reuse existing:
- Kotlin architecture
- Jetpack Compose
- Material 3
- Room
- Repository/use-case patterns
- Existing ViewModels
- Existing navigation
- Existing theme system
- Existing preference infrastructure, if present

Do not unnecessarily rewrite working code.

Do not introduce a backend, authentication, cloud synchronization, or unrelated dependencies.

Keep Finly local-first and functional offline.

Do not break existing:
- Expenses
- Categories
- Dashboard
- Analytics
- Budgets
- Recurring Expenses
- Income
- Balance
- Data Management

Preserve existing database data.

If a Room migration is genuinely required, implement it safely and test it.

==================================================
STEP 1 — PERSONALIZATION FOUNDATION
==================================================

Inspect the current project and determine how application preferences should be stored.

Create a clean preference layer for persistent user settings.

Support the necessary preference values for:
- Currency
- Theme mode
- Date format/preferences
- Default application settings

Prefer the simplest architecture consistent with the existing project.

Preferences must survive:
- Screen changes
- App restart
- Process recreation

Do not redesign the entire application.

TEST:
- Preference persistence
- Default values
- Reading and writing preferences
- Restart persistence

BUILD the application.

VERIFY on the Pixel 9 emulator if possible.

REPORT:
- Files created/changed
- Architecture used
- Tests passed
- Build result
- Verification result

STOP.

==================================================
STEP 2 — CURRENCY PREFERENCE
==================================================

Implement currency selection and persistence.

Provide a clean currency preference UI.

At minimum support the currencies already appropriate for the application's intended use, while keeping the design extensible.

The selected currency must consistently affect financial amount display throughout Finly.

Ensure:
- Dashboard
- Expenses
- Budgets
- Analytics
- Income
- Balance
- Recurring Expenses
- Other financial displays

use the selected currency formatting where applicable.

Do not perform currency conversion unless explicitly required by the existing specification.

TEST:
- Default currency
- Changing currency
- Persistence
- Correct symbol/format display
- Existing financial values remain numerically unchanged

BUILD.

VERIFY on Pixel 9.

REPORT.

STOP.

==================================================
STEP 3 — THEME AND DARK MODE
==================================================

Implement the personalization theme settings.

Support:
- Light
- Dark
- System default

Use the existing Finly Material 3 theme architecture.

The application must react immediately when the user changes the theme.

Ensure all existing screens remain visually usable:
- Dashboard
- Expenses
- Add Expense
- Edit Expense
- Categories
- Analytics
- Budgets
- Recurring Expenses
- Income
- Settings

Avoid introducing unnecessary colors, gradients, decorative elements, or unrelated redesigns.

Keep the existing Finly visual language.

TEST:
- Light mode
- Dark mode
- System mode
- Persistence after restart
- Navigation across screens while changing themes

BUILD.

VERIFY on Pixel 9.

REPORT.

STOP.

==================================================
STEP 4 — DATE AND REGIONAL FORMATTING
==================================================

Implement user-configurable date formatting/preferences supported by the project requirements.

Ensure dates are displayed consistently throughout the application.

Check:
- Expense dates
- Recurring expense dates
- Analytics periods
- Dashboard dates
- Income dates
- Budget-related dates
- Any other user-visible dates

Do not alter the underlying stored date representation unnecessarily.

Separate storage format from display formatting.

TEST:
- Default date format
- Changing preference
- Persistence
- Existing dates remain correct
- Different screens display dates consistently

BUILD.

VERIFY.

REPORT.

STOP.

==================================================
STEP 5 — DEFAULT APPLICATION SETTINGS
==================================================

Add useful default preferences that fit the existing Finly application.

Examples may include:
- Default currency
- Default theme
- Default date preference
- Default category where appropriate
- Default sorting/filter behavior where appropriate

Only implement settings that are genuinely useful and compatible with the current architecture.

Do not invent unnecessary settings just to increase feature count.

Ensure defaults are applied correctly without overriding explicit user choices.

TEST:
- Fresh/default state
- User changes
- Persistence
- App restart
- Existing workflows

BUILD.

VERIFY.

REPORT.

STOP.

==================================================
STEP 6 — SETTINGS UI
==================================================

Create or integrate a dedicated Settings/Personalization screen.

The UI should allow the user to easily manage:

- Currency
- Theme
- Date preferences
- Default settings
- Other appropriate personalization options

Use the existing Finly UI style.

Requirements:
- Clean
- Minimal
- Dark-mode compatible
- Easy to understand
- No unnecessary decoration
- Clear current values
- Proper navigation
- Immediate feedback when settings change

Do not redesign unrelated screens.

TEST the complete settings workflow.

BUILD.

VERIFY on Pixel 9.

REPORT.

STOP.

==================================================
STEP 7 — FULL APPLICATION INTEGRATION
==================================================

Verify that personalization settings actually propagate through the entire application.

Test combinations such as:

1. Change currency → verify financial screens.
2. Change theme → verify every major screen.
3. Change date preference → verify date displays.
4. Restart app → verify all settings persist.
5. Add expense → verify formatting.
6. Edit expense → verify formatting.
7. Add income → verify formatting.
8. Create budget → verify formatting.
9. Create recurring expense → verify formatting.
10. Open analytics → verify formatting.
11. Return to dashboard → verify consistency.

Ensure personalization does not break existing functionality.

Fix only issues caused by Phase 10 integration.

TEST.

BUILD.

VERIFY on Pixel 9.

REPORT.

STOP.

==================================================
STEP 8 — FULL PHASE 10 TESTING AND VERIFICATION
==================================================

Perform the final Phase 10 verification.

Run the complete available test suite.

Verify:
- Unit tests
- ViewModel tests
- Preference tests
- UI-related tests where available
- Room/database tests if affected
- Build
- App installation
- App launch
- Settings persistence
- Currency behavior
- Theme behavior
- Date formatting
- Existing Phase 1–9 functionality

Perform regression testing for:
- Expenses
- Categories
- Dashboard
- Analytics
- Budgets
- Recurring Expenses
- Income
- Balance
- Data Management

If something fails:
- Diagnose the actual cause.
- Fix only what is necessary.
- Re-run the relevant tests.
- Re-run the build.
- Verify again.

Do NOT mark Phase 10 complete unless verification succeeds.

FINAL REPORT MUST INCLUDE:
- Phase 10 status
- Steps completed
- Files changed
- Tests executed and results
- Build result
- Pixel 9 verification result
- Any remaining limitations

==================================================
FINAL EXECUTION RULE
==================================================

ONE STEP ONLY PER EXECUTION.

Never automatically continue to the next step.

After each step:

IMPLEMENT
→ TEST
→ BUILD
→ VERIFY
→ REPORT
→ STOP

Wait for my explicit instruction.

DO NOT perform ANY Git operation.

DO NOT modify `.git/`.

DO NOT commit.

DO NOT push.

DO NOT attempt Git repair.

Git is completely outside this task.

START WITH STEP 1 ONLY.

# FINLY — AUTOMATIC EXPENSE CAPTURE UPDATE
# Notification-Based Automatic Expense Detection & Creation

You are continuing development of the Finly Personal Expense Tracker Android application.

THIS IS A CRITICAL PRODUCT UPDATE.

Finly is intended to be an AUTOMATIC expense tracker.

Manual expense entry must remain available as a fallback, but the primary expense-ingestion workflow should be automatic:

BANK / UPI / CARD / WALLET NOTIFICATION
        ↓
ANDROID NOTIFICATION LISTENER
        ↓
FINLY RECEIVES NOTIFICATION
        ↓
FINANCIAL TRANSACTION DETECTION
        ↓
DEBIT / CREDIT CLASSIFICATION
        ↓
DEBIT ONLY
        ↓
TRANSACTION PARSING
        ↓
MERCHANT + AMOUNT + TIME + REFERENCE
        ↓
CATEGORY DETECTION
        ↓
DUPLICATE CHECK
        ↓
AUTOMATIC ROOM EXPENSE CREATION
        ↓
EXISTING DASHBOARD / ANALYTICS / BUDGETS UPDATE

==================================================
IMPORTANT PRODUCT REQUIREMENT
==================================================

Do NOT treat Finly as a conventional manual-entry expense tracker.

The user should NOT normally have to manually enter an expense when a valid transaction notification is received.

Example:

Notification:

"₹450 debited from A/c XXXX1234 for Swiggy"

Finly should automatically create:

Amount:
₹450

Merchant:
Swiggy

Type:
Expense

Category:
Food

Date/time:
Notification transaction time

No manual input should be required.

Another example:

"₹1200 paid to Amazon using UPI"

Should be interpreted as a potential debit transaction and processed according to the parser/classifier.

But:

"₹50,000 credited to your account"

MUST NOT automatically become an expense.

Credits are NOT automatic expenses.

==================================================
CRITICAL EXECUTION RULE
==================================================

Execute the implementation ONE STEP AT A TIME.

Never automatically continue from Step N to Step N+1.

For every step:

IMPLEMENT
→ TEST
→ BUILD
→ VERIFY
→ REPORT
→ STOP

Wait for my explicit instruction before continuing.

Do not combine multiple implementation steps into one uncontrolled batch.

Keep each step small and verifiable.

==================================================
GIT SAFETY — ABSOLUTE RULE
==================================================

DO NOT perform ANY Git operation.

DO NOT run:

git status
git add
git commit
git push
git pull
git fetch
git reset
git checkout
git restore
git merge
git rebase
git clean
git update-index

DO NOT modify, repair, recreate, delete, or inspect anything inside `.git/`.

DO NOT use Git to verify your work.

DO NOT attempt to repair Git.

Git is completely outside the scope of this task.

There must be ZERO Git operations during this update.

==================================================
ARCHITECTURE SAFETY
==================================================

Before modifying code, inspect the existing Finly architecture.

Reuse the existing:

- Kotlin
- Jetpack Compose
- Material 3
- Room database
- ExpenseEntity
- Expense model
- Expense repository
- Existing expense use cases
- Existing category system
- Existing ViewModels
- Existing Dashboard
- Existing Analytics
- Existing Budget system
- Existing Recurring Expense system
- Existing Income/Balance system
- Existing Data Management
- Existing theme/settings architecture

Do NOT rewrite working features.

Do NOT create a second independent expense database.

Do NOT create a second independent category system.

Automatic notification expenses MUST eventually enter the SAME existing expense data layer used by manually created expenses.

Do NOT introduce cloud services.

Do NOT send notification contents to a remote server.

Notification processing must happen locally on the device by default.

Do NOT add unnecessary dependencies.

Do NOT change the application package name.

Do NOT break existing Room data.

If a Room migration is required, implement it safely and test it.

==================================================
FEATURE ARCHITECTURE
==================================================

Implement the automatic capture system as a proper pipeline.

Recommended conceptual architecture:

NotificationListenerService
        ↓
Notification Filter
        ↓
Transaction Detector
        ↓
Debit/Credit Classifier
        ↓
Transaction Parser
        ↓
Parsed Transaction Model
        ↓
Category Resolver
        ↓
Duplicate Detector
        ↓
Create Expense Use Case
        ↓
Existing Expense Repository
        ↓
Room
        ↓
Existing Finly UI

Keep these responsibilities separated.

Do not put the entire parser inside a Compose screen or Activity.

==================================================
STEP 1 — NOTIFICATION LISTENER FOUNDATION
==================================================

Create the Android NotificationListenerService foundation.

The service must be capable of receiving posted notifications after the user explicitly grants Android Notification Access permission.

Do NOT silently request or bypass Android permission.

The application should clearly explain that notification access is required for automatic expense capture.

Add the necessary Android manifest/service configuration.

The service should safely receive notification events.

At this stage:

- Receive notification
- Read notification package/source
- Read notification title/text where permitted
- Pass notification data into the local processing pipeline
- Do NOT create an expense yet unless the rest of the pipeline is implemented

Do not process every phone notification as a financial transaction.

Create clean internal models/interfaces for notification input.

TEST:
- Service registration
- Service lifecycle
- Permission-disabled behavior
- Permission-enabled behavior
- Receiving a test notification
- No crash from malformed/empty notification data

BUILD.

VERIFY.

REPORT.

STOP.

==================================================
STEP 2 — FINANCIAL NOTIFICATION DETECTION
==================================================

Implement transaction notification detection.

Finly must distinguish likely financial transaction notifications from unrelated notifications.

Do not simply process every notification.

Create a robust local detection layer.

Potential transaction indicators include concepts such as:

- debited
- debit
- spent
- paid
- payment
- purchase
- transaction
- withdrawn
- UPI payment
- card payment
- payment successful
- amount deducted
- amount paid

But do NOT rely on one exact phrase.

Different banks, UPI applications, cards, and wallets can use different wording.

The detector should be designed to support multiple formats.

Also recognize transaction-status concepts such as:

- successful
- completed
- processed
- paid

while avoiding obvious non-transaction notifications.

Do not create expenses at this stage unless classification/parsing succeeds.

TEST:
- Financial transaction notification
- Non-financial notification
- Empty notification
- Promotional notification
- Bank notification
- UPI notification
- Card notification
- Different wording variations

BUILD.

VERIFY.

REPORT.

STOP.

==================================================
STEP 3 — DEBIT VS CREDIT CLASSIFICATION
==================================================

Implement explicit transaction direction classification.

This is one of the most important requirements.

Every detected financial notification must be classified as one of:

DEBIT
CREDIT
UNKNOWN

Only DEBIT transactions may enter the automatic expense creation pipeline.

Examples of CREDIT indicators:

- credited
- credit
- received
- deposit
- money received
- salary credited
- refund received
- cash deposited
- amount credited

These MUST NOT automatically become expenses.

Examples of DEBIT indicators:

- debited
- debit
- spent
- paid
- purchase
- amount deducted
- withdrawn
- payment made
- UPI payment
- card purchase

When conflicting indicators exist, classify as UNKNOWN rather than guessing.

UNKNOWN transactions must NOT automatically create expenses.

Do not accidentally classify:

"₹500 credited"

as:

Expense ₹500.

It must be ignored by the automatic expense pipeline.

TEST:
- Clear debit
- Clear credit
- Ambiguous transaction
- Refund/credit
- Salary/credit
- Debit purchase
- UPI debit
- Card debit

BUILD.

VERIFY.

REPORT.

STOP.

==================================================
STEP 4 — TRANSACTION PARSER
==================================================

Implement a dedicated local transaction parser.

The parser must attempt to extract:

- Amount
- Merchant/payee
- Transaction date/time
- Source/package
- Transaction/reference ID when available
- Original notification text internally where needed for processing

Create a structured ParsedTransaction model.

Example input:

"₹450 debited from A/c XXXX1234 for Swiggy"

Expected parsing:

amount = 450
merchant = Swiggy
direction = DEBIT

Another:

"Your UPI payment of Rs. 250 to Zomato was successful"

Expected:

amount = 250
merchant = Zomato
direction = DEBIT

The parser must support common amount formats such as:

₹450
Rs 450
Rs. 450
INR 450
₹1,250.50
1,250 INR

Do not assume every notification uses the rupee symbol.

However, do not blindly parse arbitrary numbers.

For example:

"Your OTP is 482913"

must NOT become:

Expense ₹482913.

The parser should require sufficient evidence that the number represents a transaction amount.

Merchant extraction should handle common wording such as:

for Swiggy
to Zomato
at Amazon
merchant: Uber
paid to ABC
purchase at XYZ

Do not require one exact sentence structure.

If amount cannot be confidently extracted:

DO NOT CREATE AN EXPENSE.

If merchant cannot be confidently extracted, use a safe fallback only if the transaction itself is otherwise confidently identified.

Preserve enough metadata internally for debugging and duplicate detection without unnecessarily storing sensitive notification contents.

TEST extensively with multiple notification formats.

BUILD.

VERIFY.

REPORT.

STOP.

==================================================
STEP 5 — TRANSACTION SOURCE AND FORMAT HANDLING
==================================================

Improve source-aware handling.

Financial notifications may originate from:

- Bank applications
- UPI applications
- Card applications
- Wallet applications
- Payment applications

Do not hardcode one bank's notification format as the entire solution.

Create an extensible structure where source-specific parsing rules can be added later.

The system should identify the originating package/application when available.

Use source information as a parsing signal, not as the sole requirement.

Do not create a giant collection of fragile hardcoded rules.

Keep parsing modular.

TEST:
- Multiple source package names
- Unknown source
- Known financial source
- Non-financial source
- Same transaction wording from different sources

BUILD.

VERIFY.

REPORT.

STOP.

==================================================
STEP 6 — AUTOMATIC CATEGORY RESOLUTION
==================================================

Connect parsed transactions to the EXISTING Finly category system.

Do NOT create a second category implementation.

Use the category architecture already established in the application.

Resolve merchants into appropriate categories.

Examples:

Swiggy → Food
Zomato → Food
Uber → Transportation
Ola → Transportation
Amazon → Shopping
Netflix → Entertainment
Apollo → Health
Pharmacy → Health
Electricity → Bills
Mobile recharge → Bills

These are examples, not an exhaustive hardcoded list.

Category resolution should support:

1. Known merchant mapping
2. Existing category information
3. Reasonable keyword matching
4. Safe fallback to "Other" when confidence is insufficient

Do NOT falsely claim high-confidence categorization when uncertain.

The resulting category MUST be compatible with the existing category filter, analytics, budgets, and dashboard.

TEST:
- Known merchants
- Unknown merchants
- Category fallback
- Existing custom categories if supported
- Category consistency with manually created expenses

BUILD.

VERIFY.

REPORT.

STOP.

==================================================
STEP 7 — DUPLICATE TRANSACTION PROTECTION
==================================================

Implement strong duplicate protection.

This is mandatory.

The same transaction must never be inserted multiple times simply because:

- Notification is repeated
- Notification is updated
- App receives the same notification again
- Bank sends multiple related notification events
- Service restarts
- Notification is delivered more than once

Prefer a real transaction/reference ID when available.

If no reference ID exists, implement a safe fallback using appropriate transaction attributes such as:

- Amount
- Merchant
- Direction
- Transaction time
- Source

Use a reasonable time tolerance rather than requiring an exact timestamp.

Do NOT make duplicate detection so aggressive that two legitimate separate purchases are incorrectly treated as one.

The duplicate-check logic must be testable independently.

TEST:
- Same notification twice
- Same transaction with changed notification text
- Same amount/merchant at different times
- Different merchants with same amount
- Different transactions close together
- Reference-ID based duplicates

BUILD.

VERIFY.

REPORT.

STOP.

==================================================
STEP 8 — AUTOMATIC EXPENSE CREATION
==================================================

Connect the successful parsed DEBIT transaction to the existing expense creation architecture.

Only create an expense when ALL required conditions are satisfied:

1. Notification is financial
2. Transaction is confidently classified as DEBIT
3. Amount is valid
4. Transaction is successfully parsed
5. Transaction is not a duplicate
6. Category can be resolved safely

Then create the expense through the existing application data layer.

Do NOT directly manipulate Room from the notification service if the existing architecture provides repository/use-case abstractions.

Reuse the existing expense creation path wherever appropriate.

The automatically created expense must appear in:

- Expense list
- Dashboard
- Current-month spending
- Today's spending
- Analytics
- Category totals
- Budget progress
- Search/filter/sort
- Other existing expense-related features

The automatic expense should behave exactly like an expense created through the application's normal expense data layer.

TEST:
- Valid debit notification → expense created
- Credit notification → ignored
- Invalid amount → ignored
- Duplicate → ignored
- Valid debit + category → stored
- Dashboard updates
- Analytics updates
- Budget updates

BUILD.

VERIFY.

REPORT.

STOP.

==================================================
STEP 9 — BACKGROUND BEHAVIOR
==================================================

Verify that automatic capture works while the Finly UI is not actively open, subject to Android's NotificationListenerService lifecycle and permission behavior.

Do NOT make false assumptions about Android background execution.

The service should process incoming notification events without requiring the user to keep the Finly screen open.

Handle:

- Service connected
- Service disconnected
- Permission unavailable
- Notification removed/updated where relevant
- App process recreation
- Device restart behavior according to Android's actual lifecycle

Do not create an aggressive polling service.

Do not run a continuous background loop.

Use notification events as the trigger.

TEST on the Pixel 9 emulator where realistic.

If the emulator cannot generate realistic bank/UPI notifications, create controlled test notifications or automated tests rather than pretending real banking notifications were verified.

BUILD.

VERIFY.

REPORT.

STOP.

==================================================
STEP 10 — AUTOMATIC CAPTURE SETTINGS
==================================================

Add a user-facing setting for Automatic Expense Capture.

The user must be able to clearly see:

Automatic Expense Capture
ON / OFF

Also provide the current notification-access status.

If notification access is disabled:

Explain that Android Notification Access is required.

Provide a button/action that takes the user to the appropriate Android notification access settings screen.

Do NOT bypass Android permission controls.

When automatic capture is OFF:

Finly must not automatically create expenses from notifications.

Manual expense entry must continue to work.

When turned ON:

Automatic processing can resume after the required Android permission is granted.

Keep the UI consistent with Finly's existing personalization/settings design.

TEST:
- Automatic capture OFF
- Automatic capture ON
- Permission unavailable
- Permission available
- Reopening settings
- App restart
- Manual expense entry remains functional

BUILD.

VERIFY.

REPORT.

STOP.

==================================================
STEP 11 — PRIVACY AND DATA MINIMIZATION
==================================================

Because transaction notifications may contain highly sensitive financial information, implement privacy-conscious processing.

Requirements:

- Process notifications locally.
- Do NOT send notification text to any cloud API.
- Do NOT send notification data to external AI services.
- Do NOT log full notification contents in Logcat in production code.
- Do NOT store unnecessary raw notification text permanently.
- Store only the information necessary for the expense and duplicate protection.
- Avoid exposing notification contents in UI unnecessarily.

Debug logging, if temporarily required during development, must avoid exposing sensitive information where practical and must not become permanent production logging.

Automatic capture must be clearly user-controlled.

TEST:
- Sensitive content is not unnecessarily persisted
- No full notification text appears in normal logs
- Automatic capture can be disabled

BUILD.

VERIFY.

REPORT.

STOP.

==================================================
STEP 12 — FAILURE AND EDGE CASE HANDLING
==================================================

Implement safe behavior for malformed or unexpected notifications.

Examples:

- Missing title
- Missing text
- Empty notification
- No amount
- Multiple amounts
- Invalid amount
- Credit notification
- Ambiguous debit/credit
- Unknown merchant
- Duplicate notification
- Promotional notification
- OTP notification
- Bank balance notification
- Failed transaction
- Pending transaction
- Refund
- Cashback
- Transfer between user's own accounts
- Notification with multiple transaction values

The system must prefer:

IGNORE SAFELY

over:

CREATE A WRONG EXPENSE.

Never crash the NotificationListenerService because one notification could not be parsed.

Create clear internal result states such as:

IGNORED
DEBIT_PARSED
CREDIT_IGNORED
AMBIGUOUS
INVALID
DUPLICATE
CREATED

where appropriate.

TEST all important edge cases.

BUILD.

VERIFY.

REPORT.

STOP.

==================================================
STEP 13 — INTEGRATION WITH EXISTING FINLY FEATURES
==================================================

Verify that automatically captured expenses integrate with everything already built.

Test:

Automatic debit notification
        ↓
Expense created
        ↓
Dashboard updated
        ↓
Analytics updated
        ↓
Category totals updated
        ↓
Budget progress updated
        ↓
Expense list updated
        ↓
Search/filter/sort works
        ↓
Edit expense works
        ↓
Delete expense works

Also verify:

Automatic expense
        ↓
Edit manually
        ↓
Updated expense remains valid

Automatic expense
        ↓
Delete
        ↓
Expense removed correctly

Do not create a separate UI or database representation for automatic expenses unless there is a strong architectural reason.

BUILD.

VERIFY.

REPORT.

STOP.

==================================================
STEP 14 — TEST SUITE
==================================================

Create comprehensive tests for the automatic capture pipeline.

At minimum test:

Notification detection
Debit/credit classification
Amount parsing
Merchant parsing
Transaction parsing
Category resolution
Duplicate detection
Automatic expense creation
Invalid notification handling
Credit ignoring
Unknown transaction handling
Preference ON/OFF behavior

Include representative examples.

Example:

INPUT:
"₹450 debited from A/c XXXX1234 for Swiggy"

EXPECTED:
DEBIT
₹450
Swiggy
Food
Expense created

INPUT:
"₹50,000 credited to your account"

EXPECTED:
CREDIT
No expense created

INPUT:
"Your OTP is 123456"

EXPECTED:
Ignored
No expense

INPUT:
"Payment of ₹250 to Zomato successful"

EXPECTED:
DEBIT
₹250
Zomato
Food
Expense created

INPUT:
Same valid notification twice

EXPECTED:
Only one expense

Run the complete available test suite.

BUILD.

VERIFY.

REPORT.

STOP.

==================================================
STEP 15 — PIXEL 9 VERIFICATION
==================================================

Perform practical Android verification using the existing Pixel 9 emulator where possible.

Verify:

1. Finly launches.
2. Settings contains Automatic Expense Capture.
3. Notification access status is visible.
4. Android permission/settings navigation works.
5. A controlled test notification can reach the listener where emulator capabilities permit.
6. Debit notification is parsed.
7. Credit notification is ignored.
8. Duplicate notification does not create duplicate expenses.
9. Created expense appears in Expenses.
10. Dashboard updates.
11. Category is correct.
12. Analytics/budget data reacts correctly.
13. Manual Add Expense still works.
14. Existing features still work.

If real bank/UPI notification testing is impossible on the emulator, clearly state that limitation.

Do NOT claim real bank testing occurred if it did not.

BUILD.

VERIFY.

REPORT.

STOP.

==================================================
STEP 16 — FINAL REGRESSION AND PHASE UPDATE
==================================================

Perform final regression testing.

Do NOT break existing functionality from Phases 1–10.

Verify:

- Local database
- Expense creation
- Expense editing
- Expense deletion
- Search/filter/sort
- Dashboard
- Categories
- Analytics
- Budgets
- Recurring expenses
- Income/balance
- Data management
- Personalization
- Theme
- Currency
- Date preferences
- Manual expense entry
- Automatic notification capture

Update relevant project documentation to reflect that Finly now supports automatic expense capture from qualifying debit transaction notifications.

Update documentation only where necessary.

Do NOT rewrite unrelated phase documentation.

FINAL REPORT MUST INCLUDE:

- Automatic capture architecture
- Files created/modified
- Notification listener implementation
- Detection logic
- Debit/credit classification
- Parser capabilities
- Category resolution
- Duplicate protection
- Automatic expense creation
- Settings/permission behavior
- Privacy handling
- Tests executed
- Test results
- Build result
- Pixel 9 verification result
- Known limitations
- Anything still requiring future improvement

Do NOT mark this feature complete unless the implementation, tests, and build are successful.

==================================================
ABSOLUTE FINAL RULE
==================================================

ONE STEP ONLY PER EXECUTION.

Never automatically proceed to the next step.

After every step:

IMPLEMENT
→ TEST
→ BUILD
→ VERIFY
→ REPORT
→ STOP

Wait for my explicit instruction.

ZERO GIT OPERATIONS.

Do not run any Git command.

Do not modify `.git/`.

Do not commit.

Do not push.

Do not pull.

Do not fetch.

Do not perform Git repair.

Git is completely outside the scope of this task.

START WITH STEP 1 ONLY.







