Library Management System - README
BY:
Hana Kouiriti
Merjen Dursunova

PROJECT STRUCTURE:
LibrarySystem/
├── src/
│ ├── DBUtil.java (Database connection utility)
│ ├── LibraryApp.java (Main application with login)
│ ├── StudentMenu.java (Student functions)
│ ├── LibrarianMenu.java (Librarian functions)
│ ├── BookManager.java (Book CRUD operations)
│ └── BorrowManager.java (Borrow/return logic)
├── lib/
│ └── mysql-connector-j-9.5.0.jar
├── database/
│ └── library_system.sql (Complete database schema)
└── README.txt

PREREQUISITES:

1. MySQL 8.0+ installed and running
2. Java JDK 11+
3. MySQL Connector/J 9.5.0

SETUP INSTRUCTIONS:

STEP 1: Set up Database

1. Open MySQL Workbench or command line
2. Run the SQL script: database/library_system.sql
3. Database 'library_system' will be created with all tables and sample data

STEP 2: Configure Database Connection

1. Open DBUtil.java
2. Change the following lines if needed:
   - URL: "jdbc:mysql://localhost:3306/library_system"
   - USER: "root"
   - PASSWORD: "yourpassword" (change to your MySQL password)

STEP 3: Compile and Run

1. Open terminal in project directory
2. Compile all Java files:
   javac -cp "lib/mysql-connector-j-9.5.0.jar" src/\*.java
3. Run the application:
   java -cp "src:lib/mysql-connector-j-9.5.0.jar" LibraryApp

SAMPLE LOGIN CREDENTIALS:

Students:

1. Student ID: 1001, Email: alice@uni.edu
2. Student ID: 1002, Email: bob@uni.edu
3. Student ID: 1003, Email: charlie@uni.edu

Librarians:

1. Email: admin@library.edu, Password: admin123
2. Email: john@library.edu, Password: password123

FEATURES IMPLEMENTED:
Student Functions:

- Browse available books
- Borrow books (14-day limit)

````markdown
# LibrarySystem

Small Java Swing library management application with student and librarian interfaces, a basic reporting view, and an experimental AI assistant.

Date: 2026-01-14

---

## Overview

- Language: Java (11+)
- UI: Swing (desktop)
- DB: MySQL / MariaDB (JDBC via `DBUtil`)
- Source: `src/`
- SQL schema/dumps: `database/`, `Dump20260114/`

## Project structure (high level)

- `src/` — Java source files (UI, DB helpers, AI client, managers)
- `lib/` — third-party jars (example: MySQL connector)
- `database/` — schema and SQL to create/load sample data
- `Dump20260114/` — extra SQL dumps
- `run.sh`, `setup.sh` — scripts with helpful env hints and run logic

## Major features

- Authentication: login and registration (`LoginWindow.java`). UI tweaks improve button contrast and input sizing.
- Student UI:
  - Browse/search books (`BookBrowserPanel.java`)
  - Book details dialog (`BookDetailsDialog.java`) — can auto-send a contextual question to the AI assistant
  - Borrow/reserve books and view history
  - Profile with Logout and Delete Account (delete is visually red and performs a DELETE on the `Student` table)
- Librarian UI:
  - Manage books (add/update/remove)
  - Manage students (list, Edit, Delete) — implemented with a DefaultTableModel and JDBC UPDATE/DELETE flows
  - View borrow/reservation data and overdue items
  - Generate reports (tabular summaries; charting disabled by default)
- Borrow/Return and fine handling:
  - Java-side fine computation (fallback to avoid missing DB stored procedure): reads DueDate and computes fines at $0.50/day overdue, updates `BorrowRecord`.
- AI Assistant (student-facing):
  - `AIClient.java` — HTTP client driven by env vars: `AI_API_URL`, `AI_API_KEY`, `AI_MODEL` (and OpenRouter fallbacks `YOUR_API_KEY`, `OPENROUTER_MODEL`).
  - `AIChatDialog.java` — chat UI with New Chat, session history (in-memory), disables input while waiting, and replaces "Thinking..." with the real response.
  - Responses are sanitized (no markdown, no raw `\n`) using a system prompt and post-processing.

## Setup & run

1. Install Java 11+ and MySQL (or MariaDB).
2. Create a database and load schema:
   - `mysql -u root -p < database/library_system.sql` (or run via Workbench)
3. Configure DB connection in `DBUtil.java` (JDBC URL, user, password).
4. (Optional) Set AI environment variables if you plan to use the AI assistant. Example exports are included in `run.sh` and `setup.sh`.

Build and run (quick):

```bash
cd /Users/mac/Desktop/LibrarySystem
javac -d out $(find src -name "*.java")
java -cp out LibraryApp
```
````

Or use the convenience script(s):

```bash
./setup.sh   # applies helpful environment hints
./run.sh     # compiles and runs the application (reads env exports in the script)
```

## AI configuration

- Environment variables the app reads:
  - `AI_API_URL` — API endpoint for chat completions (OpenRouter/OpenAI-style)
  - `AI_API_KEY` — API key for the endpoint
  - `AI_MODEL` — preferred model string
  - `YOUR_API_KEY` and `OPENROUTER_MODEL` — optional fallbacks used by `AIClient` if provided

- Logs: raw API error bodies are written to `logs/ai_error_<timestamp>.json` for debugging when provider errors occur.

## Report generation / charts

- `ReportGenerator.java` originally used JFreeChart (org.jfree.\*). In the current workspace JFreeChart jars are not bundled, so `ReportGenerator` has been replaced with a lightweight table-based view so the project compiles out-of-the-box.
- To re-enable charts:
  1.  Add JFreeChart (and any required supporting jars) to `lib/`.
  2.  Compile with the jars on the classpath, e.g. `javac -cp "lib/*:out" ...` or set up Gradle/Maven with `org.jfree` dependencies.

## Known issues & notes

- Report charts: missing JFreeChart jars will cause compile errors if you restore the original charting code.
- Provider policy: some OpenRouter models may be blocked by provider-side policy settings (you may see 404 policy error JSON). Adjust provider privacy settings or switch models.
- Delete operations: deleting a `Student` that has borrow/reservation records may be blocked by foreign key constraints — either remove dependent records first or adapt the DB to use cascade rules.

## Where to look in code

- AI: `src/AIClient.java`, `src/AIChatDialog.java`
- Student UI: `src/StudentDashboard.java`, `src/BookBrowserPanel.java`, `src/BookDetailsDialog.java`
- Librarian UI: `src/LibrarianDashboard.java`
- Borrow/Return + fine: `src/BorrowReturnPanel.java`
- DB helper: `src/DBUtil.java`
