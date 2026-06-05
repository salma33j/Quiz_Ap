import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  ArrowLeft,
  BookOpen,
  CalendarClock,
  ChevronRight,
  Clock,
  FileQuestion,
} from "lucide-react";

import studentQuizApi from "../../api/studentQuizApi";
import styles from "./QuizCorrections.module.css";

const getSubjectName = (source) =>
  source?.matiereName ||
  source?.matiereNom ||
  source?.subjectName ||
  source?.nom ||
  source?.name ||
  source?.matiere?.nom ||
  source?.subject?.name ||
  source?.quizTheme ||
  source?.theme ||
  "Matière générale";

const getSubjectId = (source) =>
  source?.matiereId ||
  source?.subjectId ||
  source?.matiere?.id ||
  source?.subject?.id ||
  source?.id ||
  getSubjectName(source);

const getQuizSubjectId = (quiz) =>
  quiz?.matiereId ||
  quiz?.subjectId ||
  quiz?.matiere?.id ||
  quiz?.subject?.id ||
  getSubjectName(quiz);

const getQuizId = (quiz) =>
  quiz?.quizId || quiz?.idQuiz || quiz?.quiz?.id || quiz?.id;

const getQuizTitle = (quiz) =>
  quiz?.quizTitle || quiz?.titre || quiz?.title || quiz?.quiz?.titre || "Quiz sans titre";

const getClassLabel = (source) =>
  [
    source?.className || source?.classeName,
    source?.classFiliere,
    source?.classNiveau,
  ]
    .filter(Boolean)
    .join(" - ") || "Classe non définie";

const getTeacherLabel = (source) =>
  source?.teacherName ||
  source?.enseignantName ||
  source?.enseignantNom ||
  source?.profName ||
  source?.professeurName ||
  source?.teacher?.name ||
  source?.enseignant?.name ||
  [source?.teacher?.firstName, source?.teacher?.lastName].filter(Boolean).join(" ") ||
  [source?.enseignant?.firstName, source?.enseignant?.lastName].filter(Boolean).join(" ") ||
  "Professeur non défini";

const getScoreLabel = (quiz) => {
  if (quiz?.noteSur20 != null) {
    return `${Number(quiz.noteSur20).toFixed(2)} / 20`;
  }

  if (quiz?.earnedPoints != null && quiz?.totalPoints != null) {
    return `${quiz.earnedPoints} / ${quiz.totalPoints}`;
  }

  const score = Number(quiz?.scorePercentage ?? quiz?.score ?? quiz?.percentage ?? 0);
  return `${Math.round(Number.isFinite(score) ? score : 0)}%`;
};

const getQuestionCount = (quiz) =>
  quiz?.questionCount ||
  quiz?.questionsCount ||
  quiz?.totalQuestions ||
  quiz?.questions?.length ||
  "—";

const getDuration = (quiz) =>
  quiz?.timeLimit ||
  quiz?.durationMinutes ||
  quiz?.dureeMinutes ||
  quiz?.duration ||
  "—";

const formatDateTime = (value) => {
  if (!value) return "Date non définie";

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Date non définie";

  return date.toLocaleString("fr-FR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};

export default function QuizCorrections() {
  const navigate = useNavigate();

  const [matieres, setMatieres] = useState([]);
  const [history, setHistory] = useState([]);
  const [quizDetails, setQuizDetails] = useState({});
  const [selectedSubjectId, setSelectedSubjectId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      setError("");

      const [matieresData, historyRes] = await Promise.all([
        studentQuizApi.getMySubjects().catch(() => []),
        studentQuizApi.getMyResultsHistory(),
      ]);

      const historyData = historyRes;
      const completed = Array.isArray(historyData)
        ? historyData.filter((item) => item.isCompleted !== false)
        : [];

      setMatieres(Array.isArray(matieresData) ? matieresData : []);
      setHistory(completed);

      const detailsEntries = await Promise.all(
        completed
          .filter((item) => getQuizId(item))
          .map(async (item) => {
            const quizId = getQuizId(item);

            try {
              const details = await studentQuizApi.getQuizDetails(quizId);
              return [String(quizId), details];
            } catch {
              return [String(quizId), null];
            }
          })
      );

      setQuizDetails(Object.fromEntries(detailsEntries));
    } catch (err) {
      console.error("Erreur corrections:", err);
      setError(
        err?.response?.data?.message ||
          err?.message ||
          "Impossible de charger les corrections."
      );
    } finally {
      setLoading(false);
    }
  };

  const subjects = useMemo(() => {
    const grouped = new Map();

    matieres.forEach((matiere) => {
      const id = String(getSubjectId(matiere));

      grouped.set(id, {
        id,
        name: getSubjectName(matiere),
        classLabel: getClassLabel(matiere),
        teacherName: getTeacherLabel(matiere),
        quizzes: [],
      });
    });

    history.forEach((historyItem) => {
      const quizId = getQuizId(historyItem);
      const details = quizDetails[String(quizId)] || {};
      const mergedQuiz = { ...historyItem, ...details };

      const subjectId = String(getQuizSubjectId(mergedQuiz));

      if (!grouped.has(subjectId)) {
        grouped.set(subjectId, {
          id: subjectId,
          name: getSubjectName(mergedQuiz),
          classLabel: getClassLabel(mergedQuiz),
          teacherName: getTeacherLabel(mergedQuiz),
          quizzes: [],
        });
      }

      const subject = grouped.get(subjectId);

      if (!subject.teacherName || subject.teacherName === "Professeur non défini") {
        subject.teacherName = getTeacherLabel(mergedQuiz);
      }

      if (!subject.classLabel || subject.classLabel === "Classe non définie") {
        subject.classLabel = getClassLabel(mergedQuiz);
      }

      subject.quizzes.push(mergedQuiz);
    });

    return Array.from(grouped.values()).sort((a, b) =>
      a.name.localeCompare(b.name)
    );
  }, [matieres, history, quizDetails]);

  const selectedSubject = useMemo(
    () => subjects.find((subject) => String(subject.id) === String(selectedSubjectId)),
    [subjects, selectedSubjectId]
  );

  if (loading) {
    return <div className={styles.loading}>Chargement des corrections...</div>;
  }

  if (error) {
    return <div className={styles.errorBox}>{error}</div>;
  }

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <span className={styles.badge}>Corrections</span>

        <h1>
          {selectedSubject ? selectedSubject.name : "Choisissez une matière"}
        </h1>

        <p>
          {selectedSubject
            ? selectedSubject.quizzes.length > 0
              ? "Choisissez un quiz corrigé pour consulter votre résultat complet."
              : "Cette matière est bien assignée à votre classe, mais aucun quiz corrigé n'est disponible pour le moment."
            : "Chaque rectangle représente une matière assignée à votre classe par l'admin. Les quiz déjà répondus apparaissent dans leur matière."}
        </p>
      </div>

      {!selectedSubject ? (
        subjects.length === 0 ? (
          <div className={styles.empty}>
            Aucune matière assignée à votre classe pour le moment.
          </div>
        ) : (
          <div className={styles.subjectGrid}>
            {subjects.map((subject) => (
              <button
                key={subject.id}
                type="button"
                className={styles.subjectCard}
                onClick={() => setSelectedSubjectId(subject.id)}
              >
                <div className={styles.subjectIcon}>
                  <BookOpen size={30} />
                </div>

                <div>
                  <h2>{subject.name}</h2>
                  <strong>Prof : {subject.teacherName}</strong>
                  <small>{subject.classLabel}</small>
                  <p>{subject.quizzes.length} quiz corrigé(s)</p>
                </div>

                <ChevronRight size={22} />
              </button>
            ))}
          </div>
        )
      ) : (
        <>
          <button
            type="button"
            className={styles.backButton}
            onClick={() => setSelectedSubjectId(null)}
            aria-label="Retour aux matières"
          >
            <ArrowLeft size={18} />
          </button>

          {selectedSubject.quizzes.length === 0 ? (
            <div className={styles.empty}>
              Aucun quiz corrigé dans cette matière pour le moment.
            </div>
          ) : (
            <div className={styles.quizGrid}>
              {selectedSubject.quizzes.map((quiz, index) => {
                const quizId = getQuizId(quiz);

                return (
                  <article className={styles.quizCard} key={quizId || index}>
                    <div className={styles.quizPreviewTop}>
                      <div className={styles.quizIntro}>
                        <h2>{getQuizTitle(quiz)}</h2>
                        <p>Prévisualisation du résultat avant l’ouverture.</p>
                      </div>

                      <button
                        type="button"
                        className={styles.startButton}
                        onClick={() => navigate(`/student/quizzes/${quizId}/result`)}
                        disabled={!quizId}
                      >
                        Voir résultats
                      </button>
                    </div>

                    <div className={styles.infoGrid}>
                      <div>
                        <BookOpen size={20} />
                        <span>{getSubjectName(quiz)}</span>
                      </div>

                      <div>
                        <FileQuestion size={20} />
                        <span>{getQuestionCount(quiz)} questions</span>
                      </div>

                      <div>
                        <Clock size={20} />
                        <span>Score : {getScoreLabel(quiz)}</span>
                      </div>

                      <div>
                        <CalendarClock size={20} />
                        <span>
                          {formatDateTime(
                            quiz.completedDate ||
                              quiz.submittedAt ||
                              quiz.date ||
                              quiz.createdAt
                          )}
                        </span>
                      </div>
                    </div>
                  </article>
                );
              })}
            </div>
          )}
        </>
      )}
    </div>
  );
}
