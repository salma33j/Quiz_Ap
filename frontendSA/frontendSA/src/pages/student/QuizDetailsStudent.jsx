import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, BookOpen, CalendarClock, Clock, FileQuestion } from "lucide-react";
import studentQuizApi from "../../api/studentQuizApi";
import styles from "./QuizDetailsStudent.module.css";

const formatDateTime = (value) => {
  if (!value) return "Non definie";

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Non definie";

  return date.toLocaleString("fr-FR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};

const getThemeLabel = (quiz) =>
  quiz?.matiereName ||
  quiz?.matiereNom ||
  quiz?.subjectName ||
  quiz?.theme ||
  "Theme non defini";

export default function QuizDetailsStudent() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [quiz, setQuiz] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadQuiz = async () => {
      try {
        setLoading(true);
        const quizData = await studentQuizApi.getQuizDetails(id);
        setQuiz(quizData);
      } catch (err) {
        setError(
          err?.response?.data?.message ||
            err?.message ||
            "Impossible de charger les informations du quiz."
        );
      } finally {
        setLoading(false);
      }
    };

    loadQuiz();
  }, [id]);

  if (loading) {
    return <div className={styles.loading}>Chargement du quiz...</div>;
  }

  if (error) {
    return <div className={styles.errorBox}>{error}</div>;
  }

  return (
    <div className={styles.page}>
      <button className={styles.backBtn} onClick={() => navigate(-1)} aria-label="Retour">
        <ArrowLeft size={18} />
      </button>

      <div className={styles.card}>
        <div className={styles.topRow}>
          <div>
            <h1>{quiz?.titre || "Quiz sans titre"}</h1>
            <p>{quiz?.description || "Previsualisation du quiz avant de le commencer."}</p>
          </div>
          <button
            className={styles.startBtn}
            onClick={() => navigate(`/student/quizzes/${id}/take`)}
          >
            Commencer le quiz
          </button>
        </div>

        <div className={styles.statsGrid}>
          <div>
            <BookOpen size={18} />
            <span>{getThemeLabel(quiz)}</span>
          </div>
          <div>
            <FileQuestion size={18} />
            <span>{quiz?.questionCount || quiz?.nbQuestions || 0} questions</span>
          </div>
          <div>
            <Clock size={18} />
            <span>{quiz?.timeLimit || 0} minutes</span>
          </div>
          <div>
            <CalendarClock size={18} />
            <span className={styles.deadlineText}>
              <strong>Date limite :</strong>
              <small>{formatDateTime(quiz?.availableUntil || quiz?.dateFin || quiz?.deadline)}</small>
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}
