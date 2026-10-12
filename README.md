# Library Management System

A Java Swing desktop application for managing a library, with separate student and librarian workflows, MySQL storage, reporting, and an optional AI assistant.

## Project Structure

```text
LibrarySystem/
<<<<<<< HEAD
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

## Demo

[put video here]



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
=======
├── src/                 Java application source
├── lib/                 JDBC and other library jars
├── database/            Database schema and setup SQL
├── Dump20260114/        Additional SQL dumps
├── run.sh               Compile and launch the application
└── setup.sh             Setup helper (see note below)
>>>>>>> 6d4e18f (fixed readme)
```

## Requirements

- Java JDK 11 or newer
- MySQL 8.0 or newer
- MySQL Connector/J in `lib/`

## Database Setup

1. Create or initialize the `library_system` database using `database/library_system.sql` in MySQL Workbench or the MySQL command line.
2. In `src/DBUtil.java`, set the JDBC URL, username, and password for your local MySQL server.

> **Note:** `setup.sh` currently replaces `database/library_system.sql` with a placeholder. Do not run it when you need to preserve the existing schema.

## Build and Run

From the project directory, run:

```bash
chmod +x run.sh
./run.sh
```

The script compiles the Java files with the jars in `lib/` and launches `LibraryGUI`.

## Features

### Student

- Browse and search books, and view book details
- Borrow or reserve books
- View borrowing history and manage a profile
- Ask questions using the optional AI assistant

### Librarian

- Add, update, and remove books
- Manage student records
- Review borrowing, reservation, and overdue information
- Generate reports

## Optional AI Assistant

The AI assistant requires a compatible chat-completions API and a valid API key. Configure the provider settings through the environment variables supported by `src/AIClient.java`. The launch script includes OpenRouter-related placeholders; replace them with your own settings before using the assistant.

## Notes

- Report output currently uses a table-based view. JFreeChart is not bundled with this project.
- Removing a student with related borrow or reservation records may be prevented by database foreign-key constraints.
