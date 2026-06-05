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

const unwrapResponse = (res) => res?.data?.data ?? res?.data ?? res ?? [];

const getSubjectName = (quiz) =>
  quiz.matiereName ||
  quiz.matiereNom ||
  quiz.subjectName ||
  quiz.nom ||
  quiz.name ||
  quiz.matiere?.nom ||
  quiz.subject?.name ||
  quiz.theme ||
  "Matière générale";

const getQuizSubjectId = (quiz) =>
  quiz.matiereId ||
  quiz.subjectId ||
  quiz.matiere?.id ||
  quiz.subject?.id ||
  getSubjectName(quiz);

const getMatiereId = (matiere) =>
  matiere.id || matiere.matiereId || matiere.subjectId || getSubjectName(matiere);

const getClassLabel = (source) =>
  [source.className || source.classeName, source.classFiliere, source.classNiveau]
    .filter(Boolean)
    .join(" - ");

const getTeacherLabel = (source) =>
  source.teacherName ||
  source.enseignantName ||
  source.enseignantNom ||
  source.profName ||
  source.professeurName ||
  source.teacher?.name ||
  source.enseignant?.name ||
  [source.teacher?.firstName, source.teacher?.lastName].filter(Boolean).join(" ") ||
  [source.enseignant?.firstName, source.enseignant?.lastName].filter(Boolean).join(" ") ||
  "";

const getQuizTitle = (quiz) => quiz.titre || quiz.title || "Quiz sans titre";

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

const unwrap = (res) => res?.data?.data ?? res?.data ?? res ?? [];

const normalizeStatus = (status) =>
  `${status || ""}`
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase();

const getQuizEndDate = (quiz) =>
  quiz?.availableUntil ||
  quiz?.endDate ||
  quiz?.dateFin ||
  quiz?.deadline ||
  quiz?.expiresAt ||
  null;

const getQuizStartDate = (quiz) =>
  quiz?.availableFrom ||
  quiz?.startDate ||
  quiz?.dateDebut ||
  quiz?.startsAt ||
  null;

const isAvailableQuiz = (quiz) => {
  const status = normalizeStatus(quiz?.status);
  const now = new Date();

  if (status.includes("expir") || status.includes("termin") || status.includes("complete")) {
    return false;
  }

  const startDate = getQuizStartDate(quiz);
  const endDate = getQuizEndDate(quiz);
  const parsedStart = startDate ? new Date(startDate) : null;
  const parsedEnd = endDate ? new Date(endDate) : null;

  if (parsedStart && !Number.isNaN(parsedStart.getTime()) && now < parsedStart) return false;
  if (parsedEnd && !Number.isNaN(parsedEnd.getTime()) && now > parsedEnd) return false;

  return status.includes("faire") || status.includes("available") || true;
};

const uniqueById = (items) => {
  const map = new Map();
  items.forEach((item) => {
    const key = String(item?.id || item?.quizId || item?.idQuiz || JSON.stringify(item));
    if (!map.has(key)) map.set(key, item);
  });
  return Array.from(map.values());
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
        const [matieresData, quizzesData] = await Promise.all([
          studentQuizApi.getMySubjects().catch(() => []),
          studentQuizApi.getAvailableQuizzes(),
        ]);

        const safeMatieres = unwrapResponse(matieresData);
        const safeQuizzes = unwrapResponse(quizzesData);

        setMatieres(Array.isArray(safeMatieres) ? safeMatieres : []);
        setQuizzes(Array.isArray(safeQuizzes) ? safeQuizzes : []);
      } catch (err) {
        setError(
          err?.response?.data?.message ||
            err?.message ||
            "Impossible de charger les quiz disponibles."
        );
      } finally {
        setLoading(false);
      }
    };

    fetchQuizzes();
  }, []);

  const subjects = useMemo(() => {
    const grouped = new Map();

    matieres.forEach((matiere) => {
      const id = String(getMatiereId(matiere));

      grouped.set(id, {
        id,
        name: getSubjectName(matiere),
        classLabel: getClassLabel(matiere),
        teacherName: getTeacherLabel(matiere),
        quizzes: [],
      });
    });

    quizzes.forEach((quiz) => {
      const name = getSubjectName(quiz);
      const id = String(getQuizSubjectId(quiz));

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
      if (!subject.teacherName) {
        subject.teacherName = getTeacherLabel(quiz);
      }
      subject.quizzes.push(quiz);
    });

    return Array.from(grouped.values()).sort((a, b) =>
      a.name.localeCompare(b.name)
    );
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
        <h1>
          {selectedSubject
            ? selectedSubject.name
            : "Choisissez une matière"}
        </h1>
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
            <div className={styles.stateBox}>
              Aucune matière dédiée à votre classe pour le moment.
            </div>
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
                    {subject.teacherName && (
                      <small className={styles.teacherLine}>Prof : {subject.teacherName}</small>
                    )}
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
            <div className={styles.stateBox}>
              Aucun quiz disponible dans cette matière pour le moment.
            </div>
          ) : (
            <div className={styles.quizGrid}>
              {visibleQuizzes.map((quiz) => (
                <article className={styles.quizCard} key={quiz.id}>
                  <div className={styles.quizPreviewTop}>
                    <div className={styles.quizIntro}>
                      <h2>{getQuizTitle(quiz)}</h2>
                      <p>Previsualisation du quiz avant de le commencer.</p>
                    </div>

                    <button
                      type="button"
                      className={styles.startButton}
                      onClick={() => navigate(`/student/quizzes/${quiz.id}/take`)}
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
