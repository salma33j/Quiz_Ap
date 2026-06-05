import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  ArrowLeft,
  BookOpen,
  CalendarClock,
  ChevronRight,
  Clock,
  FileQuestion,
} from "lucide-react";
import studentQuizApi from "../../api/studentQuizApi";
import styles from "./AvailableQuizzes.module.css";

const unwrap = (res) => res?.data?.data ?? res?.data ?? res ?? [];

const getErrorMessage = (err) =>
  err?.response?.data?.message || err?.message || "Impossible de charger les quiz disponibles.";

const normalizeText = (value) =>
  String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();

const isTodoStatus = (status) => {
  const s = normalizeText(status);
  return s.includes("faire") || s.includes("available") || s.includes("published");
};

const isCompletedOrExpired = (quiz) => {
  const s = normalizeText(quiz?.status);
  if (s.includes("termin") || s.includes("complete") || s.includes("expir")) return true;

  const end = quiz?.availableUntil || quiz?.dateFin || quiz?.deadline || quiz?.endDate;
  if (!end) return false;
  const parsedEnd = new Date(end);
  return !Number.isNaN(parsedEnd.getTime()) && parsedEnd < new Date();
};

const uniqueById = (items) => {
  const map = new Map();
  items.forEach((item) => {
    const id = item?.id || item?.quizId || item?.idQuiz;
    if (!id) return;
    map.set(String(id), item);
  });
  return Array.from(map.values());
};

const getSubjectName = (quiz) =>
  quiz?.matiereName ||
  quiz?.matiereNom ||
  quiz?.subjectName ||
  quiz?.nom ||
  quiz?.name ||
  quiz?.matiere?.nom ||
  quiz?.subject?.name ||
  quiz?.theme ||
  "Matière générale";

const getQuizSubjectId = (quiz) =>
  quiz?.matiereId ||
  quiz?.subjectId ||
  quiz?.matiere?.id ||
  quiz?.subject?.id ||
  null;

const getMatiereId = (matiere) =>
  matiere?.id || matiere?.matiereId || matiere?.subjectId || null;

const getClassLabel = (source) =>
  [source?.className || source?.classeName, source?.classFiliere, source?.classNiveau]
    .filter(Boolean)
    .join(" - ");

const getTeacherLabel = (source) =>
  source?.teacherName ||
  source?.enseignantName ||
  source?.enseignantNom ||
  source?.profName ||
  source?.professeurName ||
  source?.teacher?.name ||
  source?.enseignant?.name ||
  [source?.teacher?.firstName, source?.teacher?.lastName].filter(Boolean).join(" ") ||
  [source?.enseignant?.firstName, source?.enseignant?.lastName].filter(Boolean).join(" ") ||
  "";

const getQuizTitle = (quiz) => quiz?.titre || quiz?.title || quiz?.quizTitle || "Quiz sans titre";

const getQuizId = (quiz) => quiz?.id || quiz?.quizId || quiz?.idQuiz;

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

export default function AvailableQuizzes() {
  const navigate = useNavigate();
  const [quizzes, setQuizzes] = useState([]);
  const [matieres, setMatieres] = useState([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchQuizzes = async () => {
      try {
        setLoading(true);
        setError("");

        const [matieresRes, availableRes, historyRes] = await Promise.allSettled([
          studentQuizApi.getMySubjects(),
          studentQuizApi.getAvailableQuizzes(),
          studentQuizApi.getQuizHistory(),
        ]);

        if (availableRes.status === "rejected" && historyRes.status === "rejected") {
          throw availableRes.reason || historyRes.reason;
        }

        const safeMatieres = matieresRes.status === "fulfilled" ? unwrap(matieresRes.value) : [];
        const safeAvailable = availableRes.status === "fulfilled" ? unwrap(availableRes.value) : [];
        const safeHistory = historyRes.status === "fulfilled" ? unwrap(historyRes.value) : [];

        const availableFromEndpoint = Array.isArray(safeAvailable) ? safeAvailable : [];
        const availableFromHistory = Array.isArray(safeHistory)
          ? safeHistory.filter((quiz) => isTodoStatus(quiz?.status) && !isCompletedOrExpired(quiz))
          : [];

        setMatieres(Array.isArray(safeMatieres) ? safeMatieres : []);
        setQuizzes(uniqueById([...availableFromEndpoint, ...availableFromHistory]));
      } catch (err) {
        setError(getErrorMessage(err));
      } finally {
        setLoading(false);
      }
    };

    fetchQuizzes();
  }, []);

  const subjects = useMemo(() => {
    const grouped = new Map();
    const nameToId = new Map();

    matieres.forEach((matiere) => {
      const matiereId = getMatiereId(matiere);
      const name = getSubjectName(matiere);
      const id = matiereId ? `id:${matiereId}` : `name:${normalizeText(name)}`;

      grouped.set(id, {
        id,
        name,
        classLabel: getClassLabel(matiere),
        teacherName: getTeacherLabel(matiere),
        quizzes: [],
      });

      nameToId.set(normalizeText(name), id);
    });

    quizzes.forEach((quiz) => {
      const name = getSubjectName(quiz);
      const quizSubjectId = getQuizSubjectId(quiz);
      let id = quizSubjectId ? `id:${quizSubjectId}` : nameToId.get(normalizeText(name));

      if (!id) {
        id = `name:${normalizeText(name)}`;
      }

      if (!grouped.has(id)) {
        grouped.set(id, {
          id,
          name,
          classLabel: getClassLabel(quiz),
          teacherName: getTeacherLabel(quiz),
          quizzes: [],
        });
      }

      const subject = grouped.get(id);
      if (!subject.teacherName) subject.teacherName = getTeacherLabel(quiz);
      if (!subject.classLabel) subject.classLabel = getClassLabel(quiz);
      subject.quizzes.push(quiz);
    });

    return Array.from(grouped.values()).sort((a, b) => a.name.localeCompare(b.name));
  }, [matieres, quizzes]);

  const selectedSubject = useMemo(
    () => subjects.find((subject) => String(subject.id) === String(selectedSubjectId)),
    [subjects, selectedSubjectId]
  );

  const visibleQuizzes = selectedSubject?.quizzes || [];

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <span className={styles.badge}>Quiz disponibles</span>
        <h1>{selectedSubject ? selectedSubject.name : "Choisissez une matière"}</h1>
        <p>
          {selectedSubject
            ? selectedSubject.quizzes.length > 0
              ? "Sélectionnez un quiz pour le commencer."
              : "Cette matière est bien dédiée à votre classe, mais aucun quiz n'est disponible pour le moment."
            : "Chaque rectangle représente une matière dédiée à votre classe par l'admin. Les quiz déjà soumis disparaissent automatiquement de cette page."}
        </p>
      </div>

      {loading && <div className={styles.stateBox}>Chargement des quiz...</div>}
      {error && <div className={styles.errorBox}>{error}</div>}

      {!loading && !error && !selectedSubject && (
        <>
          {subjects.length === 0 ? (
            <div className={styles.stateBox}>Aucune matière dédiée à votre classe pour le moment.</div>
          ) : (
            <div className={styles.subjectGrid}>
              {subjects.map((subject) => (
                <button
                  type="button"
                  className={styles.subjectCard}
                  key={subject.id}
                  onClick={() => setSelectedSubjectId(subject.id)}
                >
                  <div className={styles.subjectIcon}>
                    <BookOpen size={30} />
                  </div>
                  <div>
                    <h2>{subject.name}</h2>
                    {subject.teacherName && <small className={styles.teacherLine}>Prof : {subject.teacherName}</small>}
                    {subject.classLabel && <small>{subject.classLabel}</small>}
                    <p>{subject.quizzes.length} quiz disponible(s)</p>
                  </div>
                  <ChevronRight size={22} />
                </button>
              ))}
            </div>
          )}
        </>
      )}

      {!loading && !error && selectedSubject && (
        <>
          <button
            type="button"
            className={styles.backButton}
            onClick={() => setSelectedSubjectId(null)}
            aria-label="Retour aux matières"
          >
            <ArrowLeft size={18} />
          </button>

          {visibleQuizzes.length === 0 ? (
            <div className={styles.stateBox}>Aucun quiz disponible dans cette matière pour le moment.</div>
          ) : (
            <div className={styles.quizGrid}>
              {visibleQuizzes.map((quiz) => (
                <article className={styles.quizCard} key={getQuizId(quiz)}>
                  <div className={styles.quizPreviewTop}>
                    <div className={styles.quizIntro}>
                      <h2>{getQuizTitle(quiz)}</h2>
                      <p>Previsualisation du quiz avant de le commencer.</p>
                    </div>

                    <button
                      type="button"
                      className={styles.startButton}
                      onClick={() => navigate(`/student/quizzes/${getQuizId(quiz)}/take`)}
                    >
                      Commencer le quiz
                    </button>
                  </div>

                  <div className={styles.infoGrid}>
                    <div>
                      <BookOpen size={20} />
                      <span>{getSubjectName(quiz)}</span>
                    </div>
                    <div>
                      <FileQuestion size={20} />
                      <span>{quiz.questionCount || quiz.questions?.length || 0} questions</span>
                    </div>
                    <div>
                      <Clock size={20} />
                      <span>{quiz.timeLimit || quiz.durationMinutes || 0} minutes</span>
                    </div>
                    <div>
                      <CalendarClock size={20} />
                      <span className={styles.deadlineInfo}>
                        <strong>Date limite :</strong>
                        <small>{formatDateTime(quiz.availableUntil || quiz.dateFin || quiz.deadline)}</small>
                      </span>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}
