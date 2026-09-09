# 🧾 FINLY — REAL USER TRANSACTIONS DETECTION & VERIFICATION REPORT

**Document ID**: `REPORT-REAL-TXN-002`  
**Date**: `2026-09-09`  
**Test Suite Class**: [`RealUserTransactionsTestSuite.kt`](file:///Users/surajkoley/Projects/Expense%20Tracker%20App/app/src/test/java/com/personalexpensetracker/data/notification/RealUserTransactionsTestSuite.kt)  
**Real-World Test Cases**: `11 / 11 PASSED (100% Pass Rate)`  
**Master Application Test Suite**: `485 / 485 PASSED (100% Pass Rate)`  
**Build Status**: `BUILD SUCCESSFUL`

---

## 1. Context & Source Breakdown

This verification phase tests the exact real-world financial notifications provided directly from phone screenshots. The notifications cover multiple financial institutions and apps:
1. **Slice Bank (`VK-SLCBNK-S`, `VA-SLCBNK-S`, `JM-SLCBNK-S`)**:
   - Outgoing UPI transactions formatted as: `Rs. [amount] sent from a/c xx0322 on [date] to [Merchant] (UPI Ref: [ref]). Not you? Call 08048329999 - slice`
   - Non-transactional administrative alerts (Weekly saver atom created/deleted) which must be safely ignored without creating false expenses.
2. **HDFC Bank UPI (`JX-HDFCBK-S`)**:
   - Multiline UPI debits formatted as: `Sent Rs.1.00\nFrom HDFC Bank A/C\n*7201\nTo ARYA MUKHERJEE\nOn 13/08/26\nRef 127857047659\nNot You? Call 18002586161/SMS BLOCK UPI to 7308080808`
3. **HDFC Bank Card (`JX-HDFCBK-S`)**:
   - Multiline card debits formatted as: `Spent Rs.74 On\nHDFC Bank Card\n6446 At ASSPL On\n2026-09-08:01:09:36.Not You? To Block+Reissue Call 18002586161/SMS BLOCK CC 6446 to 7308080808`

---

## 2. Real Transactions Verification Matrix

| # | Source / Sender | Full Message Text | Amount Detected | Integer (Clean) | Direction | Merchant Extracted | Account / Ref Extracted | Database Action | Status |
|---|---|---|---|---|---|---|---|---|---|
| **01** | `VK-SLCBNK-S` (Slice) | *"Rs. 960 sent from a/c xx0322 on 17-Jun-26 to MAHESH COMPANY (UPI Ref: 616835125961). Not you? Call 08048329999 - slice"* | `Rs. 960` | `960.00` | `DEBIT` | `MAHESH COMPANY` | A/c: `0322`<br>Ref: `616835125961` | **Expense Created** (id=1) | **PASS** |
| **02** | `VK-SLCBNK-S` (Slice) | *"Rs. 9.70 sent from a/c xx0322 on 27-Jun-26 to Indian Railways UTS (UPI Ref: 654474188225). Not you? Call 08048329999 - slice"* | `Rs. 9.70` | `9.70` | `DEBIT` | `Indian Railways UTS` | A/c: `0322`<br>Ref: `654474188225` | **Expense Created** (id=2) | **PASS** |
| **03** | `VK-SLCBNK-S` (Slice) | *"Rs. 4.85 sent from a/c xx0322 on 09-Sep-26 to Indian Railways UTS (UPI Ref: 129307963204). Not you? Call 08048329999 - slice"* | `Rs. 4.85` | `4.85` | `DEBIT` | `Indian Railways UTS` | A/c: `0322`<br>Ref: `129307963204` | **Expense Created** (id=3) | **PASS** |
| **04** | `VA-SLCBNK-S` (Slice) | *"Rs. 125 sent from a/c xx0322 on 08-Sep-26 to Sourav Store (UPI Ref: 625159788516). Not you? Call 08048329999 - slice"* | `Rs. 125` | `125.00` | `DEBIT` | `Sourav Store` | A/c: `0322`<br>Ref: `625159788516` | **Expense Created** (id=4) | **PASS** |
| **05** | `VA-SLCBNK-S` (Slice) | *"Rs. 1,000 sent from a/c xx0322 on 08-Sep-26 to TECHNO INDIA HOOGHLY (UPI Ref: 625187723190). Not you? Call 08048329999 - slice"* | `Rs. 1,000` | `1000.00` | `DEBIT` | `TECHNO INDIA HOOGHLY` | A/c: `0322`<br>Ref: `625187723190` | **Expense Created** (id=5) | **PASS** |
| **06** | `JM-SLCBNK-S` (Slice) | *"Rs. 100 sent from a/c xx0322 on 03-Aug-26 to AAMAR KOLKATA METRO (UPI Ref: 103786402142). Not you? Call 08048329999 - slice"* | `Rs. 100` | `100.00` | `DEBIT` | `AAMAR KOLKATA METRO` | A/c: `0322`<br>Ref: `103786402142` | **Expense Created** (id=6) | **PASS** |
| **07** | `JM-SLCBNK-S` (Slice) | *"Rs. 50 sent from a/c xx0322 on 06-Sep-26 to Sankar Biswas (UPI Ref: 624938074811). Not you? Call 08048329999 - slice"* | `Rs. 50` | `50.00` | `DEBIT` | `Sankar Biswas` | A/c: `0322`<br>Ref: `624938074811` | **Expense Created** (id=7) | **PASS** |
| **08** | `JX-HDFCBK-S` (HDFC) | *"Sent Rs.1.00\nFrom HDFC Bank A/C\n*7201\nTo ARYA MUKHERJEE\nOn 13/08/26\nRef 127857047659..."* | `Rs.1.00` | `1.00` | `DEBIT` | `ARYA MUKHERJEE` | A/c: `7201`<br>Ref: `127857047659` | **Expense Created** (id=8) | **PASS** |
| **09** | `JX-HDFCBK-S` (HDFC) | *"Spent Rs.74 On\nHDFC Bank Card\n6446 At ASSPL On\n2026-09-08:01:09:36.Not You?..."* | `Rs.74` | `74.00` | `DEBIT` | `ASSPL` | Card: `6446` | **Expense Created** (id=9) | **PASS** |
| **10** | `VA-SLCBNK-S` (Alert) | *"Your Weekly saver atom was created on 03 Sep '26. Auto-save is set for every Sunday. For queries, call 080-4832-9999 - slice"* | `null` | `null` | `NON-FINANCIAL` | `null` | — | **Ignored (0 expenses)** | **PASS** |
| **11** | `JM-SLCBNK-S` (Alert) | *"Your Weekly saver atom has been successfully deleted on 05 Sep '26. For queries, call 080-4832-9999 - slice"* | `null` | `null` | `NON-FINANCIAL` | `null` | — | **Ignored (0 expenses)** | **PASS** |

---

## 3. Technical Enhancements Implemented

### 3.1 TRAI DLT Bank Sender Headers
- **Pattern Identified**: Indian TRAI telecom headers often feature operator/service suffixes, such as `-S` (Service SMS), e.g. `VK-SLCBNK-S`, `VA-SLCBNK-S`, `JM-SLCBNK-S`, `JX-HDFCBK-S`.
- **Engine Update**: Updated `TRAI_BANK_HEADER_REGEX` in `FinancialNotificationDetector.kt`:
  ```kotlin
  Regex("""^(?:[A-Za-z]{2}-)?[A-Za-z0-9]{4,10}(?:-[A-Za-z0-9]{1,4})?$""")
  ```
  Now recognizes all standard bank codes (`SLCBNK`, `HDFCBK`, `SBIINB`, `ICICIB`, `AXISBK`, `PAYTMB`) with or without gateway prefixes (`VK-`, `JX-`, `VA-`, `JM-`) and suffixes (`-S`, `-T`, `-G`).
- Added `"slcbnk"` and `"hdfcbk"` explicitly to `BANK_SENDER_KEYWORDS`.

### 3.2 "Sent from Account" Debit Direction Classification
- **Pattern Identified**: Slice and HDFC SMS structure outgoing transfers as:
  - `Rs. [amount] sent from a/c ... to [merchant]`
  - `Sent Rs. [amount] From [bank] A/C ... To [payee]`
- **Engine Update**:
  - Added `"sent from a/c"`, `"sent from acct"`, `"sent from account"`, `"sent from card"`, `"sent from your a/c"`, `"sent"` to `EXPLICIT_DEBIT_KEYWORDS` and `DEBIT_KEYWORDS`.
  - Added `amountSentRegex = Regex("""(?:₹|[Rr][Ss]\.?|INR|\$)?\s*\d+(?:\.\d+)?\s+sent\b""")`.
  - Added `sentFromAccountRegex = Regex("""\bsent\b[\s\S]*?\bfrom\s+(?:[A-Za-z0-9]+\s+)*(?:a/?c|acct|account|card)\b""")`.
  - Updated `calculateStructuralDebitScore` so that `"from a/c"` (outward debit from user's account) does not get falsely flagged as an inward transfer.

### 3.3 Parenthesized UPI Ref & Safety Suffix Merchant Cleaning
- **Pattern Identified**:
  - `to MAHESH COMPANY (UPI Ref: 616835125961). Not you? Call 08048329999 - slice`
  - `To ARYA MUKHERJEE\nOn 13/08/26\nRef 127857047659`
- **Engine Update**:
  - Updated `MERCHANT_PATTERNS` lookahead to stop before `\(` (opening parenthesis), `upi`, `not you`, `call`.
  - Added `cleanMerchant` normalization:
    ```kotlin
    if (s.contains("(")) s = s.substringBefore("(").trim()
    if (lower.contains("not you")) s = s.substring(0, lower.indexOf("not you")).trim()
    if (lower.contains("call ")) s = s.substring(0, lower.indexOf("call ")).trim()
    ```
  - Cleanly strips phone numbers, customer support hotlines, and security warnings from merchant names.

### 3.4 Card & Account Last-4 Extraction
- Accurately captures account identifiers across layouts:
  - `xx0322` -> `0322`
  - `*7201` -> `7201`
  - `Card 6446` -> `6446`

---

## 4. Verification & Regression Status

- **Real User Transactions Test**: `11 / 11 passed` (`RealUserTransactionsTestSuite.kt`)
- **Master Unit Test Suite**: `485 / 485 passed` (`testDebugUnitTest`)
- **All Previous Test Suites**: 100% passing (0 regressions)
