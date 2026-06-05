import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams, useSearchParams } from "react-router-dom";
import {
  ArrowLeft,
  BookOpen,
  CheckCircle2,
  Edit,
  Plus,
  Trash2,
} from "lucide-react";

import teacherQuizApi from "../../api/teacherQuizApi";
import styles from "./ManageQuestions.module.css";

const PUBLISH_INTENT_STORAGE_KEY = "teacher-quiz-publish-intents";

const getPublishIntents = () => {
  try {
    return JSON.parse(localStorage.getItem(PUBLISH_INTENT_STORAGE_KEY) || "[]").map(String);
  } catch {
    return [];
  }
};

const hasPublishIntent = (quizId) => getPublishIntents().includes(String(quizId));

const clearPublishIntent = (quizId) => {
  const ids = getPublishIntents().filter((item) => item !== String(quizId));
  localStorage.setItem(PUBLISH_INTENT_STORAGE_KEY, JSON.stringify(ids));
};

export default function ManageQuestions() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const isViewMode = searchParams.get("mode") === "view";

  const [quiz, setQuiz] = useState(null);
  const [questions, setQuestions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");
  const [publishNotice, setPublishNotice] = useState("");
  const autoPublishRef = useRef(false);

  useEffect(() => {
    load();
  }, [id]);

  async function load() {
    try {
      setLoading(true);
      setErr("");

      const [quizData, questionsData] = await Promise.all([
        teacherQuizApi.getQuizById(id),
        teacherQuizApi.getQuestions(id),
      ]);

      setQuiz(quizData);
      setQuestions(Array.isArray(questionsData) ? questionsData : []);
    } catch (e) {
      setErr(
        e?.response?.data?.message ||
          "Impossible de charger les questions."
      );
    } finally {
      setLoading(false);
    }
  }

  async function removeQuestion(questionId) {
    try {
      setErr("");

      await teacherQuizApi.deleteQuestion(id, questionId);

      load();
    } catch (e) {
      setErr(
        e?.response?.data?.message ||
          "Impossible de supprimer la question."
      );
    }
  }

  async function publishQuiz() {
    if (questions.length < 15) {
      setPublishNotice(
        `Publication automatique active : ajoutez encore ${15 - questions.length} question(s).`
      );
      return;
    }

    try {
      setErr("");
      setPublishNotice("Publication du quiz en cours...");

      await teacherQuizApi.publishQuiz(id);

      clearPublishIntent(id);
      navigate("/teacher/quizzes");
    } catch (e) {
      setErr(
        e?.response?.data?.message ||
          "Impossible de publier ce quiz."
      );
      setPublishNotice("");
    }
  }

  const canEdit = !isViewMode && quiz?.status === "DRAFT";
  const shouldAutoPublish = canEdit && hasPublishIntent(id);

  useEffect(() => {
    if (!shouldAutoPublish || loading) {
      setPublishNotice("");
      return;
    }

    if (questions.length < 15) {
      setPublishNotice(
        `Publication automatique active : ajoutez encore ${15 - questions.length} question(s).`
      );
      return;
    }

    if (autoPublishRef.current) return;
    autoPublishRef.current = true;
    void publishQuiz();
  }, [shouldAutoPublish, loading, questions.length]);

  const statusFr = (status) => {
    switch (status) {
      case "DRAFT":
        return "Brouillon";
      case "PUBLISHED":
        return "Publié";
      case "EXPIRED":
        return "Expiré";
      case "ARCHIVED":
        return "Archivé";
      case "DELETED":
        return "Supprimé";
      default:
        return "Brouillon";
    }
  };

  const getQuestionText = (q) =>
    q.enonce || q.questionText || q.text || "Question sans énoncé";

  const getCorrectAnswer = (q) =>
    q.reponseCorrecte || q.correctAnswer || "";

  const getTypeLabel = (type) => {
    if (type === "MCQ" || type === "QCM") return "QCM";
    if (type === "TRUE_FALSE") return "Vrai / Faux";
    if (type === "TEXT") return "Réponse texte";
    return type || "QCM";
  };

  const getOptions = (q) => {
    if (q.type === "TEXT") return [];

    if (Array.isArray(q.options) && q.options.length > 0) {
      return q.options.map((opt, index) => ({
        label: opt.label || ["A", "B", "C", "D"][index],
        text: opt.text || opt.value || opt,
      }));
    }

    if (q.type === "TRUE_FALSE") {
      return [
        { label: "A", text: q.choixA || "Vrai" },
        { label: "B", text: q.choixB || "Faux" },
      ];
    }

    return [
      { label: "A", text: q.choixA },
      { label: "B", text: q.choixB },
      { label: "C", text: q.choixC },
      { label: "D", text: q.choixD },
    ].filter((opt) => opt.text);
  };

  const getQuizSubject = () =>
    quiz?.matiereName ||
    quiz?.matiereNom ||
    quiz?.subjectName ||
    quiz?.theme ||
    "Matiere non definie";

  if (loading) {
    return (
      <div className={styles.loading}>
        Chargement des questions...
      </div>
    );
  }

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <button
          className={styles.backBtn}
          onClick={() => navigate("/teacher/quizzes")}
          aria-label="Retour"
        >
          <ArrowLeft size={18} />
        </button>

        <div className={styles.headerContent}>
          <span className={styles.badge}>
            <BookOpen size={16} />
            {canEdit ? "Gestion des questions" : "Aperçu final"}
          </span>

          <h1>{quiz?.titre || "Questions du quiz"}</h1>

          <p>
            {canEdit
              ? "Organisez les questions, vérifiez les réponses correctes puis publiez le quiz."
              : "Version finale du quiz. Le contenu est visible sans modification."}
          </p>
        </div>

        {canEdit && (
          <Link
            className={styles.addMainBtn}
            to={`/teacher/quizzes/${id}/questions/add`}
          >
            <Plus size={18} />
            Ajouter
          </Link>
        )}
      </div>

      {err && <div className={styles.error}>{err}</div>}
      {publishNotice && <div className={styles.error}>{publishNotice}</div>}

      <section className={styles.infoGrid}>
        <div>
          <span>Statut</span>
          <strong>{statusFr(quiz?.status)}</strong>
        </div>

        <div>
          <span>Matiere</span>
          <strong>{getQuizSubject()}</strong>
        </div>

        <div>
          <span>Durée</span>
          <strong>{quiz?.timeLimit || 0} min</strong>
        </div>

        <div>
          <span>Questions</span>
          <strong>{questions.length}</strong>
        </div>
      </section>

      {questions.length === 0 ? (
        <div className={styles.empty}>
          <BookOpen size={42} />

          <h2>Aucune question</h2>

          <p>Ajoutez au moins une question avant de publier le quiz.</p>

          {canEdit && (
            <Link to={`/teacher/quizzes/${id}/questions/add`}>
              <Plus size={18} />
              Ajouter une question
            </Link>
          )}
        </div>
      ) : (
        <section className={styles.questionsSection}>
          <div className={styles.sectionHeader}>
            <div>
              <h2>Questions du quiz</h2>

              <p>
                Chaque question est affichée dans une carte claire et organisée.
              </p>
            </div>
          </div>

          <div className={styles.questionsList}>
            {questions.map((question, index) => {
              const correctAnswer = getCorrectAnswer(question);
              const type = question.type || "QCM";
              const options = getOptions(question);

              return (
                <article
                  className={styles.questionCard}
                  key={question.id || question._id || index}
                >
                  <div className={styles.questionTop}>
                    <span>Question {index + 1}</span>

                    <div className={styles.questionMeta}>
                      <small>{getTypeLabel(type)}</small>
                      <small>{question.points || 1} pts</small>
                    </div>
                  </div>

                  <div className={styles.questionTextBox}>
                    {getQuestionText(question)}
                  </div>

                  {type === "TEXT" ? (
                    <div className={styles.textAnswerBlock}>
                      <label>Réponse attendue</label>

                      <div className={styles.textAnswer}>
                        {correctAnswer || "Non définie"}
                      </div>
                    </div>
                  ) : (
                    <div className={styles.optionsList}>
                      {options.map((option) => {
                        const isCorrect = correctAnswer === option.label;

                        return (
                          <div
                            key={option.label}
                            className={`${styles.optionRow} ${
                              isCorrect ? styles.correctOption : ""
                            }`}
                          >
                            <div className={styles.optionLetter}>
                              {option.label}
                            </div>

                            <div className={styles.optionText}>
                              {option.text}
                            </div>

                            {isCorrect && (
                              <div className={styles.correctBadge}>
                                <CheckCircle2 size={15} />
                                Bonne réponse
                              </div>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  )}

                  <div className={styles.actions}>
                    {canEdit && (
                      <>
                        <Link
                          className={styles.editBtn}
                          to={`/teacher/quizzes/${id}/questions/add?edit=${question.id || question._id}`}
                        >
                          <Edit size={16} />
                          Modifier
                        </Link>

                        <button
                          className={styles.deleteBtn}
                          onClick={() => removeQuestion(question.id || question._id)}
                        >
                          <Trash2 size={16} />
                          Supprimer
                        </button>
                      </>
                    )}
                  </div>
                </article>
              );
            })}
          </div>
        </section>
      )}

      {false && canEdit && questions.length > 0 && (
        <div className={styles.footerActions}>
          <Link
            className={styles.secondaryBtn}
            to={`/teacher/quizzes/${id}/assign`}
          >
            <Users size={17} />
            Affecter aux étudiants
          </Link>

          <button
            className={styles.primaryBtn}
            onClick={publishQuiz}
          >
            Publier le quiz
          </button>
        </div>
      )}
    </div>
  );
}
