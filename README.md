# AP Project
## University ERP System

**Authors:** Nalin Gupta, Tanish Jindal
**Roll Numbers:** 2024362, 2024579
**Course:** CSE201: Advanced Programming

---

## Introduction

### Project Overview

The University ERP System is a desktop application designed to streamline academic operations for a university. The system provides comprehensive functionality for managing users, courses, sections, enrollments, and grades while maintaining strict access control and data security.

### Key Features

- **Role-Based Access Control**: Three distinct user roles (Admin, Instructor, Student) with specific permissions
- **Secure Authentication**: Password hashing using bcrypt with no plaintext password storage
- **Dual Database Architecture**: Separate databases for authentication and ERP data
- **Maintenance Mode**: System-wide read-only mode for students and instructors
- **Modern Dark UI**: Contemporary interface using FlatLaf with sidebar navigation

### Technology Stack

- **Language**: Java JDK 21
- **UI Framework**: Java Swing with FlatLaf (Dark Theme)
- **Layout Manager**: MigLayout
- **Database**: MySQL (auth_db and erp_db)
- **Password Hashing**: jBCrypt
- **Logging**: SLF4J with Logback
- **Testing**: JUnit 5 with Mockito

---

## Compilation and Execution

### Prerequisites

- Java Development Kit (JDK) 21 or higher
- MySQL Server 8.0 or higher
- Maven 3.6 or higher (for dependency management)

### Database Setup

1. **Configure database credentials**

   Open `src/resources/application.conf` and update the database user, password, and MySQL `bin` directory to match your local setup:

   ```
   db {
     host = "localhost"
     port = "3306"
     user = "root"
     password = "YOUR_PASSWORD"
   }

   mysql_paths {
     # Example for macOS:
     # bin_directory = "/usr/local/mysql/bin/"
     # Example for Windows:
     # bin_directory = "C:/Program Files/MySQL/MySQL Server 8.0/bin/"
   }
   ```

2. Start MySQL server

3. Execute the seed script:
   ```bash
   mysql -u root -p < small_seed.sql
   ```
   OR
   ```bash
   mysql -u root -p < large_seed.sql
   ```
   (for a larger seed data)

4. This creates two databases: `auth_db` and `erp_db`

### Compilation

Navigate to the project root directory and execute:
```bash
mvn clean compile
```

### Execution

Run the application using Maven:
```bash
mvn exec:java -Dexec.mainClass="edu.univ.erp.ui.Main"
```

Or execute the compiled JAR:
```bash
java -jar target/university-erp-1.0.jar
```

Or run the project by creating an `IntelliJ Maven` Project, and running the `Main.java` file from there.

### Default Credentials

| Username | Password | Role       |
|----------|----------|------------|
| admin1   | pass123  | Admin      |
| inst1    | pass123  | Instructor |
| stu1     | pass123  | Student    |

---

## System Architecture

### Database Schema

#### Auth Database (auth_db)

**users_auth Table Structure**

| Column        | Type         | Description                    |
|---------------|--------------|--------------------------------|
| user_id       | INT          | Primary key, auto-increment    |
| username      | VARCHAR(50)  | Unique username                |
| role          | ENUM         | Student, Instructor, or Admin  |
| password_hash | VARCHAR(255) | bcrypt hashed password         |
| status        | VARCHAR(20)  | Account status (Active/Inactive) |
| last_login    | TIMESTAMP    | Last successful login time     |

#### ERP Database (erp_db)

**ERP Database Tables**

| Table       | Purpose                                                                                           |
|-------------|---------------------------------------------------------------------------------------------------|
| students    | Student profiles (user_id, roll_no, program, year)                                               |
| instructors | Instructor profiles (user_id, name, department)                                                   |
| courses     | Course catalog (course_id, code, title, credits)                                                  |
| sections    | Course sections (section_id, course_id, instructor_id, day_time, room, capacity, semester, year) |
| enrollments | Student registrations (enrollment_id, student_id, section_id, status)                            |
| grades      | Assessment scores (grade_id, enrollment_id, component, score, final_grade)                       |
| settings    | System settings (setting_key, setting_value)                                                      |

### Final Grade Input and Computation

The ERP system allows instructors to enter the assessment scores and grading thresholds for each course section. All component scores entered by the instructor are assumed to already be **scaled out of 100**. The ERP then computes the final grade based on simple addition and the grading thresholds provided by the instructor.

#### Assessment Components

Each course uses the following three components, each with a score between 0 and 100:

- **Quiz**
- **Mid-Term Exam**
- **End-Term Exam**

The instructor enters these component scores directly in the Gradebook interface. No automatic weighting or scaling is applied.

#### Final Score Calculation

The ERP computes the final numeric score using:

```
FinalScore = Quiz + MidTerm + EndSem
```

The maximum possible final score is 100.

#### Instructor-Defined Grading Thresholds

Unlike systems with fixed grading schemes, this ERP allows each instructor to define their own grading thresholds. During grade entry, the instructor provides the minimum score required for each letter grade.

An example of instructor-provided thresholds:

| Letter Grade | Minimum Score Required |
|--------------|------------------------|
| A+           | 70+                    |
| A            | 60+                    |
| B            | 50+                    |
| C            | 40+                    |
| D            | 30+                    |
| F            | Below 30               |

These values are stored in the `grades` table along with the student's final score.

#### Letter Grade Assignment Logic

Once the thresholds are stored, the ERP assigns the final letter grade using the following procedure:

1. Compute the student's total score (0–100).
2. Compare the score against the instructor-defined thresholds.
3. Assign the highest letter grade for which the score meets the minimum requirement.

Formally, if the instructor provides thresholds T<sub>A+</sub>, T<sub>A</sub>, T<sub>B</sub>, T<sub>C</sub>, T<sub>D</sub>, then the assigned grade G is:

- **A+** if FinalScore ≥ T<sub>A+</sub>
- **A** if FinalScore ≥ T<sub>A</sub>
- **B** if FinalScore ≥ T<sub>B</sub>
- **C** if FinalScore ≥ T<sub>C</sub>
- **D** if FinalScore ≥ T<sub>D</sub>
- **F** otherwise

#### Validation

Before computing final grades, the following checks are performed:

- The sum of the components must add up to 100.
- Threshold values must be strictly decreasing (e.g., T<sub>A+</sub> ≥ T<sub>A</sub> ≥ T<sub>B</sub>).
- All thresholds must be numeric and non-negative.

#### Integration With the Gradebook

After computation:

- The letter grade is shown in the instructor UI,
- Saved to the `grades` table,
- Displayed to students in the Student Dashboard.

This design allows flexibility while keeping the grading workflow simple and transparent for instructors.

---

## Role-Based Access Control

### Access Rules Implementation

The system enforces strict access control through the `AccessControl` class:

```java
public class AccessControl {
    public boolean canModifySection(User user, int sectionId) {
        if (user.role() == Role.Admin) return true;
        if (user.role() == Role.Instructor) {
            // Verify instructor owns this section
            return sectionRepo.isInstructorForSection(
                user.userId(), sectionId);
        }
        return false;
    }
}
```

### Maintenance Mode Enforcement

When maintenance mode is ON:
- **Banner Display**: Yellow warning banner appears on all dashboards
- **Write Blocking**: All data modification operations are blocked
- **Read Access**: View operations remain available
- **Admin Override**: Admins can still disable maintenance mode

---

## Authentication and Security

### Password Security

#### Hashing Implementation

Passwords are secured using bcrypt with work factor 10:

```java
public class PasswordHasher {
    private static final int WORK_FACTOR = 10;
    
    public static String hash(String password) {
        return BCrypt.hashpw(password, 
            BCrypt.gensalt(WORK_FACTOR));
    }
    
    public static boolean checkPassword(String plaintext, 
                                        String hashed) {
        return BCrypt.checkpw(plaintext, hashed);
    }
}
```

#### Security Features

- **No Plaintext Storage**: Only bcrypt hashes stored in database
- **Automatic Salting**: bcrypt handles salt generation
- **Configurable Work Factor**: Can increase computational cost
- **Session Management**: Single active session per user
- **Change Password**: Users can update passwords securely

### Database Separation

#### Auth DB (auth_db)

- Contains only authentication data
- Accessed through `UserAuthRepository`
- Handles login verification
- Stores password hashes
- Tracks login attempts (removed lockout in final version)

#### ERP DB (erp_db)

- Contains all application data
- No password or authentication information
- Linked to auth_db via `user_id`
- Accessed through domain-specific repositories

### Login Flow

1. User enters username and password in LoginWindow
2. `AuthApi.login()` called with credentials
3. `AuthService.login()` validates credentials:
   - Query `auth_db` for user record
   - Verify password hash using bcrypt
   - If valid, update `last_login`
4. Load user profile from `erp_db` based on role
5. Create `User` object with profile data
6. Store in `SessionManager` singleton
7. Route to appropriate dashboard

### Test Infrastructure

- **Framework**: JUnit 5
- **Mocking**: Mockito with MockitoExtension
- **Coverage**: Service layer and repository layer
- **Approach**: Unit tests with dependency injection

---

## Additional Features

### Implemented Features

1. **Auto-refresh on Tab Switch**: Panels refresh data when selected
2. **Comprehensive Logging**: SLF4J logging throughout application
3. **Input Validation**: Client-side and server-side validation
4. **Error Recovery**: Graceful handling of database errors

### Bonus Features Attempted

- **Change Password Dialog**: Fully implemented with validation. Available to all users.
- **Grades CSV Export**: Students can export their final grades to a CSV file

---

## Known Limitations

### Current Limitations

- **Single Session**: Users cannot log in from multiple locations
- **No Backup/Restore**: Manual database backup required
- **No Email Notifications**: Manual communication required

---

## References

1. Oracle Java Documentation. https://docs.oracle.com/en/java/
2. FlatLaf Look and Feel. https://www.formdev.com/flatlaf/
3. MigLayout Documentation. http://www.miglayout.com/
4. jBCrypt Library. https://www.mindrot.org/projects/jBCrypt/
5. JUnit 5 User Guide. https://junit.org/junit5/docs/current/user-guide/
6. Mockito Documentation. https://site.mockito.org/
7. MySQL Reference Manual. https://dev.mysql.com/doc/
8. SLF4J Manual. http://www.slf4j.org/manual.html
