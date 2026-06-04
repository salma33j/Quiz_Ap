-- =========================================================
-- Quiz App - Schema MySQL 8+ 
-- Spring Boot 3.x / JPA
-- =========================================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS reponses;
DROP TABLE IF EXISTS resultats;
DROP TABLE IF EXISTS quiz_students;
DROP TABLE IF EXISTS question;
DROP TABLE IF EXISTS quiz;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

-- =========================================================
-- 1) USERS (Table principale des utilisateurs)
-- =========================================================
CREATE TABLE users (
                       id BIGINT NOT NULL AUTO_INCREMENT,
                       first_name VARCHAR(100) NOT NULL,
                       last_name VARCHAR(100) NOT NULL,
                       email VARCHAR(190) NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       role VARCHAR(20) NOT NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       PRIMARY KEY (id),
                       CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB;

-- =========================================================
-- 2) QUIZ (Table des quiz créés par les enseignants)
-- =========================================================
CREATE TABLE quiz (
                      id BIGINT NOT NULL AUTO_INCREMENT,
                      titre VARCHAR(100) NOT NULL,
                      theme VARCHAR(100),
                      question_count INT DEFAULT 0,
                      available_from DATETIME,
                      available_until DATETIME,
                      time_limit INT,
                      status VARCHAR(20) DEFAULT 'DRAFT',
                      creation_type VARCHAR(20) DEFAULT 'MANUAL',
                      id_enseignant BIGINT NOT NULL,
                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                      deleted_at TIMESTAMP NULL,
                      deleted_by BIGINT NULL,
                      PRIMARY KEY (id),
                      KEY idx_quiz_enseignant (id_enseignant),
                      KEY idx_quiz_status (status),
                      CONSTRAINT fk_quiz_enseignant FOREIGN KEY (id_enseignant) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 3) QUESTION (Table des questions)
-- =========================================================
CREATE TABLE question (
                          id_question BIGINT NOT NULL AUTO_INCREMENT,
                          enonce TEXT NOT NULL,
                          choixA VARCHAR(100),
                          choixB VARCHAR(100),
                          choixC VARCHAR(100),
                          choixD VARCHAR(100),
                          reponse_correcte TEXT NOT NULL,
                          points INT DEFAULT 1,
                          type VARCHAR(20) DEFAULT 'MCQ',
                          id_quiz BIGINT NOT NULL,
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          PRIMARY KEY (id_question),
                          KEY idx_question_quiz (id_quiz),
                          CONSTRAINT fk_question_quiz FOREIGN KEY (id_quiz) REFERENCES quiz(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 4) QUIZ_STUDENTS (Table des étudiants autorisés par quiz)
-- =========================================================
CREATE TABLE quiz_students (
                               id BIGINT NOT NULL AUTO_INCREMENT,
                               quiz_id BIGINT NOT NULL,
                               student_id BIGINT NOT NULL,
                               added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               PRIMARY KEY (id),
                               UNIQUE KEY uk_quiz_student (quiz_id, student_id),
                               KEY idx_quiz_students_quiz (quiz_id),
                               KEY idx_quiz_students_student (student_id),
                               CONSTRAINT fk_quiz_students_quiz FOREIGN KEY (quiz_id) REFERENCES quiz(id) ON DELETE CASCADE,
                               CONSTRAINT fk_quiz_students_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 5) RESULTATS (Table des résultats des quiz)
-- =========================================================
CREATE TABLE resultats (
                           id BIGINT NOT NULL AUTO_INCREMENT,
                           student_id BIGINT NOT NULL,
                           quiz_id BIGINT NOT NULL,
                           total_points INT,
                           earned_points INT,
                           score_percentage DOUBLE,
                           score DOUBLE,
                           is_completed BOOLEAN DEFAULT FALSE,
                           feedback_ia TEXT,
                           strengths TEXT,
                           weaknesses TEXT,
                           recommendations TEXT,
                           suggested_quiz VARCHAR(100),
                           grade VARCHAR(5),
                           started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           completed_date TIMESTAMP NULL,
                           status VARCHAR(20) DEFAULT 'IN_PROGRESS',
                           PRIMARY KEY (id),
                           UNIQUE KEY uk_student_quiz (student_id, quiz_id),
                           KEY idx_resultats_student (student_id),
                           KEY idx_resultats_quiz (quiz_id),
                           KEY idx_resultats_status (status),
                           CONSTRAINT fk_resultats_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
                           CONSTRAINT fk_resultats_quiz FOREIGN KEY (quiz_id) REFERENCES quiz(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 6) REPONSES (Table des réponses des étudiants)
-- =========================================================
CREATE TABLE reponses (
                          id BIGINT NOT NULL AUTO_INCREMENT,
                          student_id BIGINT NOT NULL,
                          question_id BIGINT NOT NULL,
                          student_answer TEXT,
                          is_correct BOOLEAN,
                          points_earned INT,
                          answered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          PRIMARY KEY (id),
                          UNIQUE KEY uk_student_question (student_id, question_id),
                          KEY idx_reponses_student (student_id),
                          KEY idx_reponses_question (question_id),
                          CONSTRAINT fk_reponses_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
                          CONSTRAINT fk_reponses_question FOREIGN KEY (question_id) REFERENCES question(id_question) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 7) INSERTS DES DONNÉES INITIALES (DataInitializer)
-- =========================================================

-- Mot de passe encodé: "admin123" (BCrypt)
INSERT INTO users (first_name, last_name, email, password, role) VALUES
    ('Admin', 'System', 'admin@quizapp.com', '$2a$10$N.Zu9TqKQFqyUQxKqv4bWOqXQZBqXQFqyUQxKqv4bWOqXQZBqXQ', 'ADMIN');

-- Mot de passe encodé: "teacher123"
INSERT INTO users (first_name, last_name, email, password, role) VALUES
                                                                     ('Professeur', 'Alpha', 'prof.alpha@quizapp.com', '$2a$10$N.Zu9TqKQFqyUQxKqv4bWOqXQZBqXQFqyUQxKqv4bWOqXQZBqXQ', 'ENSEIGNANT'),
                                                                     ('Professeur', 'Beta', 'prof.beta@quizapp.com', '$2a$10$N.Zu9TqKQFqyUQxKqv4bWOqXQZBqXQFqyUQxKqv4bWOqXQZBqXQ', 'ENSEIGNANT');

-- Mot de passe encodé: "student123"
INSERT INTO users (first_name, last_name, email, password, role) VALUES
                                                                     ('Jean', 'Dupont', 'jean.dupont@etu.quizapp.com', '$2a$10$N.Zu9TqKQFqyUQxKqv4bWOqXQZBqXQFqyUQxKqv4bWOqXQZBqXQ', 'ETUDIANT'),
                                                                     ('Marie', 'Martin', 'marie.martin@etu.quizapp.com', '$2a$10$N.Zu9TqKQFqyUQxKqv4bWOqXQZBqXQFqyUQxKqv4bWOqXQZBqXQ', 'ETUDIANT'),
                                                                     ('Karim', 'Benali', 'karim.benali@etu.quizapp.com', '$2a$10$N.Zu9TqKQFqyUQxKqv4bWOqXQZBqXQFqyUQxKqv4bWOqXQZBqXQ', 'ETUDIANT');

-- =========================================================
-- 8) EXEMPLE DE QUIZ DE DÉMONSTRATION
-- =========================================================

-- Insérer un quiz (id_enseignant = 2 = Professeur Alpha)
INSERT INTO quiz (titre, theme, question_count, available_from, available_until, time_limit, status, creation_type, id_enseignant) VALUES
    ('Java Programming Basics', 'Programmation Java', 3, '2026-05-01 00:00:00', '2026-12-31 23:59:59', 30, 'PUBLISHED', 'MANUAL', 2);

-- Insérer des questions pour le quiz
INSERT INTO question (enonce, choixA, choixB, choixC, choixD, reponse_correcte, points, type, id_quiz) VALUES
                                                                                                           ('Qu''est-ce qu''une classe en Java ?', 'Un plan pour créer des objets', 'Une fonction', 'Une variable', 'Un package', 'A', 5, 'MCQ', 1),
                                                                                                           ('Quel mot-clé est utilisé pour hériter d''une classe ?', 'implement', 'extends', 'import', 'package', 'B', 5, 'MCQ', 1),
                                                                                                           ('Que signifie JVM ?', 'Java Virtual Machine', 'Java Variable Method', 'Java Version Manager', 'Just Very Minimal', 'A', 5, 'MCQ', 1);

-- Autoriser les étudiants à répondre au quiz
INSERT INTO quiz_students (quiz_id, student_id) VALUES
                                                    (1, 4),  -- Jean Dupont
                                                    (1, 5),  -- Marie Martin
                                                    (1, 6);  -- Karim Benali

-- =========================================================
-- 9) STATISTIQUES RÉCAPITULATIVES
-- =========================================================

-- Afficher le nombre d'utilisateurs par rôle
SELECT '=== STATISTIQUES UTILISATEURS ===' AS '';
SELECT role, COUNT(*) as total FROM users GROUP BY role;

-- Afficher le nombre de quiz par statut
SELECT '=== STATISTIQUES QUIZ ===' AS '';
SELECT status, COUNT(*) as total FROM quiz GROUP BY status;

-- Afficher le nombre de questions par quiz
SELECT '=== STATISTIQUES QUESTIONS ===' AS '';
SELECT q.id, q.titre, COUNT(qt.id_question) as nb_questions
FROM quiz q
         LEFT JOIN question qt ON q.id = qt.id_quiz
GROUP BY q.id, q.titre;

-- =========================================================
-- FIN DU SCRIPT
-- =========================================================