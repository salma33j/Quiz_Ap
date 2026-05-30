import { useCallback, useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, BookOpen, Search, Users } from "lucide-react";
import teacherQuizApi from "../../api/teacherQuizApi";
import styles from "./AssignStudents.module.css";

const getClassId = (classe) => classe?.id ?? classe?._id;
const getStudentId = (student) => student?.id ?? student?._id ?? student?.email;
const getStudentEmail = (student) => String(student?.email || "").trim().toLowerCase();
const getQuizTitle = (quiz) => quiz?.titre || quiz?.title || "Quiz sans titre";
const getQuizClassId = (quiz) =>
  quiz?.classId ??
  quiz?.classeId ??
  quiz?.classe?.id ??
  quiz?.classe?._id ??
  quiz?.classEntity?.id ??
  quiz?.classEntity?._id;
const getStudentFullName = (student) =>
  [student?.firstName || student?.prenom, student?.lastName || student?.nom]
    .filter(Boolean)
    .join(" ") || "Étudiant";
const getStudentCne = (student) => student?.cne || student?.codeEtudiant || "-";
const getStudentCodeApogee = (student) =>
  student?.codeApoge || student?.codeApogee || student?.studentCodeApogee || "-";

export default function AssignStudents() {
  const { id: quizId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();

  const [classes, setClasses] = useState([]);
  const [students, setStudents] = useState([]);
  const [quizzes, setQuizzes] = useState([]);
  const [selectedClassId, setSelectedClassId] = useState("");
  const [selectedQuizId, setSelectedQuizId] = useState(quizId || "");
  const [studentQuery, setStudentQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [assignedQuizzes, setAssignedQuizzes] = useState([]);
  const [loadingAssignedQuizzes, setLoadingAssignedQuizzes] = useState(false);

  const selectedClass = useMemo(
    () => classes.find((classe) => String(getClassId(classe)) === String(selectedClassId)),
    [classes, selectedClassId]
  );

  const totalStudents = useMemo(
    () => classes.reduce((total, classe) => total + Number(classe.studentCount ?? 0), 0),
    [classes]
  );

  const canUseClassActions = Boolean(selectedClassId);

  const filteredStudents = useMemo(() => {
    const value = studentQuery.trim().toLowerCase();
    if (!value) return students;

    return students.filter((student) =>
      [
        getStudentFullName(student),
        student.email,
        getStudentCne(student),
        getStudentCodeApogee(student),
      ]
        .join(" ")
        .toLowerCase()
        .includes(value)
    );
  }, [students, studentQuery]);

  const loadInitialData = useCallback(async (preferredClassId = "") => {
    try {
      setLoading(true);
      setError("");

      const [classesData, quizzesData] = await Promise.all([
        teacherQuizApi.getClasses(),
        teacherQuizApi.getMyQuizzes(),
      ]);

      const nextClasses = Array.isArray(classesData) ? classesData : [];
      const nextQuizzes = Array.isArray(quizzesData) ? quizzesData : [];
      const nextSelected =
        nextClasses.find((classe) => String(getClassId(classe)) === String(preferredClassId)) ||
        nextClasses[0];

      setClasses(nextClasses);
      setQuizzes(nextQuizzes);
      setSelectedClassId(nextSelected ? String(getClassId(nextSelected)) : "");
    } catch (e) {
      setError(
        e?.response?.data?.message ||
          "Impossible de charger les classes. Vérifiez les endpoints backend."
      );
    } finally {
      setLoading(false);
    }
  }, []);

  const loadStudents = useCallback(async (classId) => {
    try {
      setError("");
      const data = await teacherQuizApi.getClassStudents(classId);
      setStudents(Array.isArray(data) ? data : []);
    } catch (e) {
      setStudents([]);
      setError(
        e?.response?.data?.message ||
          "Impossible de charger les étudiants de cette classe."
      );
    }
  }, []);

  const loadAssignedQuizzes = useCallback(async () => {
    if (!selectedClassId || students.length === 0 || quizzes.length === 0) {
      setAssignedQuizzes([]);
      return;
    }

    try {
      setLoadingAssignedQuizzes(true);

      const classEmails = students.map(getStudentEmail).filter(Boolean);
      const nextAssignedById = new Map();

      quizzes.forEach((quiz) => {
        if (String(getQuizClassId(quiz) || "") === String(selectedClassId)) {
          nextAssignedById.set(String(quiz.id), quiz);
        }
      });

      const quizzesToVerify = quizzes.filter((quiz) => {
        if (nextAssignedById.has(String(quiz.id))) return false;
        return getQuizClassId(quiz) == null;
      });

      if (classEmails.length > 0 && quizzesToVerify.length > 0) {
        await Promise.all(
          quizzesToVerify.map(async (quiz) => {
            try {
              const quizStudents = await teacherQuizApi.getQuizStudents(quiz.id);
              const allowedEmails = new Set(
                (Array.isArray(quizStudents) ? quizStudents : [])
                  .map(getStudentEmail)
                  .filter(Boolean)
              );

              if (classEmails.every((email) => allowedEmails.has(email))) {
                nextAssignedById.set(String(quiz.id), quiz);
              }
            } catch {
              // La page reste utilisable même si un quiz isolé ne renvoie pas ses étudiants.
            }
          })
        );
      }

      const nextAssigned = Array.from(nextAssignedById.values()).sort((a, b) =>
        getQuizTitle(a).localeCompare(getQuizTitle(b), "fr", { sensitivity: "base" })
      );

      setAssignedQuizzes(nextAssigned);
    } finally {
      setLoadingAssignedQuizzes(false);
    }
  }, [selectedClassId, students, quizzes]);

  useEffect(() => {
    let cancelled = false;

    async function run() {
      await Promise.resolve();
      if (!cancelled) {
        await loadInitialData();
      }
    }

    run();

    return () => {
      cancelled = true;
    };
  }, [loadInitialData]);

  useEffect(() => {
    if (!selectedClassId) return;
    let cancelled = false;

    async function run() {
      await Promise.resolve();
      if (!cancelled) {
        await loadStudents(selectedClassId);
      }
    }

    run();

    return () => {
      cancelled = true;
    };
  }, [selectedClassId, loadStudents]);

  useEffect(() => {
    let cancelled = false;

    async function run() {
      await Promise.resolve();
      if (!cancelled) {
        await loadAssignedQuizzes();
      }
    }

    run();

    return () => {
      cancelled = true;
    };
  }, [loadAssignedQuizzes]);

  function resetNotice() {
    setError("");
    setMessage("");
  }

  function selectClass(classId) {
    resetNotice();
    setStudentQuery("");
    setStudents([]);
    setAssignedQuizzes([]);
    setSelectedClassId(String(classId));
  }

  function requireClass() {
    if (selectedClassId) return true;
    setError("Sélectionnez une classe avant de continuer.");
    return false;
  }

  async function assignQuizToClass() {
    if (!requireClass()) return;

    if (!selectedQuizId) {
      setError("Veuillez sélectionner un quiz.");
      return;
    }

    try {
      resetNotice();
      setWorking("assign");
      await teacherQuizApi.assignQuizToClass(selectedQuizId, selectedClassId);

      const refreshedQuizzes = await teacherQuizApi.getMyQuizzes().catch(() => quizzes);
      setQuizzes(Array.isArray(refreshedQuizzes) ? refreshedQuizzes : quizzes);
      setMessage("Quiz affecté à toute la classe avec succès.");
    } catch (e) {
      setError(e?.response?.data?.message || "Impossible d'affecter le quiz à la classe.");
    } finally {
      setWorking("");
    }
  }

  if (loading) {
    return <div className={styles.loading}>Chargement des classes...</div>;
  }

  return (
    <div className={styles.page}>
      <section className={styles.hero}>
        {quizId && (
          <button
            type="button"
            className={styles.backBtn}
            onClick={() => navigate(location.state?.from || "/teacher/quizzes")}
            aria-label="Retour"
          >
            <ArrowLeft size={20} />
          </button>
        )}

        <span className={styles.badge}>
          <Users size={16} />
          Classes & étudiants
        </span>

        <h1>Gestion des étudiants</h1>
        <p>
          Consultez les classes créées par l'administration, vérifiez les étudiants et
          affectez vos quiz aux groupes disponibles.
        </p>
      </section>

      {error && <div className={styles.error}>{error}</div>}
      {message && <div className={styles.success}>{message}</div>}

      <section className={styles.stats}>
        <div>
          <span>Classes</span>
          <strong>{classes.length}</strong>
        </div>
        <div>
          <span>Étudiants</span>
          <strong>{totalStudents}</strong>
        </div>
        <div>
          <span>Classe active</span>
          <strong>{selectedClass?.name || "Aucune"}</strong>
        </div>
        <div>
          <span>Quiz affectés {selectedClass ? `à ${selectedClass.name}` : ""}</span>
          <strong>{loadingAssignedQuizzes ? "..." : assignedQuizzes.length}</strong>
        </div>
      </section>

      <div className={styles.layout}>
        <aside className={styles.classesPanel}>
          <div className={styles.panelTitle}>
            <div>
              <h2>Classes</h2>
              <p>Choisissez le groupe de travail.</p>
            </div>
          </div>

          <div className={styles.classList}>
            {classes.length === 0 ? (
              <div className={styles.emptySmall}>
                Aucune classe disponible. Les classes sont créées par l'admin.
              </div>
            ) : (
              classes.map((classe) => {
                const classId = getClassId(classe);
                const isActive = String(selectedClassId) === String(classId);

                return (
                  <div
                    key={classId}
                    className={`${styles.classItem} ${isActive ? styles.activeClass : ""}`}
                  >
                    <button
                      type="button"
                      className={styles.classSelect}
                      onClick={() => selectClass(classId)}
                    >
                      <strong>{classe.name}</strong>
                      {(classe.filiere || classe.niveau) && (
                        <small className={styles.classMeta}>
                          {classe.filiere && <em>{classe.filiere}</em>}
                          {classe.niveau && <em>{classe.niveau}</em>}
                        </small>
                      )}
                      <span>{classe.studentCount ?? 0} étudiants</span>
                    </button>
                  </div>
                );
              })
            )}
          </div>
        </aside>

        <main className={styles.content}>
          <section className={styles.card}>
            <div className={styles.cardHeader}>
              <div>
                <h2>Affecter un quiz à la classe</h2>
                <p>Tous les étudiants de la classe sélectionnée recevront ce quiz.</p>
              </div>
            </div>

            <div className={styles.assignBox}>
              <select
                value={selectedQuizId}
                onChange={(e) => setSelectedQuizId(e.target.value)}
                disabled={!canUseClassActions}
              >
                <option value="">Choisir un quiz</option>
                {quizzes.map((quiz) => (
                  <option key={quiz.id} value={quiz.id}>
                    {getQuizTitle(quiz)}
                  </option>
                ))}
              </select>

              <button
                type="button"
                onClick={assignQuizToClass}
                disabled={!canUseClassActions || !selectedQuizId || working === "assign"}
              >
                <BookOpen size={18} />
                {working === "assign" ? "Affectation..." : "Affecter à la classe"}
              </button>
            </div>
          </section>

          <section className={styles.card}>
            <div className={styles.cardHeader}>
              <div>
                <h2>Quiz affectés à {selectedClass?.name || "la classe sélectionnée"}</h2>
                <p>
                  Total : {loadingAssignedQuizzes ? "..." : assignedQuizzes.length} quiz
                  affecté(s) à tous les étudiants de cette classe.
                </p>
              </div>
            </div>

            {loadingAssignedQuizzes ? (
              <div className={styles.empty}>Chargement des quiz affectés...</div>
            ) : assignedQuizzes.length === 0 ? (
              <div className={styles.empty}>Aucun quiz affecté à cette classe.</div>
            ) : (
              <div className={styles.assignedQuizList}>
                {assignedQuizzes.map((quiz) => (
                  <div className={styles.assignedQuizItem} key={quiz.id}>
                    <BookOpen size={18} />
                    <div>
                      <strong>{getQuizTitle(quiz)}</strong>
                      <span>{quiz.theme || quiz.status || "Quiz"}</span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>

          <section className={styles.card}>
            <div className={styles.cardHeader}>
              <div>
                <h2>Étudiants de {selectedClass?.name || "la classe sélectionnée"}</h2>
                <p>Liste en consultation seulement. La création des comptes est réservée à l'admin.</p>
              </div>
            </div>

            <div className={styles.searchBox}>
              <Search size={18} />
              <input
                value={studentQuery}
                onChange={(event) => setStudentQuery(event.target.value)}
                placeholder="Rechercher par nom, email, CNE ou Code Apogée..."
              />
            </div>

            {students.length === 0 ? (
              <div className={styles.empty}>
                {selectedClass
                  ? "Aucun étudiant dans cette classe."
                  : "Sélectionnez une classe pour afficher ses étudiants."}
              </div>
            ) : filteredStudents.length === 0 ? (
              <div className={styles.empty}>Aucun étudiant ne correspond à cette recherche.</div>
            ) : (
              <div className={styles.table}>
                <div className={styles.tableHead}>
                  <span>Nom</span>
                  <span>Email</span>
                  <span>CNE</span>
                  <span>Code Apogée</span>
                </div>

                {filteredStudents.map((student) => (
                  <div className={styles.tableRow} key={getStudentId(student)}>
                    <span>
                      <strong>{getStudentFullName(student)}</strong>
                    </span>
                    <span>{student.email}</span>
                    <span>{getStudentCne(student)}</span>
                    <span>{getStudentCodeApogee(student)}</span>
                  </div>
                ))}
              </div>
            )}
          </section>
        </main>
      </div>
    </div>
  );
}
