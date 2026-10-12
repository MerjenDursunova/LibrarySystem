# Library Management System

A Java Swing desktop application for managing library books, students, borrowing, returns, and reports. It uses MySQL for data storage and includes an optional AI assistant.

## Features

- Student and librarian interfaces
- Browse and manage books
- Borrow, return, and reserve books
- View borrowing records and overdue items
- Generate tabular reports
- Optional AI assistant for students

## Requirements

- JDK 15 or newer
- MySQL 8.0 or newer
- MySQL Connector/J in the `lib` directory

## Project Layout

```text
LibrarySystem/
├── src/                 Java application source
├── lib/                 JDBC and other library jars
├── database/            Database schema and setup SQL
├── Dump20260114/        Additional SQL dumps
├── run.sh               Compile and launch the application
└── setup.sh             Setup helper
```

## Setup

1. Create the `library_system` database by running `database/library_system.sql` with MySQL Workbench or the MySQL command line.
2. Open `src/DBUtil.java` and set the JDBC URL, username, and password for your MySQL server.
3. Make sure the MySQL Connector/J jar is present in `lib/`.

> **Important:** `setup.sh` currently replaces `database/library_system.sql` with a placeholder. Do not run it if you need to keep the existing schema file.

## Run

From the project directory, run:

```bash
./run.sh
```

The script compiles the Java sources and starts `LibraryGUI`.

## AI Assistant

The assistant needs a compatible chat-completions API and a valid API key. Configure the provider variables read by `src/AIClient.java` before using it. The placeholders in `run.sh` are not valid credentials.

## Notes

- Report output currently uses tables; JFreeChart is not included.
- Deleting a student with related borrow or reservation records may be blocked by database foreign-key constraints.
