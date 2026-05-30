import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, ChevronLeft, ChevronRight } from "lucide-react";

import studentQuizApi from "../../api/studentQuizApi";
import styles from "./TakeQuiz.module.css";

/* ── Helpers ────────────────────────────────────────────── */
const getOptions = (question) => {
  if (!question) return [];
  if (question.type === "TRUE_FALSE") return ["Vrai", "Faux"];
  return question.options || question.choix || question.choices || [];
};

const formatTime = (seconds) => {
  if (seconds === null || seconds === undefined) return "--:--";
  const m = Math.floor(seconds / 60).toString().padStart(2, "0");
  const s = (seconds % 60).toString().padStart(2, "0");
  return `${m}:${s}`;
};

/* ── Composant Timer inline ─────────────────────────────── */
function TimerBadge({ seconds }) {
  const isUrgent = seconds !== null && seconds <= 60;
  const isWarning = seconds !== null && seconds <= 180 && seconds > 60;
  return (
    <div
      className={`${styles.timerBadge} ${
        isUrgent ? styles.timerUrgent : isWarning ? styles.timerWarning : ""
      }`}
    >
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className={styles.timerIcon}>
        <circle cx="12" cy="12" r="10" />
        <polyline points="12 6 12 12 16 14" />
      </svg>
      <span>{formatTime(seconds)}</span>
    </div>
  );
}

/* ── Page principale ────────────────────────────────────── */
export default function TakeQuiz() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [quiz,             setQuiz]             = useState(null);
  const [questions,        setQuestions]        = useState([]);
  const [currentIndex,     setCurrentIndex]     = useState(0);
  const [answers,          setAnswers]          = useState({});
  const [remainingSeconds, setRemainingSeconds] = useState(null);
  const [started,          setStarted]          = useState(false);
  const [loading,          setLoading]          = useState(true);
  const [starting,         setStarting]         = useState(false);
  const [submitting,       setSubmitting]       = useState(false);
  const [error,            setError]            = useState("");
  const [timeOver,         setTimeOver]         = useState(false);
  const [submitMessage,    setSubmitMessage]    = useState("");

  const intervalRef = useRef();
  const pollRef     = useRef();

  /* Charger le quiz */
  useEffect(() => {
    const loadQuiz = async () => {
      try {
        setLoading(true);
        const quizData = await studentQuizApi.getQuizDetails(id);
        setQuiz(quizData);
      } catch (err) {
        setError(err?.response?.data?.message || err?.message || "Impossible de charger le quiz.");
      } finally {
        setLoading(false);
      }
    };
    loadQuiz();
    return () => { clearInterval(intervalRef.current); clearInterval(pollRef.current); };
  }, [id]);

  /* Countdown */
  useEffect(() => {
    if (!started || remainingSeconds === null) return;
    if (remainingSeconds <= 0) { setTimeOver(true); return; }
    intervalRef.current = window.setInterval(() => {
      setRemainingSeconds((cur) => {
        if (cur <= 1) { clearInterval(intervalRef.current); clearInterval(pollRef.current); setTimeOver(true); return 0; }
        return cur - 1;
      });
    }, 1000);
    return () => clearInterval(intervalRef.current);
  }, [started, remainingSeconds]);

  /* Poll serveur */
  useEffect(() => {
    if (!started) return;
    pollRef.current = window.setInterval(async () => {
      try { const s = await studentQuizApi.getRemainingSeconds(id); setRemainingSeconds(s); } catch { /* ignore */ }
    }, 10000);
    return () => clearInterval(pollRef.current);
  }, [started, id]);

  /* Démarrer */
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
      setError(err?.response?.data?.message || err?.message || "Impossible de démarrer le quiz.");
    } finally {
      setStarting(false);
    }
  };

  /* Soumettre */
  const handleSubmit = async () => {
    try {
      setSubmitting(true);
      await studentQuizApi.submitQuiz(id, answers);
      setSubmitMessage("Quiz envoyé avec succès !");
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Erreur lors de l'envoi.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleAnswer = (questionId, value) =>
    setAnswers((prev) => ({ ...prev, [questionId]: value }));

  /* Dérivés */
  const currentQuestion  = questions[currentIndex];
  const answeredCount    = Object.keys(answers).length;
  const progress         = questions.length > 0 ? (answeredCount / questions.length) * 100 : 0;
  const isFirst          = currentIndex === 0;
  const isLast           = currentIndex === questions.length - 1;

  /* ── États de chargement / erreur ── */
  if (loading) return <div className={styles.stateBox}><div className={styles.spinner} /><p>Chargement du quiz...</p></div>;
  if (error)   return <div className={`${styles.stateBox} ${styles.stateError}`}><p>{error}</p><button className={styles.btnBack} onClick={() => navigate(-1)}>Retour</button></div>;

  /* ── Succès soumission ── */
  if (submitMessage) {
    return (
      <div className={styles.page}>
        <div className={styles.successScreen}>
          <div className={styles.successIcon}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
              <circle cx="12" cy="12" r="10" /><polyline points="9 12 11 14 15 10" />
            </svg>
          </div>
          <h2>Quiz soumis avec succès !</h2>
          <p>Vos réponses ont bien été enregistrées.</p>
          <button className={styles.btnPrimary} onClick={() => navigate(-1)}>Retour aux quiz</button>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.page}>

      {/* ══ BARRE D'AVANCEMENT GLOBALE (tout en haut) ══ */}
      <div className={styles.globalProgressTrack}>
        <div className={styles.globalProgressFill} style={{ width: `${progress}%` }} />
      </div>

      {/* ══ HEADER ══ */}
      <div className={styles.header}>
        <button className={styles.backBtn} onClick={() => navigate(-1)}>
          <ArrowLeft size={18} />
        </button>

        <div className={styles.headerCenter}>
          <h1 className={styles.quizTitle}>{quiz?.titre || "Quiz"}</h1>
          {started && (
            <div className={styles.quizMeta}>
              <span className={styles.metaPill}>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" width="14" height="14">
                  <path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/>
                  <rect x="9" y="3" width="6" height="4" rx="1"/>
                </svg>
                Question {currentIndex + 1} / {questions.length}
              </span>
              <span className={styles.metaPill}>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" width="14" height="14">
                  <polyline points="20 6 9 17 4 12"/>
                </svg>
                {answeredCount} répondu{answeredCount > 1 ? "s" : ""}
              </span>
              <span className={`${styles.metaPill} ${styles.metaType}`}>QCM</span>
            </div>
          )}
        </div>

        <TimerBadge seconds={remainingSeconds} />
      </div>

      {/* ══ ÉCRAN DE DÉPART ══ */}
      {!started ? (
        <div className={styles.readyCard}>
          <div className={styles.readyIcon}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
              <circle cx="12" cy="12" r="10"/>
              <path d="M10 8l6 4-6 4V8z" fill="currentColor" stroke="none"/>
            </svg>
          </div>
          <h2>{quiz?.titre || "Quiz"}</h2>
          {quiz?.description && <p className={styles.readyDesc}>{quiz.description}</p>}
          <div className={styles.readyInfoRow}>
            {quiz?.dureeMinutes && (
              <div className={styles.readyInfoPill}>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" width="16" height="16">
                  <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
                </svg>
                {quiz.dureeMinutes} min
              </div>
            )}
            {quiz?.nombreQuestions && (
              <div className={styles.readyInfoPill}>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" width="16" height="16">
                  <path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/><rect x="9" y="3" width="6" height="4" rx="1"/>
                </svg>
                {quiz.nombreQuestions} questions
              </div>
            )}
          </div>
          <button className={styles.btnStart} onClick={startQuiz} disabled={starting}>
            {starting ? (
              <><div className={styles.spinnerSmall} /> Démarrage...</>
            ) : (
              <>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="18" height="18">
                  <path d="M10 8l6 4-6 4V8z" fill="currentColor" stroke="none"/>
                </svg>
                Passer le quiz
              </>
            )}
          </button>
        </div>
      ) : (
        /* ══ CARTE QUESTION ══ */
        <div className={styles.quizCard}>

          {/* Barre de progression interne */}
          <div className={styles.progressRow}>
            <div className={styles.progressTrack}>
              <div className={styles.progressFill} style={{ width: `${progress}%` }} />
            </div>
            <span className={styles.progressLabel}>{Math.round(progress)}%</span>
          </div>

          {/* Numéro + question */}
          <div className={styles.questionNum}>Question {currentIndex + 1}</div>
          <h2 className={styles.questionText}>
            {currentQuestion?.enonce || "Question indisponible"}
          </h2>

          {/* Options ou texte libre */}
          {currentQuestion?.type === "TEXT" ? (
            <textarea
              className={styles.textAnswer}
              value={answers[currentQuestion.id] || ""}
              onChange={(e) => handleAnswer(currentQuestion.id, e.target.value)}
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
                    className={`${styles.optionBtn} ${isSelected ? styles.optionSelected : ""}`}
                    onClick={() => handleAnswer(currentQuestion.id, option)}
                    disabled={timeOver}
                  >
                    <span className={`${styles.optionLetter} ${isSelected ? styles.optionLetterSelected : ""}`}>
                      {String.fromCharCode(65 + index)}
                    </span>
                    <span className={styles.optionText}>{option}</span>
                    {isSelected && (
                      <svg className={styles.optionCheck} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                        <polyline points="20 6 9 17 4 12"/>
                      </svg>
                    )}
                  </button>
                );
              })}
            </div>
          )}

          {/* Temps écoulé */}
          {timeOver && (
            <div className={styles.timeExpiredBanner}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="16" height="16">
                <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
              Temps écoulé — vous ne pouvez plus modifier vos réponses.
            </div>
          )}

          {/* Navigation Précédent / Suivant */}
          <div className={styles.navigation}>
            <button
              className={styles.btnPrev}
              onClick={() => setCurrentIndex((p) => Math.max(p - 1, 0))}
              disabled={isFirst}
            >
              <ChevronLeft size={18} />
              Précédent
            </button>

            {!isLast ? (
              <button
                className={styles.btnNext}
                onClick={() => setCurrentIndex((p) => Math.min(p + 1, questions.length - 1))}
              >
                Suivant
                <ChevronRight size={18} />
              </button>
            ) : (
              <button
                className={styles.btnSubmit}
                onClick={handleSubmit}
                disabled={timeOver || submitting}
              >
                {submitting ? (
                  <><div className={styles.spinnerSmall} /> Envoi...</>
                ) : (
                  <>
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" width="17" height="17">
                      <polyline points="20 6 9 17 4 12"/>
                    </svg>
                    Valider le quiz
                  </>
                )}
              </button>
            )}
          </div>

          {/* Minimap questions */}
          <div className={styles.questionMap}>
            {questions.map((q, i) => (
              <button
                key={i}
                className={`${styles.mapDot} ${i === currentIndex ? styles.mapDotActive : ""} ${answers[q.id] ? styles.mapDotAnswered : ""}`}
                onClick={() => setCurrentIndex(i)}
                title={`Question ${i + 1}`}
              />
            ))}
          </div>

        </div>
      )}
    </div>
  );
}