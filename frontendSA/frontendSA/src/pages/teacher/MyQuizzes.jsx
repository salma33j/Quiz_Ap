import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  BarChart3,
  BookOpen,
  Edit,
  Eye,
  Plus,
  Trash2,
  Users,
  Trophy,
} from "lucide-react";
import teacherQuizApi from "../../api/teacherQuizApi";
import styles from "./MyQuizzes.module.css";

const AI_QUIZ_STORAGE_KEY = "teacher-ai-quizzes";

export default function MyQuizzes() {
  const [items, setItems] = useState([]);
  const [activeFilter, setActiveFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");
  const [cardErrors, setCardErrors] = useState({});

  useEffect(() => {
    load();
  }, []);

  async function load() {
    try {
      setLoading(true);
      setErr("");

      const data = await teacherQuizApi.getMyQuizzes();
      setItems(Array.isArray(data) ? data : []);
    } catch (e) {
      setErr(e?.response?.data?.message || "Impossible de charger les quiz");
    } finally {
      setLoading(false);
    }
  }

  async function remove(id) {
    if (!window.confirm("Supprimer ce quiz ?")) return;

    try {
      await teacherQuizApi.deleteQuiz(id);
      load();
    } catch (e) {
      setErr(e?.response?.data?.message || "Impossible de supprimer ce quiz");
    }
  }

  async function publish(quiz) {
    const questionCount = getQuestionCount(quiz);

    if (questionCount < 10) {
      setCardErrors((prev) => ({
        ...prev,
        [quiz.id]: "Le quiz doit contenir au moins 10 questions avant publication.",
      }));
      return;
    }

    try {
      setCardErrors((prev) => ({ ...prev, [quiz.id]: "" }));
      await teacherQuizApi.publishQuiz(quiz.id);
      load();
    } catch (e) {
      setCardErrors((prev) => ({
        ...prev,
        [quiz.id]: e?.response?.data?.message || "Impossible de publier ce quiz.",
      }));
    }
  }

  const filteredItems = useMemo(() => {
    if (activeFilter === "ALL") return items;
    return items.filter((q) => q.status === activeFilter);
  }, [items, activeFilter]);

  const countByStatus = (status) => {
    if (status === "ALL") return items.length;
    return items.filter((q) => q.status === status).length;
  };

  const getStatusLabel = (status) => {
    switch (status) {
      case "PUBLISHED":
        return "Publié";
      case "DRAFT":
        return "Brouillon";
      case "EXPIRED":
        return "Expiré";
      case "ARCHIVED":
        return "Archivé";
      case "DELETED":
        return "Supprimé";
      default:
        return status || "Brouillon";
    }
  };

  const getStatusClass = (status) => {
    switch (status) {
      case "PUBLISHED":
        return styles.published;
      case "DRAFT":
        return styles.draft;
      case "EXPIRED":
        return styles.expired;
      case "ARCHIVED":
        return styles.archived;
      default:
        return styles.draft;
    }
  };

  const getQuestionCount = (q) =>
    q.questionCount ?? q.questionsCount ?? q.nombreQuestions ?? 0;

  const getDuration = (q) =>
    q.timeLimit ?? q.duration ?? q.duree ?? 0;

  const isAiQuiz = (q) => {
    const storedIds = JSON.parse(localStorage.getItem(AI_QUIZ_STORAGE_KEY) || "[]");

    return (
      String(q.creationType || "").toUpperCase() === "AI" ||
      storedIds.map(String).includes(String(q.id))
    );
  };

  const buildAiQuizState = (q) => ({
    quizId: q.id,
    titre: q.titre || q.title || "",
    theme: q.theme || "",
    description: q.description || "",
    classe: q.classe || "",
    difficulty: q.difficulty || "MOYEN",
    timeLimit: getDuration(q) || 30,
    publishNow: q.status === "PUBLISHED" ? "true" : "false",
    availableFrom: q.availableFrom || null,
    availableUntil: q.availableUntil || null,
    creationType: "AI",
    from: "/teacher/quizzes",
  });

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <div>
          <span className={styles.badge}>Espace quiz</span>

          <h1>Mes quiz</h1>

          <p>
            Gérez vos brouillons, consultez vos quiz publiés et suivez les
            résultats.
          </p>
        </div>

        <Link className={styles.primary} to="/teacher/quizzes/create">
          <Plus size={18} />
          Nouveau quiz
        </Link>
      </div>

      {err && <div className={styles.error}>{err}</div>}

      <div className={styles.filters}>
        <button
          className={activeFilter === "ALL" ? styles.activeFilter : ""}
          onClick={() => setActiveFilter("ALL")}
        >
          Tous
          <span>{countByStatus("ALL")}</span>
        </button>

        <button
          className={activeFilter === "DRAFT" ? styles.activeFilter : ""}
          onClick={() => setActiveFilter("DRAFT")}
        >
          Brouillons
          <span>{countByStatus("DRAFT")}</span>
        </button>

        <button
          className={activeFilter === "PUBLISHED" ? styles.activeFilter : ""}
          onClick={() => setActiveFilter("PUBLISHED")}
        >
          Publiés
          <span>{countByStatus("PUBLISHED")}</span>
        </button>

        <button
          className={activeFilter === "EXPIRED" ? styles.activeFilter : ""}
          onClick={() => setActiveFilter("EXPIRED")}
        >
          Expirés
          <span>{countByStatus("EXPIRED")}</span>
        </button>
      </div>

      {loading ? (
        <div className={styles.loading}>Chargement des quiz...</div>
      ) : filteredItems.length === 0 ? (
        <div className={styles.empty}>Aucun quiz dans cette catégorie.</div>
      ) : (
        <div className={styles.grid}>
          {filteredItems.map((q) => {
            const status = q.status || "DRAFT";
            const isDraft = status === "DRAFT";
            const isPublished = status === "PUBLISHED";
            const isExpired = status === "EXPIRED";

            return (
              <article className={styles.card} key={q.id}>
                <div className={styles.top}>
                  <span>{q.theme || "Sans thème"}</span>

                  <b className={getStatusClass(status)}>
                    {getStatusLabel(status)}
                  </b>
                </div>

                <h3>{q.titre || q.title || "Quiz sans titre"}</h3>

                <p>
                  {q.description ||
                    `Quiz ${
                      isAiQuiz(q)
                        ? "généré avec IA"
                        : "créé manuellement"
                    }.`}
                </p>

                <div className={styles.meta}>
                  <span>
                    <BookOpen size={15} />
                    {getQuestionCount(q)} questions
                  </span>

                  <span>{getDuration(q)} min</span>

                  <span>{q.totalStudentsAllowed ?? 0} étudiants</span>
                </div>

                <div className={styles.actions}>
                  {isDraft && (
                    <>
                      <Link
                        className={styles.editBtn}
                        to={`/teacher/quizzes/create?edit=${q.id}`}
                      >
                        <Edit size={17} />
                        Modifier
                      </Link>

                      <Link
                        className={styles.questionBtn}
                        to={
                          isAiQuiz(q)
                            ? "/teacher/ai-generator"
                            : `/teacher/quizzes/${q.id}/questions`
                        }
                        state={isAiQuiz(q) ? buildAiQuizState(q) : undefined}
                      >
                        <BookOpen size={17} />
                        Questions
                      </Link>

                      <Link
                        className={styles.assignBtn}
                        to={`/teacher/quizzes/${q.id}/assign`}
                        state={{ from: "/teacher/quizzes" }}
                      >
                        <Users size={17} />
                        Affecter
                      </Link>

                      <button
                        className={styles.publishBtn}
                        onClick={() => publish(q)}
                      >
                        Publier
                      </button>

                      <button
                        className={styles.deleteBtn}
                        onClick={() => remove(q.id)}
                      >
                        <Trash2 size={17} />
                      </button>
                    </>
                  )}

                  {cardErrors[q.id] && (
                    <div className={styles.cardError}>{cardErrors[q.id]}</div>
                  )}

                  {isPublished && (
                    <>
                      <Link to={`/teacher/quizzes/${q.id}`}>
                        <Eye size={17} />
                        Voir
                      </Link>

                      <Link to={`/teacher/quizzes/${q.id}/results`}>
                        <BarChart3 size={17} />
                        Résultats
                      </Link>

                      <Link to={`/teacher/quizzes/${q.id}/ranking`}>
                        <Trophy size={17} />
                        Classement
                      </Link>

                      <Link to={`/teacher/quizzes/${q.id}/statistics`}>
                        <BarChart3 size={17} />
                        Stats
                      </Link>
                    </>
                  )}

                  {isExpired && (
                    <>
                      <Link to={`/teacher/quizzes/${q.id}`}>
                        <Eye size={17} />
                        Voir
                      </Link>

                      <Link to={`/teacher/quizzes/${q.id}/results`}>
                        <BarChart3 size={17} />
                        Résultats
                      </Link>

                      <Link to={`/teacher/quizzes/${q.id}/statistics`}>
                        <BarChart3 size={17} />
                        Stats
                      </Link>
                    </>
                  )}

                  {!isDraft && !isPublished && !isExpired && (
                    <Link to={`/teacher/quizzes/${q.id}`}>
                      <Eye size={17} />
                      Voir
                    </Link>
                  )}
                </div>
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
}
