import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  CheckCircle2,
  XCircle,
  HelpCircle,
  ArrowLeft,
} from "lucide-react";
import studentQuizApi from "../../api/studentQuizApi";
import styles from "./QuizResult.module.css";

const hasText = (value) => typeof value === "string" && value.trim().length > 0;

const normalizeResult = (result) => {
  if (!result) return null;

  return {
    ...result,
    feedback:
      result.feedback ||
      result.feedbackIa ||
      result.feedbackIA ||
      result.feedback_ia ||
      "",
    recommendations: result.recommendations || result.recommandations || "",
    strengths: result.strengths || result.pointsForts || "",
    weaknesses: result.weaknesses || result.pointsFaibles || "",
  };
};

const readStoredResult = (quizId) => {
  try {
    const stored = sessionStorage.getItem(`quiz_result_${quizId}`);
    return stored ? JSON.parse(stored) : null;
  } catch {
    return null;
  }
};

const mergeResultData = (backendResult, storedResult) => {
  const backend = normalizeResult(backendResult);
  const stored = normalizeResult(storedResult);

  if (!backend) return stored;
  if (!stored) return backend;

  return {
    ...stored,
    ...backend,
    feedback: backend.feedback || stored.feedback,
    recommendations: backend.recommendations || stored.recommendations,
    strengths: backend.strengths || stored.strengths,
    weaknesses: backend.weaknesses || stored.weaknesses,
  };
};

const getScorePercentage = (result) => {
  const raw =
    result?.percentage ??
    result?.scorePercentage ??
    result?.score ??
    (result?.noteSur20 != null ? Number(result.noteSur20) * 5 : 0);

  const value = Number(raw);
  return Number.isFinite(value) ? value : 0;
};

const getScoreLabel = (result) => {
  if (result?.noteSur20 != null) {
    return `${Number(result.noteSur20).toFixed(2)} / 20`;
  }

  if (result?.earnedPoints != null && result?.totalPoints != null) {
    return `${result.earnedPoints} / ${result.totalPoints}`;
  }

  return `${Math.round(getScorePercentage(result))}%`;
};

const getMention = (score) => {
  if (score >= 80) return "Excellent !";
  if (score >= 60) return "Bien joué !";
  if (score >= 40) return "Peut mieux faire";
  return "À améliorer";
};

const getAnswerDisplay = (value) => {
  if (value === null || value === undefined || value === "") return "Non répondue";
  return value;
};

const getQuestionText = (item) =>
  item.questionText || item.question || item.enonce || item.texte || "Question indisponible";

const getStudentAnswer = (item) =>
  item.studentAnswer ||
  item.reponseEtudiant ||
  item.userAnswer ||
  item.answer ||
  item.selectedAnswer ||
  "";

const getCorrectAnswer = (item) =>
  item.correctAnswer ||
  item.reponseCorrecte ||
  item.correctResponse ||
  item.correctOption ||
  "";

const getQuestionPoints = (item) => {
  const isCorrect =
    item?.isCorrect === true ||
    item?.correct === true ||
    item?.correcte === true;

  const total =
    item?.pointsMax ??
    item?.points ??
    item?.totalPoints ??
    item?.noteMax ??
    item?.questionPoints ??
    1;

  const earned =
    item?.pointsEarned ??
    item?.earnedPoints ??
    item?.pointsObtenus ??
    item?.noteObtenue ??
    item?.scoreQuestion ??
    item?.pointsGagnes ??
    (isCorrect ? total : 0);

  return `${earned} / ${total}`;
};

const getOptions = (item) => {
  const options =
    item.options ||
    item.choix ||
    item.choices ||
    item.propositions ||
    [];

  if (Array.isArray(options) && options.length > 0) return options;

  return [
    item.choixa || item.choixA,
    item.choixb || item.choixB,
    item.choixc || item.choixC,
    item.choixd || item.choixD,
  ].filter(Boolean);
};

export default function QuizResult() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [submitResult, setSubmitResult] = useState(null);
  const [corrections, setCorrections] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadResult = async () => {
      try {
        setLoading(true);
        setError("");

        const storedResult = readStoredResult(id);

        const [resultData, correctionData] = await Promise.all([
          studentQuizApi.getResult(id).catch((err) => {
            if (storedResult) return null;
            throw err;
          }),
          studentQuizApi.getCorrections(id).catch(() => []),
        ]);

        const mergedResult = mergeResultData(resultData, storedResult);

        if (!mergedResult) {
          throw new Error("Aucun résultat disponible pour ce quiz.");
        }

        setSubmitResult(mergedResult);
        setCorrections(Array.isArray(correctionData) ? correctionData : []);
      } catch (err) {
        console.error("Erreur résultat:", err);
        setError(
          err?.response?.data?.message ||
            err?.response?.data ||
            err?.message ||
            "Impossible de charger les résultats."
        );
      } finally {
        setLoading(false);
      }
    };

    if (id) loadResult();
  }, [id]);

  const scorePercentage = useMemo(
    () => getScorePercentage(submitResult),
    [submitResult]
  );

  const correctCount = corrections.filter(
    (item) => item.isCorrect === true || item.correct === true || item.correcte === true
  ).length;

  const wrongCount = corrections.length - correctCount;

  const feedbackSections = [
    { title: "Feedback", value: submitResult?.feedback },
    { title: "Points forts", value: submitResult?.strengths },
    { title: "Points à améliorer", value: submitResult?.weaknesses },
    { title: "Recommandations", value: submitResult?.recommendations },
  ].filter((item) => hasText(item.value));

  if (loading) {
    return (
      <div className={styles.resultPage}>
        <div className={styles.loadingBox}>
          <div className={styles.spinner} />
          <p>Chargement des résultats...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className={styles.resultPage}>
        <div className={styles.errorBox}>
          <h2>Résultat indisponible</h2>
          <p>{error}</p>
          <button
            className={styles.primaryBtn}
            onClick={() => navigate("/student/quizzes", { replace: true })}
          >
            Retour aux quiz
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.resultPage}>
      <div className={styles.resultWrapper}>
        <button className={styles.backBtn} onClick={() => navigate("/student/quizzes")}>
          <ArrowLeft size={18} />
          Retour aux quiz
        </button>

        <section className={styles.heroCard}>
          <div>
            <span className={styles.badge}>Quiz terminé</span>
            <h1>Résultats du quiz</h1>
            <p>Voici votre score, votre feedback et la correction détaillée.</p>
          </div>

          <div className={styles.bigScore}>
            <span>{Math.round(scorePercentage)}%</span>
            <small>{submitResult?.mention || submitResult?.grade || getMention(scorePercentage)}</small>
          </div>
        </section>

        <section className={styles.statsGrid}>
          <div className={styles.statCard}>
            <span className={styles.statLabel}>Score</span>
            <strong>{getScoreLabel(submitResult)}</strong>
          </div>

          <div className={styles.statCard}>
            <span className={styles.statLabel}>Questions</span>
            <strong>{corrections.length || submitResult?.totalQuestions || 0}</strong>
          </div>

          <div className={styles.statCard}>
            <span className={styles.statLabel}>Bonnes réponses</span>
            <strong className={styles.successText}>✓ {correctCount}</strong>
          </div>

          <div className={styles.statCard}>
            <span className={styles.statLabel}>Mauvaises réponses</span>
            <strong className={styles.dangerText}>✕ {wrongCount}</strong>
          </div>
        </section>

        {feedbackSections.length > 0 && (
          <section className={styles.feedbackCard}>
            <div className={styles.feedbackHeader}>
              <span>Feedback personnalisé</span>
              <h2>Conseils après le quiz</h2>
            </div>

            <div className={styles.feedbackGrid}>
              {feedbackSections.map((item) => (
                <article key={item.title} className={styles.feedbackBlock}>
                  <strong>{item.title}</strong>
                  <p>{item.value}</p>
                </article>
              ))}
            </div>
          </section>
        )}

        <section className={styles.answersCard}>
          <div className={styles.sectionHeader}>
            <h2>Détail des réponses</h2>
            <span>{corrections.length} question(s)</span>
          </div>

          {corrections.length === 0 ? (
            <div className={styles.emptyBox}>
              Aucune correction détaillée disponible pour ce quiz.
            </div>
          ) : (
            <div className={styles.answersList}>
              {corrections.map((item, index) => {
                const isCorrect =
                  item.isCorrect === true ||
                  item.correct === true ||
                  item.correcte === true;

                const options = getOptions(item);
                const studentAnswer = getStudentAnswer(item);
                const correctAnswer = getCorrectAnswer(item);

                return (
                  <article
                    key={item.questionId || item.id || index}
                    className={`${styles.answerItem} ${
                      isCorrect ? styles.correctItem : styles.wrongItem
                    }`}
                  >
                    <div className={styles.answerTop}>
                      <div className={styles.questionNumber}>
                        {isCorrect ? (
                          <CheckCircle2 size={20} />
                        ) : (
                          <XCircle size={20} />
                        )}
                      </div>

                      <div>
                        <span className={styles.questionBadge}>
                          Question {index + 1}
                        </span>
                        <h3>{getQuestionText(item)}</h3>
                      </div>

                      <div
                        className={`${styles.statusBadge} ${
                          isCorrect ? styles.statusCorrect : styles.statusWrong
                        }`}
                      >
                        {isCorrect ? "Correcte" : "Incorrecte"}
                      </div>
                    </div>

                    {options.length > 0 && (
                      <div className={styles.optionsBox}>
                        {options.map((option, optIndex) => (
                          <div key={optIndex} className={styles.optionLine}>
                            <span>{String.fromCharCode(65 + optIndex)}</span>
                            <p>{option}</p>
                          </div>
                        ))}
                      </div>
                    )}

                    <div className={styles.answerDetails}>
                      <div>
                        <span>Votre réponse</span>
                        <strong>{getAnswerDisplay(studentAnswer)}</strong>
                      </div>

                      <div>
                        <span>Réponse correcte</span>
                        <strong>{getAnswerDisplay(correctAnswer)}</strong>
                      </div>

                      <div>
                        <span>Note de la question</span>
                        <strong>{getQuestionPoints(item)}</strong>
                      </div>
                    </div>

                    {(item.explanation || item.explication) && (
                      <div className={styles.explanation}>
                        <HelpCircle size={17} />
                        <p>{item.explanation || item.explication}</p>
                      </div>
                    )}
                  </article>
                );
              })}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
