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

USE auth_db;

-- Admin users (5 admins total)
INSERT INTO users_auth (user_id, username, role, password_hash) VALUES
(1, 'admin1', 'Admin', '$2a$10$okaPXEjJwVuIk3mFL/MbqeH6foluy1u.qrtQ3TMybDVtwxEhyxWMG'),
(5, 'admin2', 'Admin', '$2a$10$okaPXEjJwVuIk3mFL/MbqeH6foluy1u.qrtQ3TMybDVtwxEhyxWMG'),
(6, 'admin3', 'Admin', '$2a$10$okaPXEjJwVuIk3mFL/MbqeH6foluy1u.qrtQ3TMybDVtwxEhyxWMG'),
(7, 'admin4', 'Admin', '$2a$10$okaPXEjJwVuIk3mFL/MbqeH6foluy1u.qrtQ3TMybDVtwxEhyxWMG'),
(8, 'admin5', 'Admin', '$2a$10$okaPXEjJwVuIk3mFL/MbqeH6foluy1u.qrtQ3TMybDVtwxEhyxWMG');

-- Instructor users (15 instructors total)
INSERT INTO users_auth (user_id, username, role, password_hash) VALUES
(2, 'inst1', 'Instructor', '$2a$10$9NyH4VHVaxTXv55u87rp4OXQNWKOZ0FEwWSvts8DewfM2hDF1a16W'),
(9, 'inst2', 'Instructor', '$2a$10$9NyH4VHVaxTXv55u87rp4OXQNWKOZ0FEwWSvts8DewfM2hDF1a16W'),
(10, 'inst3', 'Instructor', '$2a$10$9NyH4VHVaxTXv55u87rp4OXQNWKOZ0FEwWSvts8DewfM2hDF1a16W'),
(11, 'inst4', 'Instructor', '$2a$10$9NyH4VHVaxTXv55u87rp4OXQNWKOZ0FEwWSvts8DewfM2hDF1a16W'),
(12, 'inst5', 'Instructor', '$2a$10$9NyH4VHVaxTXv55u87rp4OXQNWKOZ0FEwWSvts8DewfM2hDF1a16W'),
(13, 'inst6', 'Instructor', '$2a$10$9NyH4VHVaxTXv55u87rp4OXQNWKOZ0FEwWSvts8DewfM2hDF1a16W'),
(14, 'inst7', 'Instructor', '$2a$10$9NyH4VHVaxTXv55u87rp4OXQNWKOZ0FEwWSvts8DewfM2hDF1a16W'),
(15, 'inst8', 'Instructor', '$2a$10$9NyH4VHVaxTXv55u87rp4OXQNWKOZ0FEwWSvts8DewfM2hDF1a16W'),
(16, 'inst9', 'Instructor', '$2a$10$9NyH4VHVaxTXv55u87rp4OXQNWKOZ0FEwWSvts8DewfM2hDF1a16W'),
(17, 'inst10', 'Instructor', '$2a$10$9NyH4VHVaxTXv55u87rp4OXQNWKOZ0FEwWSvts8DewfM2hDF1a16W'),
(18, 'inst11', 'Instructor', '$2a$10$9NyH4VHVaxTXv55u87rp4OXQNWKOZ0FEwWSvts8DewfM2hDF1a16W'),
(19, 'inst12', 'Instructor', '$2a$10$9NyH4VHVaxTXv55u87rp4OXQNWKOZ0FEwWSvts8DewfM2hDF1a16W'),
(20, 'inst13', 'Instructor', '$2a$10$9NyH4VHVaxTXv55u87rp4OXQNWKOZ0FEwWSvts8DewfM2hDF1a16W'),
(21, 'inst14', 'Instructor', '$2a$10$9NyH4VHVaxTXv55u87rp4OXQNWKOZ0FEwWSvts8DewfM2hDF1a16W'),
(22, 'inst15', 'Instructor', '$2a$10$9NyH4VHVaxTXv55u87rp4OXQNWKOZ0FEwWSvts8DewfM2hDF1a16W');

-- Student users (80 students total)
INSERT INTO users_auth (user_id, username, role, password_hash) VALUES
(3, 'stu1', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(4, 'stu2', 'Student', '$2a$10$.21N7BqhHydBbt8A1WqYd.JlMOTaY4THLSWkMfIjUDLv64u4m3zkO'),
(23, 'stu3', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(24, 'stu4', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(25, 'stu5', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(26, 'stu6', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(27, 'stu7', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(28, 'stu8', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(29, 'stu9', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(30, 'stu10', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(31, 'stu11', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(32, 'stu12', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(33, 'stu13', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(34, 'stu14', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(35, 'stu15', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(36, 'stu16', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(37, 'stu17', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(38, 'stu18', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(39, 'stu19', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(40, 'stu20', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(41, 'stu21', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(42, 'stu22', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(43, 'stu23', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(44, 'stu24', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(45, 'stu25', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(46, 'stu26', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(47, 'stu27', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(48, 'stu28', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(49, 'stu29', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(50, 'stu30', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(51, 'stu31', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(52, 'stu32', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(53, 'stu33', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(54, 'stu34', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(55, 'stu35', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(56, 'stu36', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(57, 'stu37', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(58, 'stu38', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(59, 'stu39', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(60, 'stu40', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(61, 'stu41', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(62, 'stu42', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(63, 'stu43', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(64, 'stu44', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(65, 'stu45', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(66, 'stu46', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(67, 'stu47', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(68, 'stu48', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(69, 'stu49', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(70, 'stu50', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(71, 'stu51', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(72, 'stu52', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(73, 'stu53', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(74, 'stu54', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(75, 'stu55', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(76, 'stu56', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(77, 'stu57', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(78, 'stu58', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(79, 'stu59', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(80, 'stu60', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(81, 'stu61', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(82, 'stu62', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(83, 'stu63', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(84, 'stu64', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(85, 'stu65', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(86, 'stu66', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(87, 'stu67', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(88, 'stu68', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(89, 'stu69', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(90, 'stu70', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(91, 'stu71', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(92, 'stu72', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(93, 'stu73', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(94, 'stu74', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(95, 'stu75', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(96, 'stu76', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(97, 'stu77', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(98, 'stu78', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(99, 'stu79', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2'),
(100, 'stu80', 'Student', '$2a$10$0laRZ7M87tq4jqJ3OsFv7uyxf309qgczyw6Ry.8EVLNvO337Oahs2');

USE erp_db;

-- Instructor profiles
INSERT INTO instructors (user_id, name, department) VALUES
(2, 'Dr. Pankaj Jalote', 'Computer Science'),
(9, 'Dr. Rajiv Kumar', 'Computer Science'),
(10, 'Dr. Priya Sharma', 'Computer Science'),
(11, 'Dr. Amit Singh', 'Electronics and Communication'),
(12, 'Dr. Neha Gupta', 'Mathematics'),
(13, 'Dr. Vikram Patel', 'Computer Science'),
(14, 'Dr. Anjali Verma', 'Electronics and Communication'),
(15, 'Dr. Rahul Mehta', 'Design'),
(16, 'Dr. Kavita Desai', 'Computational Biology'),
(17, 'Dr. Suresh Iyer', 'Social Sciences'),
(18, 'Dr. Meera Nair', 'Computer Science'),
(19, 'Dr. Arun Bhatt', 'Electronics and Communication'),
(20, 'Dr. Pooja Reddy', 'Mathematics'),
(21, 'Dr. Karan Khanna', 'Computer Science'),
(22, 'Dr. Divya Joshi', 'Social Sciences');

-- Student profiles with diverse programs
INSERT INTO students (user_id, roll_no, program, year) VALUES
-- B.Tech CSE (20 students)
(3, '2024001', 'B.Tech CSE', 2),
(23, '2024003', 'B.Tech CSE', 1),
(24, '2024004', 'B.Tech CSE', 1),
(25, '2024005', 'B.Tech CSE', 2),
(26, '2024006', 'B.Tech CSE', 2),
(27, '2024007', 'B.Tech CSE', 2),
(28, '2024008', 'B.Tech CSE', 3),
(29, '2024009', 'B.Tech CSE', 3),
(30, '2024010', 'B.Tech CSE', 3),
(31, '2024011', 'B.Tech CSE', 4),
(32, '2024012', 'B.Tech CSE', 4),
(33, '2023013', 'B.Tech CSE', 2),
(34, '2023014', 'B.Tech CSE', 2),
(35, '2023015', 'B.Tech CSE', 3),
(36, '2023016', 'B.Tech CSE', 3),
(37, '2023017', 'B.Tech CSE', 4),
(38, '2022018', 'B.Tech CSE', 3),
(39, '2022019', 'B.Tech CSE', 3),
(40, '2022020', 'B.Tech CSE', 4),
(41, '2022021', 'B.Tech CSE', 4),
(42, '2021022', 'B.Tech CSE', 4),

-- B.Tech ECE (10 students)
(43, '2024023', 'B.Tech ECE', 1),
(44, '2024024', 'B.Tech ECE', 1),
(45, '2024025', 'B.Tech ECE', 2),
(46, '2024026', 'B.Tech ECE', 2),
(47, '2023027', 'B.Tech ECE', 2),
(48, '2023028', 'B.Tech ECE', 3),
(49, '2023029', 'B.Tech ECE', 3),
(50, '2022030', 'B.Tech ECE', 3),
(51, '2022031', 'B.Tech ECE', 4),
(52, '2021032', 'B.Tech ECE', 4),

-- B.Tech CSAM (10 students)
(4, '2024002', 'B.Tech CSAM', 2),
(53, '2024033', 'B.Tech CSAM', 1),
(54, '2024034', 'B.Tech CSAM', 1),
(55, '2024035', 'B.Tech CSAM', 2),
(56, '2024036', 'B.Tech CSAM', 2),
(57, '2023037', 'B.Tech CSAM', 2),
(58, '2023038', 'B.Tech CSAM', 3),
(59, '2023039', 'B.Tech CSAM', 3),
(60, '2022040', 'B.Tech CSAM', 3),
(61, '2022041', 'B.Tech CSAM', 4),
(62, '2021042', 'B.Tech CSAM', 4),

-- B.Tech CSAI (8 students)
(63, '2024043', 'B.Tech CSAI', 1),
(64, '2024044', 'B.Tech CSAI', 1),
(65, '2024045', 'B.Tech CSAI', 2),
(66, '2023046', 'B.Tech CSAI', 2),
(67, '2023047', 'B.Tech CSAI', 3),
(68, '2023048', 'B.Tech CSAI', 3),
(69, '2022049', 'B.Tech CSAI', 4),
(70, '2022050', 'B.Tech CSAI', 4),

-- B.Tech CSSS (8 students)
(71, '2024051', 'B.Tech CSSS', 1),
(72, '2024052', 'B.Tech CSSS', 1),
(73, '2024053', 'B.Tech CSSS', 2),
(74, '2023054', 'B.Tech CSSS', 2),
(75, '2023055', 'B.Tech CSSS', 3),
(76, '2023056', 'B.Tech CSSS', 3),
(77, '2022057', 'B.Tech CSSS', 4),
(78, '2022058', 'B.Tech CSSS', 4),

-- B.Tech CSB (8 students)
(79, '2024059', 'B.Tech CSB', 1),
(80, '2024060', 'B.Tech CSB', 1),
(81, '2024061', 'B.Tech CSB', 2),
(82, '2023062', 'B.Tech CSB', 2),
(83, '2023063', 'B.Tech CSB', 3),
(84, '2023064', 'B.Tech CSB', 3),
(85, '2022065', 'B.Tech CSB', 4),
(86, '2022066', 'B.Tech CSB', 4),

-- B.Tech CSEcon (6 students)
(87, '2024067', 'B.Tech CSEcon', 1),
(88, '2024068', 'B.Tech CSEcon', 2),
(89, '2023069', 'B.Tech CSEcon', 2),
(90, '2023070', 'B.Tech CSEcon', 3),
(91, '2022071', 'B.Tech CSEcon', 4),
(92, '2022072', 'B.Tech CSEcon', 4),

-- B.Tech CSD (5 students)
(93, '2024073', 'B.Tech CSD', 1),
(94, '2024074', 'B.Tech CSD', 2),
(95, '2023075', 'B.Tech CSD', 2),
(96, '2023076', 'B.Tech CSD', 3),
(97, '2022077', 'B.Tech CSD', 4),

-- B.Tech EVE (5 students)
(98, '2024078', 'B.Tech EVE', 1),
(99, '2024079', 'B.Tech EVE', 2),
(100, '2023080', 'B.Tech EVE', 3);

INSERT INTO courses (code, title, credits) VALUES
-- Computer Science
('CSE101', 'Introduction to Programming', 4),
('CSE102', 'Data Structures and Algorithms', 4),
('CSE112', 'Computer Organization', 4),
('CSE121', 'Discrete Mathematics', 4),
('CSE201', 'Advanced Programming', 4),
('CSE202', 'Fundamentals of Database Management Systems', 4),
('CSE222', 'Algorithm Design and Analysis', 4),
('CSE231', 'Operating Systems', 4),
('CSE232', 'Computer Networks', 4),
('CSE322', 'Theory of Computation', 4),
('CSE412', 'Software Engineering', 4),
('CSE504', 'Artificial Intelligence', 4),
('CSE556', 'Machine Learning', 4),
('CSE564', 'Compiler Design', 4),

-- Electronics and Communication
('ECE111', 'Digital Circuits', 4),
('ECE201', 'Signals and Systems', 4),
('ECE250', 'Electronic Devices', 4),
('ECE270', 'Analog Electronics', 4),
('ECE320', 'Microprocessors and Interfacing', 4),
('ECE350', 'Communication Systems', 4),
('ECE412', 'VLSI Design', 4),
('ECE440', 'Digital Signal Processing', 4),

-- Mathematics
('MTH100', 'Linear Algebra', 4),
('MTH101', 'Calculus', 4),
('MTH201', 'Probability and Statistics', 4),
('MTH301', 'Numerical Methods', 4),
('MTH403', 'Optimization Techniques', 4),

-- Computational Biology
('BIO101', 'Introduction to Biology', 4),
('BIO201', 'Bioinformatics', 4),
('BIO301', 'Computational Biology', 4),
('BIO401', 'Systems Biology', 4),

-- Social Sciences and Humanities
('SSH101', 'Introduction to Economics', 4),
('SSH201', 'Psychology', 4),
('SSH301', 'Sociology', 4),
('SSH401', 'Ethics and Society', 4),
('HUM101', 'Technical Communication', 2),
('HUM201', 'Professional Communication', 2),

-- Design
('DES201', 'Human Computer Interaction', 4),
('DES301', 'User Experience Design', 4),
('DES401', 'Design Thinking', 4),

-- Interdisciplinary
('CSB201', 'Introduction to Computational Biology', 4),
('CSE343', 'Computer Vision', 4),
('CSE471', 'Natural Language Processing', 4),
('CSE543', 'Deep Learning', 4);

INSERT INTO sections (course_id, instructor_id, day_time, room, capacity, semester, year) VALUES
-- CSE Courses
(1, 2, 'Mon-Wed 09:00-10:30', 'C-101', 50, 'Monsoon', 2025),
(1, 9, 'Tue-Thu 11:00-12:30', 'C-102', 50, 'Monsoon', 2025),
(2, 10, 'Mon-Wed 11:00-12:30', 'C-201', 45, 'Monsoon', 2025),
(2, 13, 'Tue-Thu 14:00-15:30', 'C-202', 45, 'Monsoon', 2025),
(3, 21, 'Mon-Wed 14:00-15:30', 'C-103', 40, 'Monsoon', 2025),
(4, 12, 'Tue-Thu 09:00-10:30', 'C-104', 40, 'Monsoon', 2025),
(5, 2, 'Mon-Wed 16:00-17:30', 'C-105', 35, 'Monsoon', 2025),
(6, 18, 'Tue-Thu 16:00-17:30', 'C-106', 35, 'Monsoon', 2025),
(7, 9, 'Mon-Wed 09:00-10:30', 'C-107', 40, 'Monsoon', 2025),
(8, 10, 'Tue-Thu 11:00-12:30', 'C-108', 35, 'Monsoon', 2025),
(9, 13, 'Mon-Wed 14:00-15:30', 'C-109', 35, 'Monsoon', 2025),
(10, 21, 'Tue-Thu 09:00-10:30', 'C-110', 30, 'Monsoon', 2025),
(11, 18, 'Mon-Wed 11:00-12:30', 'C-111', 30, 'Monsoon', 2025),
(12, 2, 'Tue-Thu 14:00-15:30', 'C-112', 35, 'Monsoon', 2025),
(13, 9, 'Mon-Wed 16:00-17:30', 'C-113', 30, 'Monsoon', 2025),
(14, 10, 'Tue-Thu 16:00-17:30', 'C-114', 25, 'Monsoon', 2025),

-- ECE Courses
(15, 11, 'Mon-Wed 09:00-10:30', 'E-101', 40, 'Monsoon', 2025),
(16, 14, 'Tue-Thu 11:00-12:30', 'E-102', 35, 'Monsoon', 2025),
(17, 19, 'Mon-Wed 14:00-15:30', 'E-103', 35, 'Monsoon', 2025),
(18, 11, 'Tue-Thu 09:00-10:30', 'E-104', 30, 'Monsoon', 2025),
(19, 14, 'Mon-Wed 11:00-12:30', 'E-105', 30, 'Monsoon', 2025),
(20, 19, 'Tue-Thu 14:00-15:30', 'E-106', 25, 'Monsoon', 2025),
(21, 11, 'Mon-Wed 16:00-17:30', 'E-107', 25, 'Monsoon', 2025),
(22, 14, 'Tue-Thu 16:00-17:30', 'E-108', 20, 'Monsoon', 2025),

-- Mathematics Courses
(23, 12, 'Mon-Wed 09:00-10:30', 'M-101', 50, 'Monsoon', 2025),
(23, 20, 'Tue-Thu 11:00-12:30', 'M-102', 50, 'Monsoon', 2025),
(24, 12, 'Mon-Wed 11:00-12:30', 'M-103', 45, 'Monsoon', 2025),
(24, 20, 'Tue-Thu 14:00-15:30', 'M-104', 45, 'Monsoon', 2025),
(25, 12, 'Mon-Wed 14:00-15:30', 'M-105', 40, 'Monsoon', 2025),
(26, 20, 'Tue-Thu 09:00-10:30', 'M-106', 35, 'Monsoon', 2025),
(27, 12, 'Mon-Wed 16:00-17:30', 'M-107', 30, 'Monsoon', 2025),

-- Biology Courses
(28, 16, 'Mon-Wed 09:00-10:30', 'B-101', 30, 'Monsoon', 2025),
(29, 16, 'Tue-Thu 11:00-12:30', 'B-102', 25, 'Monsoon', 2025),
(30, 16, 'Mon-Wed 14:00-15:30', 'B-103', 25, 'Monsoon', 2025),
(31, 16, 'Tue-Thu 16:00-17:30', 'B-104', 20, 'Monsoon', 2025),

-- Social Sciences
(32, 17, 'Mon-Wed 09:00-10:30', 'S-101', 40, 'Monsoon', 2025),
(32, 22, 'Tue-Thu 11:00-12:30', 'S-102', 40, 'Monsoon', 2025),
(33, 17, 'Mon-Wed 14:00-15:30', 'S-103', 35, 'Monsoon', 2025),
(34, 22, 'Tue-Thu 14:00-15:30', 'S-104', 35, 'Monsoon', 2025),
(35, 17, 'Mon-Wed 16:00-17:30', 'S-105', 30, 'Monsoon', 2025),
(36, 22, 'Tue-Thu 09:00-10:30', 'LR-101', 50, 'Monsoon', 2025),
(37, 22, 'Mon-Wed 11:00-12:30', 'LR-102', 45, 'Monsoon', 2025),

-- Design Courses
(38, 15, 'Tue-Thu 14:00-15:30', 'D-101', 30, 'Monsoon', 2025),
(39, 15, 'Mon-Wed 16:00-17:30', 'D-102', 25, 'Monsoon', 2025),
(40, 15, 'Tue-Thu 16:00-17:30', 'D-103', 25, 'Monsoon', 2025),

-- Interdisciplinary
(41, 16, 'Mon-Wed 11:00-12:30', 'B-105', 25, 'Monsoon', 2025),
(42, 18, 'Tue-Thu 09:00-10:30', 'C-115', 30, 'Monsoon', 2025),
(43, 9, 'Mon-Wed 14:00-15:30', 'C-116', 30, 'Monsoon', 2025),
(44, 13, 'Tue-Thu 16:00-17:30', 'C-117', 25, 'Monsoon', 2025);

-- Year 1 CSE students (23-24) - Basic courses
INSERT INTO enrollments (student_id, section_id, status) VALUES
(23, 1, 'Enrolled'), (23, 29, 'Enrolled'), (23, 33, 'Enrolled'), (23, 36, 'Enrolled'),
(24, 2, 'Enrolled'), (24, 30, 'Enrolled'), (24, 33, 'Enrolled'), (24, 36, 'Enrolled');

-- Year 2 CSE students (3, 4, 25-27, 33-34) - Intermediate courses
INSERT INTO enrollments (student_id, section_id, status) VALUES
(3, 3, 'Enrolled'), (3, 5, 'Enrolled'), (3, 31, 'Enrolled'), (3, 38, 'Enrolled'),
(4, 4, 'Enrolled'), (4, 6, 'Enrolled'), (4, 32, 'Enrolled'), (4, 38, 'Enrolled'),
(25, 3, 'Enrolled'), (25, 7, 'Enrolled'), (25, 31, 'Enrolled'),
(26, 4, 'Enrolled'), (26, 8, 'Enrolled'), (26, 32, 'Enrolled'),
(27, 3, 'Enrolled'), (27, 5, 'Enrolled'), (27, 31, 'Enrolled'),
(33, 4, 'Enrolled'), (33, 6, 'Enrolled'), (33, 32, 'Enrolled'),
(34, 3, 'Enrolled'), (34, 7, 'Enrolled'), (34, 31, 'Enrolled');

-- Year 3 CSE students (28-30, 35-36, 38-39) - Advanced courses
INSERT INTO enrollments (student_id, section_id, status) VALUES
(28, 9, 'Enrolled'), (28, 11, 'Enrolled'), (28, 12, 'Enrolled'), (28, 42, 'Enrolled'),
(29, 10, 'Enrolled'), (29, 11, 'Enrolled'), (29, 12, 'Enrolled'), (29, 43, 'Enrolled'),
(30, 9, 'Enrolled'), (30, 11, 'Enrolled'), (30, 13, 'Enrolled'), (30, 42, 'Enrolled'),
(35, 10, 'Enrolled'), (35, 11, 'Enrolled'), (35, 13, 'Enrolled'), (35, 43, 'Enrolled'),
(36, 9, 'Enrolled'), (36, 11, 'Enrolled'), (36, 12, 'Enrolled'),
(38, 10, 'Enrolled'), (38, 11, 'Enrolled'), (38, 13, 'Enrolled'),
(39, 9, 'Enrolled'), (39, 11, 'Enrolled'), (39, 12, 'Enrolled');

-- Year 4 CSE students (31-32, 37, 40-42) - Electives
INSERT INTO enrollments (student_id, section_id, status) VALUES
(31, 12, 'Enrolled'), (31, 13, 'Enrolled'), (31, 14, 'Enrolled'), (31, 44, 'Enrolled'),
(32, 12, 'Enrolled'), (32, 13, 'Enrolled'), (32, 14, 'Enrolled'),
(37, 12, 'Enrolled'), (37, 13, 'Enrolled'), (37, 14, 'Enrolled'),
(40, 12, 'Enrolled'), (40, 13, 'Enrolled'), (40, 44, 'Enrolled'),
(41, 12, 'Enrolled'), (41, 13, 'Enrolled'), (41, 14, 'Enrolled'),
(42, 12, 'Enrolled'), (42, 13, 'Enrolled'), (42, 14, 'Enrolled');

-- ECE students (43-52)
INSERT INTO enrollments (student_id, section_id, status) VALUES
(43, 15, 'Enrolled'), (43, 29, 'Enrolled'), (43, 33, 'Enrolled'),
(44, 15, 'Enrolled'), (44, 30, 'Enrolled'), (44, 33, 'Enrolled'),
(45, 16, 'Enrolled'), (45, 18, 'Enrolled'), (45, 31, 'Enrolled'),
(46, 16, 'Enrolled'), (46, 18, 'Enrolled'), (46, 32, 'Enrolled'),
(47, 17, 'Enrolled'), (47, 19, 'Enrolled'), (47, 31, 'Enrolled'),
(48, 17, 'Enrolled'), (48, 19, 'Enrolled'), (48, 20, 'Enrolled'),
(49, 18, 'Enrolled'), (49, 20, 'Enrolled'), (49, 21, 'Enrolled'),
(50, 18, 'Enrolled'), (50, 20, 'Enrolled'), (50, 21, 'Enrolled'),
(51, 21, 'Enrolled'), (51, 22, 'Enrolled'), (51, 27, 'Enrolled'),
(52, 21, 'Enrolled'), (52, 22, 'Enrolled'), (52, 27, 'Enrolled');

-- CSAM students (53-62)
INSERT INTO enrollments (student_id, section_id, status) VALUES
(53, 1, 'Enrolled'), (53, 29, 'Enrolled'), (53, 33, 'Enrolled'),
(54, 2, 'Enrolled'), (54, 30, 'Enrolled'), (54, 33, 'Enrolled'),
(55, 3, 'Enrolled'), (55, 31, 'Enrolled'), (55, 5, 'Enrolled'),
(56, 4, 'Enrolled'), (56, 32, 'Enrolled'), (56, 6, 'Enrolled'),
(57, 3, 'Enrolled'), (57, 31, 'Enrolled'), (57, 7, 'Enrolled'),
(58, 7, 'Enrolled'), (58, 9, 'Enrolled'), (58, 26, 'Enrolled'),
(59, 7, 'Enrolled'), (59, 10, 'Enrolled'), (59, 26, 'Enrolled'),
(60, 9, 'Enrolled'), (60, 11, 'Enrolled'), (60, 27, 'Enrolled'),
(61, 12, 'Enrolled'), (61, 13, 'Enrolled'), (61, 27, 'Enrolled'),
(62, 12, 'Enrolled'), (62, 13, 'Enrolled'), (62, 27, 'Enrolled');

-- CSAI students (63-70)
INSERT INTO enrollments (student_id, section_id, status) VALUES
(63, 1, 'Enrolled'), (63, 29, 'Enrolled'), (63, 33, 'Enrolled'),
(64, 2, 'Enrolled'), (64, 30, 'Enrolled'), (64, 33, 'Enrolled'),
(65, 3, 'Enrolled'), (65, 5, 'Enrolled'), (65, 31, 'Enrolled'),
(66, 4, 'Enrolled'), (66, 6, 'Enrolled'), (66, 32, 'Enrolled'),
(67, 9, 'Enrolled'), (67, 12, 'Enrolled'), (67, 13, 'Enrolled'),
(68, 10, 'Enrolled'), (68, 12, 'Enrolled'), (68, 13, 'Enrolled'),
(69, 12, 'Enrolled'), (69, 13, 'Enrolled'), (69, 44, 'Enrolled'),
(70, 12, 'Enrolled'), (70, 13, 'Enrolled'), (70, 44, 'Enrolled');

-- CSSS students (71-78)
INSERT INTO enrollments (student_id, section_id, status) VALUES
(71, 1, 'Enrolled'), (71, 29, 'Enrolled'), (71, 33, 'Enrolled'),
(72, 2, 'Enrolled'), (72, 30, 'Enrolled'), (72, 34, 'Enrolled'),
(73, 3, 'Enrolled'), (73, 31, 'Enrolled'), (73, 35, 'Enrolled'),
(74, 4, 'Enrolled'), (74, 32, 'Enrolled'), (74, 35, 'Enrolled'),
(75, 7, 'Enrolled'), (75, 9, 'Enrolled'), (75, 34, 'Enrolled'),
(76, 7, 'Enrolled'), (76, 10, 'Enrolled'), (76, 35, 'Enrolled'),
(77, 12, 'Enrolled'), (77, 13, 'Enrolled'), (77, 35, 'Enrolled'),
(78, 12, 'Enrolled'), (78, 13, 'Enrolled'), (78, 35, 'Enrolled');

-- CSB students (79-86)
INSERT INTO enrollments (student_id, section_id, status) VALUES
(79, 1, 'Enrolled'), (79, 28, 'Enrolled'), (79, 29, 'Enrolled'),
(80, 2, 'Enrolled'), (80, 28, 'Enrolled'), (80, 30, 'Enrolled'),
(81, 3, 'Enrolled'), (81, 29, 'Enrolled'), (81, 41, 'Enrolled'),
(82, 4, 'Enrolled'), (82, 30, 'Enrolled'), (82, 41, 'Enrolled'),
(83, 7, 'Enrolled'), (83, 30, 'Enrolled'), (83, 41, 'Enrolled'),
(84, 7, 'Enrolled'), (84, 31, 'Enrolled'), (84, 41, 'Enrolled'),
(85, 12, 'Enrolled'), (85, 30, 'Enrolled'), (85, 31, 'Enrolled'),
(86, 12, 'Enrolled'), (86, 30, 'Enrolled'), (86, 31, 'Enrolled');

-- CSEcon students (87-92)
INSERT INTO enrollments (student_id, section_id, status) VALUES
(87, 1, 'Enrolled'), (87, 29, 'Enrolled'), (87, 33, 'Enrolled'),
(88, 3, 'Enrolled'), (88, 31, 'Enrolled'), (88, 33, 'Enrolled'),
(89, 4, 'Enrolled'), (89, 32, 'Enrolled'), (89, 33, 'Enrolled'),
(90, 7, 'Enrolled'), (90, 9, 'Enrolled'), (90, 33, 'Enrolled'),
(91, 12, 'Enrolled'), (91, 13, 'Enrolled'), (91, 33, 'Enrolled'),
(92, 12, 'Enrolled'), (92, 13, 'Enrolled'), (92, 35, 'Enrolled');

-- CSD students (93-97)
INSERT INTO enrollments (student_id, section_id, status) VALUES
(93, 1, 'Enrolled'), (93, 29, 'Enrolled'), (93, 38, 'Enrolled'),
(94, 3, 'Enrolled'), (94, 31, 'Enrolled'), (94, 38, 'Enrolled'),
(95, 4, 'Enrolled'), (95, 32, 'Enrolled'), (95, 38, 'Enrolled'),
(96, 7, 'Enrolled'), (96, 9, 'Enrolled'), (96, 38, 'Enrolled'),
(97, 12, 'Enrolled'), (97, 13, 'Enrolled'), (97, 38, 'Enrolled');

-- EVE students (98-100)
INSERT INTO enrollments (student_id, section_id, status) VALUES
(98, 1, 'Enrolled'), (98, 15, 'Enrolled'), (98, 29, 'Enrolled'),
(99, 3, 'Enrolled'), (99, 16, 'Enrolled'), (99, 31, 'Enrolled'),
(100, 7, 'Enrolled'), (100, 17, 'Enrolled'), (100, 9, 'Enrolled');

INSERT INTO settings (setting_key, setting_value) VALUES
('maintenance_on', 'false'),
('current_semester', 'Monsoon'),
('current_year', '2025'),
('drop_deadline', '2025-12-15'),
('registration_deadline', '2025-12-30');

-- ========================================
-- Summary Statistics
-- ========================================
-- Total Users: 100 (5 Admin, 15 Instructor, 80 Student)
-- Total Courses: 44
-- Total Sections: 48
-- Total Enrollments: 200+
-- Programs Distribution:
--   B.Tech CSE: 20 students
--   B.Tech ECE: 10 students
--   B.Tech CSAM: 10 students
--   B.Tech CSAI: 8 students
--   B.Tech CSSS: 8 students
--   B.Tech CSB: 8 students
--   B.Tech CSEcon: 6 students
--   B.Tech CSD: 5 students
--   B.Tech EVE: 3 students
-- ========================================

-- Password for all users: pass123
-- Use these credentials to login:
-- Admins: admin1, admin2, admin3, admin4, admin5
-- Instructors: inst1 through inst15
-- Students: stu1 through stu80