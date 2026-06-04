import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  BarChart3,
  BookOpen,
  Edit,
  Eye,
  GraduationCap,
  Plus,
  Trash2,
  Trophy,
  Users,
} from "lucide-react";
import teacherQuizApi from "../../api/teacherQuizApi";
import ConfirmDialog from "../../components/common/ConfirmDialog";
import styles from "./MyQuizzes.module.css";

const AI_QUIZ_STORAGE_KEY = "teacher-ai-quizzes";
const MIN_QUESTIONS_TO_PUBLISH = 15;

const addMonths = (date, months) => {
  const next = new Date(date);
  next.setMonth(next.getMonth() + months);
  return next;
};

const getQuizCreatedAt = (quiz) =>
  quiz?.availableFrom || quiz?.createdAt || quiz?.createdDate || quiz?.publishedAt;

const hasReachedDeleteDelay = (quiz) => {
  const reference = getQuizCreatedAt(quiz);
  if (!reference) return false;

  const referenceDate = new Date(reference);
  return Number.isFinite(referenceDate.getTime()) && addMonths(referenceDate, 5) <= new Date();
};

const canDeleteQuiz = (quiz) => {
  const status = String(quiz?.status || "DRAFT").toUpperCase();
  if (status !== "PUBLISHED") return true;

  return hasReachedDeleteDelay(quiz);
};

const quizActionMessage = (error, fallback) => {
  const raw =
    error?.response?.data?.message ||
    error?.response?.data?.error ||
    error?.message ||
    "";
  const text = raw.toLowerCase();

  if (
    text.includes("foreign key") ||
    text.includes("constraint") ||
    text.includes("could not execute statement") ||
    text.includes("sql")
  ) {
    return "Impossible de supprimer ce quiz car il est encore lié à des questions, réponses ou résultats.";
  }

  return raw || fallback;
};

export default function MyQuizzes() {
  const [items, setItems] = useState([]);
  const [activeFilter, setActiveFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");
  const [cardErrors, setCardErrors] = useState({});
  const [confirmDialog, setConfirmDialog] = useState(null);

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
    const confirmed = await new Promise((resolve) => {
      setConfirmDialog({
        title: "Supprimer ce quiz",
        message: "Le quiz sera retiré de votre liste. Confirmez uniquement si vous voulez vraiment le supprimer.",
        confirmLabel: "Supprimer",
        cancelLabel: "Annuler",
        resolve,
      });
    });

    if (!confirmed) return;

    try {
      setCardErrors((prev) => ({ ...prev, [id]: "" }));
      await teacherQuizApi.deleteQuiz(id);
      load();
    } catch (e) {
      setCardErrors((prev) => ({
        ...prev,
        [id]: quizActionMessage(e, "Impossible de supprimer ce quiz."),
      }));
    }
  }

  async function publish(quiz) {
    const questionCount = getQuestionCount(quiz);

    if (questionCount < MIN_QUESTIONS_TO_PUBLISH) {
      setCardErrors((prev) => ({
        ...prev,
        [quiz.id]: `Le quiz doit contenir au moins ${MIN_QUESTIONS_TO_PUBLISH} questions avant publication.`,
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

  const isAnalyticsAvailable = (q) => {
    const status = String(q.status || "").toUpperCase();
    if (status === "EXPIRED") return true;
    if (status !== "PUBLISHED" || !q.availableUntil) return false;

    const expirationDate = new Date(q.availableUntil);
    return !Number.isNaN(expirationDate.getTime()) && expirationDate <= new Date();
  };

  const formatExpirationDate = (q) => {
    if (!q.availableUntil) return "date d'expiration";

    const expirationDate = new Date(q.availableUntil);
    if (Number.isNaN(expirationDate.getTime())) return "date d'expiration";

    return expirationDate.toLocaleDateString("fr-FR", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const getQuestionCount = (q) =>
    q.questionCount ?? q.questionsCount ?? q.nombreQuestions ?? 0;

  const getDuration = (q) => q.timeLimit ?? q.duration ?? q.duree ?? 0;

  const getAllowedCount = (q) =>
    q.totalStudentsAllowed ?? q.allowedStudentsCount ?? q.studentCount ?? 0;

  const getClassLabel = (q) => {
    const raw =
      q.classeName ||
      q.className ||
      q.nomClasse ||
      q.groupeName ||
      q.groupName ||
      q.classe?.name ||
      q.classe?.nom ||
      q.classEntity?.name ||
      q.classEntity?.nom ||
      q.classe ||
      q.class ||
      q.groupe ||
      q.group;

    return typeof raw === "string" && raw.trim()
      ? raw.trim()
      : "Classe non définie";
  };

  const getClassDetails = (q) =>
    [
      q.classFiliere ||
        q.classeFiliere ||
        q.filiere ||
        q.classe?.filiere ||
        q.classEntity?.filiere,
      q.classNiveau ||
        q.classeNiveau ||
        q.niveau ||
        q.classe?.niveau ||
        q.classEntity?.niveau,
    ]
      .filter(Boolean)
      .join(" · ");

  const getSubjectLabel = (q) => {
    const raw =
      q.matiereName ||
      q.matiereNom ||
      q.subjectName ||
      q.subject ||
      q.matiere?.nom ||
      q.matiere?.name ||
      q.theme;

    return typeof raw === "string" && raw.trim()
      ? raw.trim()
      : "Matière non définie";
  };

  const isAiQuiz = (q) => {
    const storedIds = (() => {
      try {
        const parsed = JSON.parse(localStorage.getItem(AI_QUIZ_STORAGE_KEY) || "[]");
        return Array.isArray(parsed) ? parsed : [];
      } catch {
        return [];
      }
    })();

    return (
      String(q.creationType || "").toUpperCase() === "AI" ||
      storedIds.map(String).includes(String(q.id))
    );
  };

  const buildAiQuizState = (q) => ({
    quizId: q.id,
    titre: q.titre || q.title || "",
    theme: getSubjectLabel(q),
    matiere: getSubjectLabel(q),
    description: q.description || "",
    classe: getClassLabel(q),
    className: getClassLabel(q),
    classFiliere: q.classFiliere || q.classeFiliere || q.filiere || q.classe?.filiere || "",
    classNiveau: q.classNiveau || q.classeNiveau || q.niveau || q.classe?.niveau || "",
    difficulty: q.difficulty || "MOYEN",
    timeLimit: getDuration(q) || 30,
    publishNow: q.status === "PUBLISHED" ? "true" : "false",
    availableFrom: q.availableFrom || null,
    availableUntil: q.availableUntil || null,
    creationType: "AI",
    matiereId: q.matiereId || q.subjectId || q.matiere?.id || q.subject?.id || null,
    matiereNom: getSubjectLabel(q),
    from: "/teacher/quizzes",
  });

  const groupedItems = useMemo(() => {
    const classes = new Map();

    filteredItems.forEach((quiz) => {
      const classLabel = getClassLabel(quiz);
      const classKey = classLabel.toLowerCase();
      const subjectLabel = getSubjectLabel(quiz);
      const subjectKey = subjectLabel.toLowerCase();

      if (!classes.has(classKey)) {
        classes.set(classKey, {
          label: classLabel,
          details: getClassDetails(quiz),
          subjects: new Map(),
          total: 0,
        });
      }

      const classGroup = classes.get(classKey);
      classGroup.total += 1;

      if (!classGroup.details) {
        classGroup.details = getClassDetails(quiz);
      }

      if (!classGroup.subjects.has(subjectKey)) {
        classGroup.subjects.set(subjectKey, {
          label: subjectLabel,
          quizzes: [],
        });
      }

      classGroup.subjects.get(subjectKey).quizzes.push(quiz);
    });

    return Array.from(classes.values())
      .sort((a, b) => {
        if (a.label === "Classe non définie") return 1;
        if (b.label === "Classe non définie") return -1;
        return a.label.localeCompare(b.label, "fr", { sensitivity: "base" });
      })
      .map((classGroup) => ({
        ...classGroup,
        subjects: Array.from(classGroup.subjects.values())
          .sort((a, b) => {
            if (a.label === "Matière non définie") return 1;
            if (b.label === "Matière non définie") return -1;
            return a.label.localeCompare(b.label, "fr", { sensitivity: "base" });
          })
          .map((subjectGroup) => ({
            ...subjectGroup,
            quizzes: subjectGroup.quizzes.sort((a, b) =>
              (a.titre || a.title || "").localeCompare(
                b.titre || b.title || "",
                "fr",
                { sensitivity: "base" }
              )
            ),
          })),
      }));
  }, [filteredItems]);

  const renderQuizCard = (q) => {
    const status = q.status || "DRAFT";
    const isDraft = status === "DRAFT";
    const isPublished = status === "PUBLISHED";
    const isExpired = status === "EXPIRED";
    const analyticsAvailable = isAnalyticsAvailable(q);
    const canDelete = canDeleteQuiz(q);

    return (
      <article className={styles.card} key={q.id}>
        <div className={styles.top}>
          <span>{getSubjectLabel(q)}</span>

          <b className={getStatusClass(status)}>{getStatusLabel(status)}</b>
        </div>

        <h3>{q.titre || q.title || "Quiz sans titre"}</h3>

        <p>
          {q.description ||
            `Quiz ${isAiQuiz(q) ? "généré avec IA" : "créé manuellement"}.`}
        </p>

        <div className={styles.meta}>
          <span className={styles.classMetaBadge}>
            <GraduationCap size={15} />
            {getClassLabel(q)}
          </span>

          <span>
            <BookOpen size={15} />
            {getQuestionCount(q)} questions
          </span>

          <span>{getDuration(q)} min</span>

          <span>{getAllowedCount(q)} etudiants</span>
        </div>

        <div className={styles.actions}>
          {isDraft && (
            <>
              <Link className={styles.editBtn} to={`/teacher/quizzes/create?edit=${q.id}`}>
                <Edit size={17} />
                Modifier
              </Link>

              <Link
                className={styles.questionBtn}
                to={isAiQuiz(q) ? "/teacher/ai-generator" : `/teacher/quizzes/${q.id}/questions`}
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

              <button className={styles.publishBtn} onClick={() => publish(q)}>
                Publier
              </button>

            </>
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

              {analyticsAvailable ? (
                <>
                  <Link to={`/teacher/quizzes/${q.id}/ranking`}>
                    <Trophy size={17} />
                    Classement
                  </Link>

                  <Link to={`/teacher/quizzes/${q.id}/statistics`}>
                    <BarChart3 size={17} />
                    Stats
                  </Link>
                </>
              ) : (
                <span className={styles.lockedAction}>
                  Classement et stats après {formatExpirationDate(q)}
                </span>
              )}
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

              <Link to={`/teacher/quizzes/${q.id}/ranking`}>
                <Trophy size={17} />
                Classement
              </Link>
            </>
          )}

          {!isDraft && !isPublished && !isExpired && (
            <Link to={`/teacher/quizzes/${q.id}`}>
              <Eye size={17} />
              Voir
            </Link>
          )}

          {canDelete && (
            <button className={styles.deleteBtn} onClick={() => remove(q.id)}>
              <Trash2 size={17} />
              Supprimer
            </button>
          )}

          {cardErrors[q.id] && (
            <div className={styles.cardError}>{cardErrors[q.id]}</div>
          )}
        </div>
      </article>
    );
  };

  return (
    <div className={styles.page}>
      <ConfirmDialog
        open={Boolean(confirmDialog)}
        title={confirmDialog?.title}
        message={confirmDialog?.message}
        confirmLabel={confirmDialog?.confirmLabel}
        cancelLabel={confirmDialog?.cancelLabel}
        onCancel={() => {
          confirmDialog?.resolve(false);
          setConfirmDialog(null);
        }}
        onConfirm={() => {
          confirmDialog?.resolve(true);
          setConfirmDialog(null);
        }}
      />

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
        <div className={styles.classGroups}>
          {groupedItems.map((classGroup) => (
            <section className={styles.classGroup} key={classGroup.label}>
              <div className={styles.classHeader}>
                <div>
                  <span className={styles.groupEyebrow}>Classe</span>
                  <h2>{classGroup.label}</h2>
                  {classGroup.details && <p>{classGroup.details}</p>}
                </div>

                <strong>{classGroup.total} quiz</strong>
              </div>

              {classGroup.subjects.map((subjectGroup) => (
                <div className={styles.subjectGroup} key={subjectGroup.label}>
                  <div className={styles.subjectHeader}>
                    <div>
                      <span className={styles.groupEyebrow}>Matière</span>
                      <h3>{subjectGroup.label}</h3>
                    </div>

                    <span>{subjectGroup.quizzes.length} quiz</span>
                  </div>

                  <div className={styles.grid}>
                    {subjectGroup.quizzes.map((q) => renderQuizCard(q))}
                  </div>
                </div>
              ))}
            </section>
          ))}
        </div>
      )}
    </div>
  );
}
