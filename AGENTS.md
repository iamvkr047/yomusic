# Agent Guidelines

## Build & Environment Rules

### 1. JDK Requirement
- This project strictly requires **JDK 21** [8 < 21 (recommended) < 23] (newer versions such as Java 25 cause Gradle Kotlin DSL parser errors).
- **DO NOT edit or add `org.gradle.java.home` in `gradle.properties`**, as this file is tracked in Git and must not contain machine-specific paths.

### 2. Build & Verification Commands
- **Recommended (Git Bash / Linux / WSL)**:
  Whenever need to verify or test code by compiling and building, use build.sh, which automatically handles environment.
  ```bash
  bash build.sh
  ```
- **Windows PowerShell**:
  Ensure `JAVA_HOME` is set to a valid local JDK 21 installation in the environment before running Gradle commands (do not commit any local paths):
  ```powershell
  .\gradlew compileDebugJavaWithJavac
  ```
