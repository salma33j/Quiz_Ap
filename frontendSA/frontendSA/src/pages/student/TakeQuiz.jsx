import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  ArrowLeft,
  ChevronLeft,
  ChevronRight,
  Clock,
} from "lucide-react";

import studentQuizApi from "../../api/studentQuizApi";
import Timer from "../../components/quiz/Timer";

import styles from "./TakeQuiz.module.css";

const getOptions = (question) => {
  if (!question) return [];

  if (question.type === "TRUE_FALSE") {
    return ["Vrai", "Faux"];
  }

  return (
    question.options ||
    question.choix ||
    question.choices ||
    []
  );
};

export default function TakeQuiz() {
  const { id } = useParams();

  const navigate = useNavigate();

  const [quiz, setQuiz] = useState(null);
  const [questions, setQuestions] = useState([]);
  const [currentIndex, setCurrentIndex] =
    useState(0);

  const [answers, setAnswers] = useState({});

  const [remainingSeconds, setRemainingSeconds] =
    useState(null);

  const [started, setStarted] = useState(false);

  const [loading, setLoading] = useState(true);

  const [starting, setStarting] = useState(false);

  const [error, setError] = useState("");

  const [timeOver, setTimeOver] = useState(false);

  const [submitMessage, setSubmitMessage] =
    useState("");

  const intervalRef = useRef();
  const pollRef = useRef();

  useEffect(() => {
    const loadQuiz = async () => {
      try {
        setLoading(true);

        const quizData =
          await studentQuizApi.getQuizDetails(id);

        setQuiz(quizData);
      } catch (err) {
        setError(
          err?.response?.data?.message ||
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

    return () =>
      clearInterval(intervalRef.current);
  }, [started, remainingSeconds]);

  useEffect(() => {
    if (!started) return;

    pollRef.current = window.setInterval(
      async () => {
        try {
          const latest =
            await studentQuizApi.getRemainingSeconds(
              id
            );

          setRemainingSeconds(latest);
        } catch {
          // ignore
        }
      },
      10000
    );

    return () =>
      clearInterval(pollRef.current);
  }, [started, id]);

  const currentQuestion =
    questions[currentIndex];

  const answeredQuestions = Object.keys(
    answers
  ).length;

  const progress =
    questions.length > 0
      ? (answeredQuestions / questions.length) *
        100
      : 0;

  const handleAnswer = (questionId, value) => {
    setAnswers((prev) => ({
      ...prev,
      [questionId]: value,
    }));
  };

  const startQuiz = async () => {
    try {
      setStarting(true);

      await studentQuizApi.startQuiz(id);

      const [remaining, questionData] =
        await Promise.all([
          studentQuizApi.getRemainingSeconds(id),

          studentQuizApi.getQuestions(id),
        ]);

      setRemainingSeconds(remaining);

      setQuestions(
        Array.isArray(questionData)
          ? questionData
          : []
      );

      setStarted(true);

      setTimeOver(false);
    } catch (err) {
      setError(
        err?.response?.data?.message ||
          err?.message ||
          "Impossible de démarrer le quiz."
      );
    } finally {
      setStarting(false);
    }
  };

  const handleSubmit = async () => {
    try {
      setLoading(true);

      await studentQuizApi.submitQuiz(
        id,
        answers
      );

      setSubmitMessage(
        "Quiz envoyé avec succès."
      );
    } catch (err) {
      setError(
        err?.response?.data?.message ||
          err?.message ||
          "Erreur lors de l'envoi du quiz."
      );
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className={styles.loading}>
        Chargement...
      </div>
    );
  }

  if (error) {
    return (
      <div className={styles.errorBox}>
        {error}
      </div>
    );
  }

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <button
          className={styles.backBtn}
          onClick={() => navigate(-1)}
        >
          <ArrowLeft size={18} />
        </button>

        <div className={styles.quizHeader}>
          <h1>
            {quiz?.titre || "Quiz"}
          </h1>

          <p>
            {quiz?.description ||
              "Répondez aux questions."}
          </p>
        </div>

        <Timer seconds={remainingSeconds} />
      </div>

      {!started ? (
        <div className={styles.readyBox}>
          <p>
            Cliquez sur le bouton pour commencer
            le quiz.
          </p>

          <button
            className={styles.startButton}
            onClick={startQuiz}
            disabled={starting}
          >
            {starting
              ? "Démarrage..."
              : "Commencer le quiz"}
          </button>
        </div>
      ) : (
        <div className={styles.quizCard}>
          <div className={styles.topInfo}>
            <span>
              Question {currentIndex + 1} /{" "}
              {questions.length}
            </span>

            <div className={styles.questionType}>
              QCM
            </div>
          </div>

          <div className={styles.progressBar}>
            <div
              className={styles.progressFill}
              style={{
                width: `${progress}%`,
              }}
            />
          </div>

          <div className={styles.questionHeader}>
            <h2>
              {currentQuestion?.enonce ||
                "Question indisponible"}
            </h2>
          </div>

          {currentQuestion?.type === "TEXT" ? (
            <textarea
              className={styles.textAnswer}
              value={
                answers[currentQuestion.id] || ""
              }
              onChange={(e) =>
                handleAnswer(
                  currentQuestion.id,
                  e.target.value
                )
              }
              placeholder="Votre réponse..."
              disabled={timeOver}
            />
          ) : (
            <div className={styles.optionsList}>
              {getOptions(currentQuestion).map(
                (option, index) => (
                  <button
                    key={index}
                    type="button"
                    className={`${
                      styles.optionButton
                    } ${
                      answers[
                        currentQuestion.id
                      ] === option
                        ? styles.selectedOption
                        : ""
                    }`}
                    onClick={() =>
                      handleAnswer(
                        currentQuestion.id,
                        option
                      )
                    }
                    disabled={timeOver}
                  >
                    <span
                      className={styles.optionLetter}
                    >
                      {String.fromCharCode(
                        65 + index
                      )}
                    </span>

                    <p>{option}</p>
                  </button>
                )
              )}
            </div>
          )}

          <div className={styles.navigation}>
            <button
              className={styles.prevButton}
              onClick={() =>
                setCurrentIndex((prev) =>
                  Math.max(prev - 1, 0)
                )
              }
              disabled={currentIndex === 0}
            >
              <ChevronLeft size={18} />
              Précédent
            </button>

            <button
              className={styles.nextButton}
              onClick={() =>
                setCurrentIndex((prev) =>
                  Math.min(
                    prev + 1,
                    questions.length - 1
                  )
                )
              }
              disabled={
                currentIndex ===
                questions.length - 1
              }
            >
              Suivant
              <ChevronRight size={18} />
            </button>
          </div>

          <div className={styles.submitSection}>
            <button
              className={styles.submitButton}
              onClick={handleSubmit}
              disabled={timeOver || loading}
            >
              {loading
                ? "Envoi..."
                : "Valider le quiz"}
            </button>

            {timeOver && (
              <div className={styles.timeExpired}>
                Temps écoulé.
              </div>
            )}

            {submitMessage && (
              <div
                className={styles.successMessage}
              >
                {submitMessage}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}