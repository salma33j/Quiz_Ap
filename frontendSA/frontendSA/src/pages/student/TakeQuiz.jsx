import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, ChevronLeft, ChevronRight } from "lucide-react";

import studentQuizApi from "../../api/studentQuizApi";
import styles from "./TakeQuiz.module.css";

const getOptions = (question) => {
  if (!question) return [];

  if (question.type === "TRUE_FALSE" || question.type === "VRAI_FAUX") {
    return ["Vrai", "Faux"];
  }

  return question.options || question.choix || question.choices || [];
};

const getQuestionText = (question) => {
  return (
    question?.enonce ||
    question?.questionText ||
    question?.texte ||
    question?.text ||
    "Question indisponible"
  );
};

const isTextQuestion = (question) => {
  return (
    question?.type === "TEXT" ||
    question?.type === "REPONSE_LIBRE" ||
    question?.type === "FREE_TEXT"
  );
};

const formatTime = (seconds) => {
  if (seconds === null || seconds === undefined || Number.isNaN(seconds)) {
    return "00:00";
  }

  const m = Math.floor(seconds / 60)
    .toString()
    .padStart(2, "0");

  const s = Math.floor(seconds % 60)
    .toString()
    .padStart(2, "0");

  return `${m}:${s}`;
};

const getQuizDuration = (quiz) =>
  quiz?.timeLimit ||
  quiz?.dureeMinutes ||
  quiz?.durationMinutes ||
  quiz?.duration ||
  30;

function TimerBadge({ seconds }) {
  const isWarning = seconds !== null && seconds <= 120;

  return (
    <div
      className={`${styles.timerBadge} ${
        isWarning ? styles.timerWarning : ""
      }`}
    >
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        className={styles.timerIcon}
      >
        <circle cx="12" cy="12" r="10" />
        <polyline points="12 6 12 12 16 14" />
      </svg>

      <span>{formatTime(seconds)}</span>
    </div>
  );
}

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
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [timeOver, setTimeOver] = useState(false);

  const intervalRef = useRef(null);
  const pollRef = useRef(null);
  const submittedRef = useRef(false);
  const answersRef = useRef({});

  useEffect(() => {
    answersRef.current = answers;
  }, [answers]);

  useEffect(() => {
    const loadQuiz = async () => {
      try {
        setLoading(true);
        setError("");
        setTimeOver(false);
        setStarted(false);
        setQuestions([]);
        setAnswers({});
        setCurrentIndex(0);
        submittedRef.current = false;

        const quizData = await studentQuizApi.getQuizDetails(id);
        setQuiz(quizData);

        await studentQuizApi.startQuiz(id);

        const questionData = await studentQuizApi.getQuestions(id);
        const safeQuestions = Array.isArray(questionData) ? questionData : [];

        let serverSeconds = await studentQuizApi.getRemainingSeconds(id);

        if (
          serverSeconds === null ||
          serverSeconds === undefined ||
          Number.isNaN(serverSeconds) ||
          serverSeconds < 0
        ) {
          serverSeconds = getQuizDuration(quizData) * 60;
        }

        setQuestions(safeQuestions);
        setRemainingSeconds(serverSeconds);
        setStarted(true);
      } catch (err) {
        console.error("Erreur chargement quiz:", err);
        setError(
          err?.response?.data?.message ||
            err?.response?.data ||
            err?.message ||
            "Impossible de charger le quiz."
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
    if (!started || remainingSeconds === null || submittedRef.current) return;

    if (remainingSeconds <= 0) {
      setTimeOver(true);
      handleSubmit({ automatic: true });
      return;
    }

    intervalRef.current = window.setInterval(() => {
      setRemainingSeconds((prev) => {
        if (prev <= 1) {
          clearInterval(intervalRef.current);
          clearInterval(pollRef.current);
          setTimeOver(true);
          window.setTimeout(() => {
            handleSubmit({ automatic: true });
          }, 0);
          return 0;
        }

        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(intervalRef.current);
  }, [started, remainingSeconds]);

  useEffect(() => {
    if (!started || submittedRef.current) return;

    pollRef.current = window.setInterval(async () => {
      try {
        const serverSeconds = await studentQuizApi.getRemainingSeconds(id);

        if (
          serverSeconds !== null &&
          serverSeconds !== undefined &&
          !Number.isNaN(serverSeconds) &&
          serverSeconds >= 0
        ) {
          setRemainingSeconds(serverSeconds);

          if (serverSeconds <= 0) {
            setTimeOver(true);
            clearInterval(intervalRef.current);
            clearInterval(pollRef.current);
            handleSubmit({ automatic: true });
          }
        }
      } catch (err) {
        console.warn("Timer serveur indisponible:", err);
      }
    }, 10000);

    return () => clearInterval(pollRef.current);
  }, [started, id]);

  const handleAnswer = (questionId, value) => {
    setAnswers((prev) => ({
      ...prev,
      [questionId]: value,
    }));
  };

  const buildAnswerPayload = () =>
    Object.entries(answersRef.current).map(([questionId, value]) => ({
      quizId: Number(id),
      questionId: Number(questionId),
      studentAnswer: value,
    }));

  const handleSubmit = async ({ automatic = false } = {}) => {
    if (submittedRef.current) return;

    try {
      setSubmitting(true);
      setError("");
      submittedRef.current = true;

      clearInterval(intervalRef.current);
      clearInterval(pollRef.current);

      if (automatic) {
        setTimeOver(true);
      }

      const payload = buildAnswerPayload();
      const result =
        payload.length > 0
          ? await studentQuizApi.submitQuizWithAnswers(id, payload)
          : await studentQuizApi.submitQuiz(id);

      try {
        sessionStorage.setItem(`quiz_result_${id}`, JSON.stringify(result));
      } catch {
        // Le résultat reste disponible depuis le backend si le stockage local échoue.
      }

      navigate(`/student/quizzes/${id}/result`, {
        replace: true,
        state: automatic ? { autoSubmitted: true } : undefined,
      });
    } catch (err) {
      console.error("Erreur soumission:", err);
      submittedRef.current = false;

      setError(
        err?.response?.data?.message ||
          err?.response?.data ||
          err?.message ||
          "Erreur lors de l'envoi du quiz."
      );
    } finally {
      setSubmitting(false);
    }
  };

  const currentQuestion = questions[currentIndex];
  const answeredCount = Object.keys(answers).length;
  const progress =
    questions.length > 0 ? (answeredCount / questions.length) * 100 : 0;

  const isFirst = currentIndex === 0;
  const isLast = currentIndex === questions.length - 1;

  if (loading) {
    return (
      <div className={styles.stateBox}>
        <div className={styles.spinner} />
        <p>Chargement du quiz...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className={`${styles.stateBox} ${styles.stateError}`}>
        <p>{error}</p>
        <button className={styles.btnBack} onClick={() => navigate(-1)}>
          Retour
        </button>
      </div>
    );
  }

  if (!started) {
    return (
      <div className={styles.stateBox}>
        <div className={styles.spinner} />
        <p>Preparation des questions...</p>
      </div>
    );
  }

  return (
    <div className={styles.page}>
      <div className={styles.globalProgressTrack}>
        <div
          className={styles.globalProgressFill}
          style={{ width: `${progress}%` }}
        />
      </div>

      <div className={styles.header}>
        <button className={styles.backBtn} onClick={() => navigate(-1)}>
          <ArrowLeft size={18} />
        </button>

        <div className={styles.headerCenter}>
          <h1 className={styles.quizTitle}>
            {quiz?.titre || quiz?.title || "Quiz"}
          </h1>

          {started && (
            <div className={styles.quizMeta}>
              <span className={styles.metaPill}>
                Question {currentIndex + 1} / {questions.length}
              </span>

              <span className={styles.metaPill}>
                {answeredCount} répondu{answeredCount > 1 ? "s" : ""}
              </span>
            </div>
          )}
        </div>

        {started && <TimerBadge seconds={remainingSeconds} />}
      </div>

      <div className={styles.quizCard}>
          <div className={styles.progressRow}>
            <div className={styles.progressTrack}>
              <div
                className={styles.progressFill}
                style={{ width: `${progress}%` }}
              />
            </div>

            <span className={styles.progressLabel}>
              {Math.round(progress)}%
            </span>
          </div>

          {questions.length === 0 ? (
            <div className={styles.emptyBox}>
              Aucune question disponible pour ce quiz.
            </div>
          ) : (
            <>
              <div className={styles.questionNum}>
                Question {currentIndex + 1}
              </div>

              <h2 className={styles.questionText}>
                {getQuestionText(currentQuestion)}
              </h2>

              {isTextQuestion(currentQuestion) ? (
                <textarea
                  className={styles.textAnswer}
                  value={answers[currentQuestion.id] || ""}
                  onChange={(e) =>
                    handleAnswer(currentQuestion.id, e.target.value)
                  }
                  placeholder="Votre réponse..."
                  disabled={timeOver}
                />
              ) : (
                <div className={styles.optionsList}>
                  {getOptions(currentQuestion).map((option, index) => {
                    const isSelected = answers[currentQuestion?.id] === option;

                    return (
                      <button
                        key={index}
                        type="button"
                        className={`${styles.optionBtn} ${
                          isSelected ? styles.optionSelected : ""
                        }`}
                        onClick={() =>
                          handleAnswer(currentQuestion.id, option)
                        }
                        disabled={timeOver}
                      >
                        <span
                          className={`${styles.optionLetter} ${
                            isSelected ? styles.optionLetterSelected : ""
                          }`}
                        >
                          {String.fromCharCode(65 + index)}
                        </span>

                        <span className={styles.optionText}>{option}</span>

                        {isSelected && (
                          <span className={styles.optionCheck}>✓</span>
                        )}
                      </button>
                    );
                  })}
                </div>
              )}

              {timeOver && (
                <div className={styles.timeExpiredBanner}>
                  Temps ecoule. Envoi automatique du quiz en cours...
                </div>
              )}

              <div className={styles.navigation}>
                <button
                  className={styles.btnPrev}
                  onClick={() =>
                    setCurrentIndex((prev) => Math.max(prev - 1, 0))
                  }
                  disabled={isFirst}
                >
                  <ChevronLeft size={18} />
                  Précédent
                </button>

                {!isLast ? (
                  <button
                    className={styles.btnNext}
                    onClick={() =>
                      setCurrentIndex((prev) =>
                        Math.min(prev + 1, questions.length - 1)
                      )
                    }
                  >
                    Suivant
                    <ChevronRight size={18} />
                  </button>
                ) : (
                  <button
                    className={styles.btnSubmit}
                    onClick={handleSubmit}
                    disabled={submitting}
                  >
                    {submitting ? "Envoi..." : "Valider le quiz"}
                  </button>
                )}
              </div>

              <div className={styles.questionMap}>
                {questions.map((q, i) => (
                  <button
                    key={q.id || i}
                    className={`${styles.mapDot} ${
                      i === currentIndex ? styles.mapDotActive : ""
                    } ${answers[q.id] ? styles.mapDotAnswered : ""}`}
                    onClick={() => setCurrentIndex(i)}
                    type="button"
                  >
                    {i + 1}
                  </button>
                ))}
              </div>
            </>
          )}
      </div>
    </div>
  );
}

