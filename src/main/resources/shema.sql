SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS reponses;
DROP TABLE IF EXISTS resultats;
DROP TABLE IF EXISTS quiz_session;
DROP TABLE IF EXISTS quiz_students;
DROP TABLE IF EXISTS question;
DROP TABLE IF EXISTS quiz;
DROP TABLE IF EXISTS matieres;
DROP TABLE IF EXISTS classe_enseignants;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS classes;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE classes (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         name VARCHAR(100) NOT NULL,
                         filiere VARCHAR(150),
                         niveau VARCHAR(100),
                         enseignant_id BIGINT NOT NULL,
                         created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       first_name VARCHAR(100) NOT NULL,
                       last_name VARCHAR(100) NOT NULL,
                       email VARCHAR(190) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       role VARCHAR(20) NOT NULL,
                       must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
                       blocked BOOLEAN NOT NULL DEFAULT FALSE,
                       cne VARCHAR(100) UNIQUE,
                       code_apoge VARCHAR(100) UNIQUE,
                       classe_id BIGINT NULL,
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                       updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       CONSTRAINT fk_user_classe FOREIGN KEY (classe_id) REFERENCES classes(id) ON DELETE SET NULL
) ENGINE=InnoDB;

ALTER TABLE classes
    ADD CONSTRAINT fk_classes_enseignant
        FOREIGN KEY (enseignant_id) REFERENCES users(id) ON DELETE RESTRICT;

CREATE TABLE classe_enseignants (
                                    classe_id BIGINT NOT NULL,
                                    enseignant_id BIGINT NOT NULL,
                                    PRIMARY KEY (classe_id, enseignant_id),
                                    CONSTRAINT fk_classe_enseignants_classe FOREIGN KEY (classe_id) REFERENCES classes(id) ON DELETE CASCADE,
                                    CONSTRAINT fk_classe_enseignants_enseignant FOREIGN KEY (enseignant_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE matieres (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          nom VARCHAR(150) NOT NULL,
                          description VARCHAR(800),
                          classe_id BIGINT NOT NULL,
                          enseignant_id BIGINT NOT NULL,
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                          CONSTRAINT fk_matieres_classe FOREIGN KEY (classe_id) REFERENCES classes(id) ON DELETE CASCADE,
                          CONSTRAINT fk_matieres_enseignant FOREIGN KEY (enseignant_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE quiz (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      titre VARCHAR(100) NOT NULL,
                      theme VARCHAR(100),
                      question_count INT DEFAULT 0,
                      available_from DATETIME,
                      available_until DATETIME,
                      time_limit INT,
                      status VARCHAR(20) DEFAULT 'DRAFT',
                      creation_type VARCHAR(20) DEFAULT 'MANUAL',
                      id_enseignant BIGINT NOT NULL,
                      classe_id BIGINT NULL,
                      matiere_id BIGINT NULL,
                      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                      deleted_at DATETIME NULL,
                      deleted_by BIGINT NULL,
                      CONSTRAINT fk_quiz_enseignant FOREIGN KEY (id_enseignant) REFERENCES users(id) ON DELETE CASCADE,
                      CONSTRAINT fk_quiz_classe FOREIGN KEY (classe_id) REFERENCES classes(id) ON DELETE SET NULL,
                      CONSTRAINT fk_quiz_matiere FOREIGN KEY (matiere_id) REFERENCES matieres(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE question (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          enonce TEXT NOT NULL,
                          choixa VARCHAR(100),
                          choixb VARCHAR(100),
                          choixc VARCHAR(100),
                          choixd VARCHAR(100),
                          reponse_correcte VARCHAR(200),
                          points INT DEFAULT 1,
                          type VARCHAR(20) DEFAULT 'MCQ',
                          id_quiz BIGINT NOT NULL,
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                          CONSTRAINT fk_question_quiz FOREIGN KEY (id_quiz) REFERENCES quiz(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE quiz_students (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               quiz_id BIGINT NOT NULL,
                               student_id BIGINT NOT NULL,
                               added_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                               UNIQUE KEY uk_quiz_student (quiz_id, student_id),
                               CONSTRAINT fk_quiz_students_quiz FOREIGN KEY (quiz_id) REFERENCES quiz(id) ON DELETE CASCADE,
                               CONSTRAINT fk_quiz_students_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE quiz_session (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              quiz_id BIGINT NOT NULL,
                              student_id BIGINT NOT NULL,
                              start_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                              last_activity DATETIME,
                              status VARCHAR(20) DEFAULT 'ACTIVE',
                              UNIQUE KEY uk_session_student_quiz (student_id, quiz_id),
                              CONSTRAINT fk_session_quiz FOREIGN KEY (quiz_id) REFERENCES quiz(id) ON DELETE CASCADE,
                              CONSTRAINT fk_session_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE resultats (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           quiz_id BIGINT NOT NULL,
                           student_id BIGINT NOT NULL,
                           score DOUBLE,
                           total_points INT,
                           earned_points INT,
                           score_percentage DOUBLE,
                           is_completed BOOLEAN DEFAULT FALSE,
                           feedback_ia TEXT,
                           strengths TEXT,
                           weaknesses TEXT,
                           recommendations TEXT,
                           suggested_quiz VARCHAR(255),
                           grade VARCHAR(20),
                           started_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                           completed_date DATETIME,
                           status VARCHAR(20) DEFAULT 'IN_PROGRESS',
                           UNIQUE KEY uk_resultat_student_quiz (student_id, quiz_id),
                           CONSTRAINT fk_resultats_quiz FOREIGN KEY (quiz_id) REFERENCES quiz(id) ON DELETE CASCADE,
                           CONSTRAINT fk_resultats_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE reponses (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          quiz_id BIGINT NOT NULL,
                          question_id BIGINT NOT NULL,
                          student_id BIGINT NOT NULL,
                          student_answer TEXT,
                          is_correct BOOLEAN,
                          points_earned INT,
                          answered_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                          UNIQUE KEY uk_reponse_student_question_quiz (student_id, question_id, quiz_id),
                          CONSTRAINT fk_reponses_quiz FOREIGN KEY (quiz_id) REFERENCES quiz(id) ON DELETE CASCADE,
                          CONSTRAINT fk_reponses_question FOREIGN KEY (question_id) REFERENCES question(id) ON DELETE CASCADE,
                          CONSTRAINT fk_reponses_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
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