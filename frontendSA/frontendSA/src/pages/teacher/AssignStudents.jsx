import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import {
  ArrowLeft,
  BookOpen,
  FileSpreadsheet,
  Plus,
  Trash2,
  Upload,
  Users,
} from "lucide-react";
import teacherQuizApi from "../../api/teacherQuizApi";
import styles from "./AssignStudents.module.css";

const emptyStudent = {
  firstName: "",
  lastName: "",
  email: "",
  cne: "",
};

const emptyClass = {
  name: "",
  filiere: "",
  niveau: "",
};

const getClassId = (classe) => classe?.id ?? classe?._id;
const getStudentId = (student) => student?.id ?? student?._id;
const getStudentEmail = (student) => String(student?.email || "").trim().toLowerCase();
const getQuizTitle = (quiz) => quiz?.titre || quiz?.title || "Quiz sans titre";

export default function AssignStudents() {
  const { id: quizId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();

  const [classes, setClasses] = useState([]);
  const [students, setStudents] = useState([]);
  const [quizzes, setQuizzes] = useState([]);

  const [selectedClassId, setSelectedClassId] = useState("");
  const [selectedQuizId, setSelectedQuizId] = useState(quizId || "");
  const [mode, setMode] = useState("manual");

  const [classForm, setClassForm] = useState(emptyClass);
  const [studentForm, setStudentForm] = useState(emptyStudent);
  const [file, setFile] = useState(null);

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
    () =>
      classes.reduce((total, classe) => total + Number(classe.studentCount ?? 0), 0),
    [classes]
  );

  const canUseClassActions = Boolean(selectedClassId);

  useEffect(() => {
    loadInitialData();
  }, []);

  useEffect(() => {
    if (!selectedClassId) {
      setStudents([]);
      setAssignedQuizzes([]);
      return;
    }

    setStudents([]);
    setAssignedQuizzes([]);
    loadStudents(selectedClassId);
  }, [selectedClassId]);

  useEffect(() => {
    loadAssignedQuizzes();
  }, [selectedClassId, students, quizzes]);

  async function loadInitialData(preferredClassId = selectedClassId) {
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
  }

  async function loadStudents(classId) {
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
  }

  async function loadAssignedQuizzes() {
    if (!selectedClassId || students.length === 0 || quizzes.length === 0) {
      setAssignedQuizzes([]);
      return;
    }

    try {
      setLoadingAssignedQuizzes(true);

      const classEmails = students.map(getStudentEmail).filter(Boolean);
      if (classEmails.length === 0) {
        setAssignedQuizzes([]);
        return;
      }

      const nextAssigned = [];

      await Promise.all(
        quizzes.map(async (quiz) => {
          try {
            const quizStudents = await teacherQuizApi.getQuizStudents(quiz.id);
            const allowedEmails = new Set(
              (Array.isArray(quizStudents) ? quizStudents : [])
                .map(getStudentEmail)
                .filter(Boolean)
            );

            if (classEmails.every((email) => allowedEmails.has(email))) {
              nextAssigned.push(quiz);
            }
          } catch {
            // Keep the page usable if one quiz lookup fails.
          }
        })
      );

      nextAssigned.sort((a, b) => getQuizTitle(a).localeCompare(getQuizTitle(b)));
      setAssignedQuizzes(nextAssigned);
    } finally {
      setLoadingAssignedQuizzes(false);
    }
  }

  function resetNotice() {
    setError("");
    setMessage("");
  }

  function requireClass() {
    if (selectedClassId) return true;
    setError("Créez ou sélectionnez une classe avant de continuer.");
    return false;
  }

  function updateStudentField(e) {
    const { name, value } = e.target;
    setStudentForm((prev) => ({ ...prev, [name]: value }));
  }

  function updateClassField(e) {
    const { name, value } = e.target;
    setClassForm((prev) => ({ ...prev, [name]: value }));
  }

  async function createClass(event) {
    event.preventDefault();

    const name = classForm.name.trim();
    if (!name) {
      setError("Veuillez saisir le nom de la classe.");
      return;
    }

    try {
      resetNotice();
      setWorking("class");

      const created = await teacherQuizApi.createClass({
        name,
        filiere: classForm.filiere.trim(),
        niveau: classForm.niveau.trim(),
      });

      setClassForm(emptyClass);
      await loadInitialData(created?.id);
      setMessage("Classe créée avec succès.");
    } catch (e) {
      setError(e?.response?.data?.message || "Impossible de créer la classe.");
    } finally {
      setWorking("");
    }
  }

  async function deleteClass(classId) {
    const classe = classes.find((item) => String(getClassId(item)) === String(classId));
    const ok = window.confirm(`Supprimer la classe "${classe?.name || ""}" ?`);
    if (!ok) return;

    try {
      resetNotice();
      setWorking(`delete-class-${classId}`);
      await teacherQuizApi.deleteClass(classId);
      await loadInitialData("");
      setMessage("Classe supprimée avec succès.");
    } catch (e) {
      setError(e?.response?.data?.message || "Impossible de supprimer la classe.");
    } finally {
      setWorking("");
    }
  }

  async function addStudent(event) {
    event.preventDefault();
    if (!requireClass()) return;

    const payload = {
      firstName: studentForm.firstName.trim(),
      lastName: studentForm.lastName.trim(),
      email: studentForm.email.trim(),
      cne: studentForm.cne.trim(),
    };

    if (!payload.firstName || !payload.lastName || !payload.email || !payload.cne) {
      setError("Veuillez remplir le prénom, le nom, l'email et le CNE.");
      return;
    }

    try {
      resetNotice();
      setWorking("student");
      await teacherQuizApi.addStudentToClass(selectedClassId, payload);
      setStudentForm(emptyStudent);
      await loadStudents(selectedClassId);
      await loadInitialData(selectedClassId);
      setMessage("Étudiant ajouté avec succès.");
    } catch (e) {
      setError(e?.response?.data?.message || "Impossible d'ajouter l'étudiant.");
    } finally {
      setWorking("");
    }
  }

  async function deleteStudent(studentId) {
    if (!requireClass()) return;

    try {
      resetNotice();
      setWorking(`delete-student-${studentId}`);
      await teacherQuizApi.deleteStudentFromClass(selectedClassId, studentId);
      await loadStudents(selectedClassId);
      await loadInitialData(selectedClassId);
      setMessage("Étudiant supprimé de la classe.");
    } catch (e) {
      setError(e?.response?.data?.message || "Impossible de supprimer l'étudiant.");
    } finally {
      setWorking("");
    }
  }

  async function importExcel() {
    if (!requireClass()) return;

    if (!file) {
      setError("Veuillez choisir un fichier Excel.");
      return;
    }

    try {
      resetNotice();
      setWorking("import");
      await teacherQuizApi.importStudentsToClass(selectedClassId, file);
      setFile(null);
      await loadStudents(selectedClassId);
      await loadInitialData(selectedClassId);
      setMessage("Étudiants importés avec succès.");
    } catch (e) {
      setError(e?.response?.data?.message || "Erreur lors de l'import Excel.");
    } finally {
      setWorking("");
    }
  }

  async function assignQuizToClass() {
    if (!requireClass()) return;

    if (!selectedQuizId) {
      setError("Veuillez selectionner un quiz.");
      return;
    }

    try {
      resetNotice();
      setWorking("assign");
      await teacherQuizApi.assignQuizToClass(selectedQuizId, selectedClassId);
      await loadAssignedQuizzes();
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
          Classes & Étudiants
        </span>

        <h1>Gestion des classes</h1>
        <p>
          Créez vos classes, ajoutez les étudiants et affectez un quiz à tout un
          groupe depuis un seul espace.
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

          <form className={styles.createClassBox} onSubmit={createClass}>
            <input
              name="name"
              value={classForm.name}
              onChange={updateClassField}
              placeholder="Ex : L3 Informatique"
            />
            <input
              name="filiere"
              value={classForm.filiere}
              onChange={updateClassField}
              placeholder="Filière"
            />
            <input
              name="niveau"
              value={classForm.niveau}
              onChange={updateClassField}
              placeholder="Niveau"
            />
            <button type="submit" disabled={working === "class"} aria-label="Créer la classe">
              <Plus size={18} />
            </button>
          </form>

          <div className={styles.classList}>
            {classes.length === 0 ? (
              <div className={styles.emptySmall}>Aucune classe créée.</div>
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
                      onClick={() => {
                        resetNotice();
                        setSelectedClassId(String(classId));
                      }}
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

                    <button
                      type="button"
                      className={styles.classDelete}
                      onClick={() => deleteClass(classId)}
                      disabled={working === `delete-class-${classId}`}
                      aria-label={`Supprimer ${classe.name}`}
                    >
                      <Trash2 size={16} />
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
                <h2>Ajouter des étudiants</h2>
                <p>
                  {selectedClass
                    ? `Classe sélectionnée : ${selectedClass.name}`
                    : "Créez une classe pour activer l'ajout d'étudiants."}
                </p>
              </div>
            </div>

            <div className={styles.modeGrid}>
              <button
                type="button"
                className={`${styles.modeCard} ${mode === "manual" ? styles.modeActive : ""}`}
                onClick={() => setMode("manual")}
                disabled={!canUseClassActions}
              >
                <Users size={22} />
                <strong>Ajout manuel</strong>
                <span>Ajouter un étudiant à la fois.</span>
              </button>

              <button
                type="button"
                className={`${styles.modeCard} ${mode === "excel" ? styles.modeActive : ""}`}
                onClick={() => setMode("excel")}
                disabled={!canUseClassActions}
              >
                <FileSpreadsheet size={22} />
                <strong>Import Excel</strong>
                <span>Importer plusieurs étudiants.</span>
              </button>
            </div>

            {mode === "manual" ? (
              <form className={styles.manualForm} onSubmit={addStudent}>
                <input
                  name="firstName"
                  value={studentForm.firstName}
                  onChange={updateStudentField}
                  placeholder="Prénom"
                  disabled={!canUseClassActions}
                />
                <input
                  name="lastName"
                  value={studentForm.lastName}
                  onChange={updateStudentField}
                  placeholder="Nom"
                  disabled={!canUseClassActions}
                />
                <input
                  name="email"
                  type="email"
                  value={studentForm.email}
                  onChange={updateStudentField}
                  placeholder="Email"
                  disabled={!canUseClassActions}
                />
                <input
                  name="cne"
                  value={studentForm.cne}
                  onChange={updateStudentField}
                  placeholder="CNE / Code étudiant"
                  disabled={!canUseClassActions}
                />

                <button
                  type="submit"
                  disabled={!canUseClassActions || working === "student"}
                >
                  <Plus size={18} />
                  Ajouter étudiant
                </button>
              </form>
            ) : (
              <div className={styles.importBox}>
                <label className={styles.uploadArea}>
                  <Upload size={28} />
                  <strong>Importer un fichier Excel</strong>
                  <span>Format accepté : .xlsx ou .xls</span>
                  <input
                    type="file"
                    accept=".xlsx,.xls"
                    onChange={(e) => setFile(e.target.files?.[0] || null)}
                    disabled={!canUseClassActions}
                  />
                  {file && <p>{file.name}</p>}
                </label>

                <button
                  type="button"
                  onClick={importExcel}
                  disabled={!canUseClassActions || working === "import"}
                >
                  Importer les étudiants
                </button>
              </div>
            )}
          </section>

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
                    {quiz.titre || quiz.title || "Quiz sans titre"}
                  </option>
                ))}
              </select>

              <button
                type="button"
                onClick={assignQuizToClass}
                disabled={!canUseClassActions || !selectedQuizId || working === "assign"}
              >
                <BookOpen size={18} />
                Affecter à la classe
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
                <p>Consultez et nettoyez la liste de la classe active.</p>
              </div>
            </div>

            {students.length === 0 ? (
              <div className={styles.empty}>
                {selectedClass
                  ? "Aucun étudiant dans cette classe."
                  : "Sélectionnez une classe pour afficher ses étudiants."}
              </div>
            ) : (
              <div className={styles.table}>
                <div className={styles.tableHead}>
                  <span>Nom</span>
                  <span>Email</span>
                  <span>CNE</span>
                  <span>Action</span>
                </div>

                {students.map((student) => {
                  const studentId = getStudentId(student);

                  return (
                    <div className={styles.tableRow} key={studentId}>
                      <span>
                        <strong>
                          {student.firstName} {student.lastName}
                        </strong>
                      </span>
                      <span>{student.email}</span>
                      <span>{student.cne || student.codeEtudiant || "-"}</span>
                      <span>
                        <button
                          type="button"
                          className={styles.deleteBtn}
                          onClick={() => deleteStudent(studentId)}
                          disabled={working === `delete-student-${studentId}`}
                        >
                          <Trash2 size={16} />
                          Supprimer
                        </button>
                      </span>
                    </div>
                  );
                })}
              </div>
            )}
          </section>
        </main>
      </div>
    </div>
  );
}
