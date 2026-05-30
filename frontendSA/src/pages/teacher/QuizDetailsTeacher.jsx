import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  ArrowLeft,
  BookOpen,
  Clock,
  Eye,
  FileQuestion,
  Users,
} from "lucide-react";
import teacherQuizApi from "../../api/teacherQuizApi";
import styles from "./QuizDetailsTeacher.module.css";

const QuizDetailsTeacher = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [quiz, setQuiz] = useState(null);
  const [questions, setQuestions] = useState([]);
  const [current, setCurrent] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const currentQuestion = questions[current];

  useEffect(() => {
    const loadData = async () => {
      try {
        setLoading(true);

        const [quizData, questionData] = await Promise.all([
          teacherQuizApi.getQuizById(id),
          teacherQuizApi.getQuestions(id),
        ]);

        setQuiz(quizData);
        setQuestions(Array.isArray(questionData) ? questionData : []);
      } catch (err) {
        setError(
          err?.response?.data?.message ||
            err?.message ||
            "Erreur lors du chargement du quiz."
        );
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, [id]);

  const getOptions = (question) => {
    if (!question) return [];
    return question.options || [];
  };

  const isTextQuestion = (question) => question?.type === "TEXT";
  const isTrueFalse = (question) => question?.type === "TRUE_FALSE";

  if (loading) {
    return <div className={styles.loading}>Chargement du quiz...</div>;
  }

  if (error) {
    return <div className={styles.errorBox}>{error}</div>;
  }

  return (
    <div className={styles.page}>
      <div className={styles.hero}>
        <button className={styles.backBtn} onClick={() => navigate("/teacher/quizzes")} aria-label="Retour">
          <ArrowLeft size={18} />
        </button>

        <div className={styles.heroContent}>
          <span className={styles.badge}>
            <Eye size={16} />
            Aperçu final
          </span>

          <h1>{quiz?.titre || "Quiz sans titre"}</h1>
          <p>
            Prévisualisation de la version finale visible par l’étudiant. Ce mode
            permet uniquement de consulter le contenu.
          </p>

          <div className={styles.metaGrid}>
            <div>
              <BookOpen size={18} />
              <span>{quiz?.theme || "Sans thème"}</span>
            </div>

            <div>
              <FileQuestion size={18} />
              <span>{quiz?.questionCount || questions.length} questions</span>
            </div>

            <div>
              <Clock size={18} />
              <span>{quiz?.timeLimit || 0} minutes</span>
            </div>

            <div>
              <Users size={18} />
              <span>{quiz?.totalStudentsAllowed || 0} étudiants</span>
            </div>
          </div>
        </div>
      </div>

      {questions.length === 0 ? (
        <div className={styles.emptyBox}>Aucune question dans ce quiz.</div>
      ) : (
        <section className={styles.quizPreview}>
          <div className={styles.progressHeader}>
            <div>
              <span>Question {current + 1} / {questions.length}</span>
              <h2>{currentQuestion?.enonce}</h2>
            </div>

            <strong>{currentQuestion?.points || 1} pt</strong>
          </div>

          {isTextQuestion(currentQuestion) ? (
            <textarea
              className={styles.textAnswer}
              placeholder="Réponse écrite de l’étudiant..."
              disabled
            />
          ) : (
            <div className={styles.optionsList}>
              {getOptions(currentQuestion).map((option, index) => (
                <div className={styles.optionCard} key={index}>
                  <span>{String.fromCharCode(65 + index)}</span>
                  <p>{option}</p>
                </div>
              ))}

              {isTrueFalse(currentQuestion) && getOptions(currentQuestion).length === 0 && (
                <>
                  <div className={styles.optionCard}>
                    <span>A</span>
                    <p>Vrai</p>
                  </div>

                  <div className={styles.optionCard}>
                    <span>B</span>
                    <p>Faux</p>
                  </div>
                </>
              )}
            </div>
          )}

          <div className={styles.navigation}>
            <button
              disabled={current === 0}
              onClick={() => setCurrent((prev) => prev - 1)}
            >
              Précédent
            </button>

            <button
              disabled={current === questions.length - 1}
              onClick={() => setCurrent((prev) => prev + 1)}
            >
              Suivant
            </button>
          </div>
        </section>
      )}
    </div>
  );
};

export default QuizDetailsTeacher;
