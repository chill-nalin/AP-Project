DROP DATABASE IF EXISTS erp_db;

DROP DATABASE IF EXISTS auth_db;

/* Creates the database for authentication (logins, roles, passwords) */
CREATE DATABASE auth_db;

/* Creates the database for all university data (students, courses, grades) */
CREATE DATABASE erp_db;

USE auth_db;

CREATE TABLE users_auth (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    role ENUM('Student', 'Instructor', 'Admin') NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) DEFAULT 'Active',
    last_login TIMESTAMP NULL
);

USE erp_db;

/* Profile for students, linked to auth_db by user_id */
CREATE TABLE students (
    user_id INT PRIMARY KEY,
    roll_no VARCHAR(20) NOT NULL UNIQUE,
    program VARCHAR(100),
    year INT
);

/* Profile for instructors, linked to auth_db by user_id */
CREATE TABLE instructors (
    user_id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(100)
);

/* Course catalog */
CREATE TABLE courses (
    course_id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    credits INT
);

/* Specific sections of a course */
CREATE TABLE sections (
    section_id INT AUTO_INCREMENT PRIMARY KEY,
    course_id INT NOT NULL,
    instructor_id INT,
    day_time VARCHAR(100),
    room VARCHAR(50),
    capacity INT,
    semester VARCHAR(50),
    year INT,
    FOREIGN KEY (course_id) REFERENCES courses(course_id),
    FOREIGN KEY (instructor_id) REFERENCES instructors(user_id)
);

/* Tracks which student is in which section */
CREATE TABLE enrollments (
    enrollment_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    section_id INT NOT NULL,
    status VARCHAR(20) DEFAULT 'Enrolled',
    UNIQUE KEY unique_enrollment (student_id, section_id), /* Prevents duplicate enrollment*/
    FOREIGN KEY (student_id) REFERENCES students(user_id),
    FOREIGN KEY (section_id) REFERENCES sections(section_id)
);

/* Stores grades for each enrollment */
CREATE TABLE grades (
    grade_id INT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id INT NOT NULL,
    component VARCHAR(50) NOT NULL, /* e.g, 'quiz', 'midterm', 'end-sem' */
    score DECIMAL(5, 2),
    final_grade VARCHAR(2),
    FOREIGN KEY (enrollment_id) REFERENCES enrollments(enrollment_id)
);

/* Stores application settings, like Maintenance Mode */
CREATE TABLE settings (
    setting_key VARCHAR(50) PRIMARY KEY,
    setting_value VARCHAR(100)
);

/* Seed data for auth_db */
USE auth_db;
INSERT INTO users_auth (user_id, username, role, password_hash) VALUES
(1, 'admin1', 'Admin', '$2a$10$okaPXEjJwVuIk3mFL/MbqeH6foluy1u.qrtQ3TMybDVtwxEhyxWMG'),
(2, 'inst1', 'Instructor', '$2a$10$9NyH4VHVaxTXv55u87rp4OXQNWKOZ0FEwWSvts8DewfM2hDF1a16W'),
(3, 'stu1', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(4, 'stu2', 'Student', '$2a$10$.21N7BqhHydBbt8A1WqYd.JlMOTaY4THLSWkMfIjUDLv64u4m3zkO');

/* Seed data for erp_db */
USE erp_db;

/* 1. Add corresponding student/instructor profiles */
INSERT INTO instructors (user_id, name, department) VALUES
(2, 'Dr. Sambuddho Chakravarty', 'Computer Science');

INSERT INTO students (user_id, roll_no, program, year) VALUES
(3, '2024001', 'B.Tech CSE', 2),
(4, '2024002', 'B.Tech CSAM', 2);

/* 2. Add the settings */
INSERT INTO settings (setting_key, setting_value) VALUES
('maintenance_on', 'false'),
('current_semester', 'Monsoon'),
('current_year', '2025'),
('drop_deadline', '2025-12-15'),
('registration_deadline', '2025-12-30');

/* 3. Add sample courses and sections */
INSERT INTO courses (code, title, credits) VALUES
('CSE201', 'Advanced Programming', 4),
('MTH210', 'Discrete Structures', 4);

INSERT INTO sections (course_id, instructor_id, day_time, room, capacity, semester, year) VALUES
(1, 2, 'Mon 10:00-11:30', 'C-102', 30, 'Monsoon', 2025),
(2, 2, 'Tue 14:00-15:30', 'C-01', 2, 'Monsoon', 2025);

/* 4. Enroll one student in one course */
INSERT INTO enrollments (student_id, section_id) VALUES
(3, 1);