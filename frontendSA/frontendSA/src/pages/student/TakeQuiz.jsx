import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, ChevronLeft, ChevronRight, Clock } from "lucide-react";
import studentQuizApi from "../../api/studentQuizApi";
import Timer from "../../components/quiz/Timer";
import styles from "./TakeQuiz.module.css";

const getOptions = (question) => {
  if (!question) return [];
  if (question.type === "TRUE_FALSE") return ["Vrai", "Faux"];
  return question.options || question.choix || question.choices || [];
};

export default function TakeQuiz() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [quiz, setQuiz] = useState(null);
  const [questions, setQuestions] = useState([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [answers, setAnswers] = useState({});
  const [remainingSeconds, setRemainingSeconds] = useState(null);
  const [started, setStarted] = useState(false);
  const [loading, setLoading] = useState(true);
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState("");
  const [timeOver, setTimeOver] = useState(false);
  const [submitMessage, setSubmitMessage] = useState("");
  const intervalRef = useRef();
  const pollRef = useRef();

  useEffect(() => {
    const loadQuiz = async () => {
      try {
        setLoading(true);
        const quizData = await studentQuizApi.getQuizDetails(id);
        setQuiz(quizData);
      } catch (err) {
        setError(
          err?.response?.data?.message || err?.message ||
            "Impossible de charger les informations du quiz."
        );
      } finally {
        setLoading(false);
      }
    };

    loadQuiz();

    return () => {
      clearInterval(intervalRef.current);
      clearInterval(pollRef.current);
    };
  }, [id]);

  useEffect(() => {
    if (!started) return;
    if (remainingSeconds === null) return;
    if (remainingSeconds <= 0) {
      setTimeOver(true);
      clearInterval(intervalRef.current);
      return;
    }

    intervalRef.current = window.setInterval(() => {
      setRemainingSeconds((current) => {
        if (current <= 1) {
          clearInterval(intervalRef.current);
          clearInterval(pollRef.current);
          setTimeOver(true);
          return 0;
        }
        return current - 1;
      });
    }, 1000);

    return () => clearInterval(intervalRef.current);
  }, [started, remainingSeconds]);

  useEffect(() => {
    if (!started) return;

    pollRef.current = window.setInterval(async () => {
      try {
        const latest = await studentQuizApi.getRemainingSeconds(id);
        setRemainingSeconds(latest);
      } catch {
        // ignore polling failures silently
      }
    }, 10000);

    return () => clearInterval(pollRef.current);
  }, [started, id]);

  const currentQuestion = questions[currentIndex];

  const handleAnswer = (questionId, value) => {
    setAnswers((prev) => ({ ...prev, [questionId]: value }));
  };

  const startQuiz = async () => {
    try {
      setStarting(true);
      await studentQuizApi.startQuiz(id);
      const [remaining, questionData] = await Promise.all([
        studentQuizApi.getRemainingSeconds(id),
        studentQuizApi.getQuestions(id),
      ]);
      setRemainingSeconds(remaining);
      setQuestions(Array.isArray(questionData) ? questionData : []);
      setStarted(true);
      setTimeOver(false);
    } catch (err) {
      setError(
        err?.response?.data?.message || err?.message ||
          "Impossible de démarrer le quiz."
      );
    } finally {
      setStarting(false);
    }
  };

  const handleSubmit = async () => {
    try {
      setLoading(true);
      await studentQuizApi.submitQuiz(id, answers);
      setSubmitMessage("Quiz envoyé. Vos réponses ont été soumises.");
    } catch (err) {
      setError(
        err?.response?.data?.message || err?.message ||
          "Une erreur est survenue lors de l'envoi du quiz."
      );
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className={styles.loading}>Chargement du quiz...</div>;
  }

  if (error) {
    return <div className={styles.errorBox}>{error}</div>;
  }

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <div className={styles.titleBlock}>
          <button className={styles.backBtn} onClick={() => navigate(-1)} aria-label="Retour">
            <ArrowLeft size={18} />
          </button>

          <div className={styles.quizHeader}>
            <h1>{quiz?.titre || "Quiz"}</h1>
            <p>{quiz?.description || "Répondez aux questions dans le temps imparti."}</p>
          </div>

          <div className={styles.metaGrid}>
            <div>
              <Clock size={18} />
              <span>{quiz?.timeLimit || 0} minutes</span>
            </div>
            <div>
              <span>{questions.length || quiz?.questionCount || 0} questions</span>
            </div>
          </div>
        </div>

        <Timer seconds={remainingSeconds} />
      </div>

      {!started ? (
        <div className={styles.readyBox}>
          <p>
            Cliquez sur "Commencer le quiz" pour lancer le chrono et afficher les
            questions. Le temps restera fixe en haut à droite pendant toute la
            session.
          </p>
          <button
            className={styles.startButton}
            onClick={startQuiz}
            disabled={starting}
          >
            {starting ? "Démarrage..." : "Commencer le quiz"}
          </button>
        </div>
      ) : (
        <div className={styles.quizCard}>
          <div className={styles.questionHeader}>
            <div>
              <span>Question {currentIndex + 1} / {questions.length}</span>
              <h2>{currentQuestion?.enonce || "Question indisponible"}</h2>
            </div>
            <strong>{currentQuestion?.points || 1} pt</strong>
          </div>

          {currentQuestion?.type === "TEXT" ? (
            <textarea
              className={styles.textAnswer}
              value={answers[currentQuestion.id] || ""}
              onChange={(event) =>
                handleAnswer(currentQuestion.id, event.target.value)
              }
              placeholder="Tapez votre réponse ici..."
              disabled={timeOver}
            />
          ) : (
            <div className={styles.optionsList}>
              {getOptions(currentQuestion).map((option, index) => (
                <button
                  key={index}
                  type="button"
                  className={`${styles.optionButton} ${
                    answers[currentQuestion.id] === option
                      ? styles.selectedOption
                      : ""
                  }`}
                  onClick={() => handleAnswer(currentQuestion.id, option)}
                  disabled={timeOver}
                >
                  <span>{String.fromCharCode(65 + index)}</span>
                  <p>{option}</p>
                </button>
              ))}
            </div>
          )}

          <div className={styles.navigation}>
            <button
              className={styles.navButton}
              onClick={() => setCurrentIndex((prev) => Math.max(prev - 1, 0))}
              disabled={currentIndex === 0}
            >
              <ChevronLeft size={16} /> Précédent
            </button>

            <button
              className={styles.navButton}
              onClick={() =>
                setCurrentIndex((prev) => Math.min(prev + 1, questions.length - 1))
              }
              disabled={currentIndex === questions.length - 1}
            >
              Suivant <ChevronRight size={16} />
            </button>
          </div>

          <div className={styles.submitSection}>
            <button
              className={styles.submitButton}
              onClick={handleSubmit}
              disabled={timeOver || loading}
            >
              {loading ? "Envoi..." : "Valider le quiz"}
            </button>
            {timeOver && (
              <div className={styles.timeExpired}>
                Le temps est écoulé. Vos réponses sont conservées.
              </div>
            )}
            {submitMessage && (
              <div className={styles.successMessage}>{submitMessage}</div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
