import { useCallback, useEffect, useState } from "react";
import { ArrowLeft } from "lucide-react";
import { useNavigate, useSearchParams } from "react-router-dom";
import teacherQuizApi from "../../api/teacherQuizApi";
import styles from "./CreateQuiz.module.css";

const AI_QUIZ_STORAGE_KEY = "teacher-ai-quizzes";

const rememberAiQuizChoice = (quizId, creationType) => {
  if (!quizId) return;

  const storedIds = JSON.parse(localStorage.getItem(AI_QUIZ_STORAGE_KEY) || "[]");
  const ids = new Set(storedIds.map(String));

  if (creationType === "AI") {
    ids.add(String(quizId));
  } else {
    ids.delete(String(quizId));
  }

  localStorage.setItem(AI_QUIZ_STORAGE_KEY, JSON.stringify([...ids]));
};

const formatDateForInput = (value) => {
  if (!value) return "";
  return value.length >= 16 ? value.slice(0, 16) : value;
};

const formatDateTimeForBackend = (value) => {
  if (!value) return null;
  return value.length === 16 ? `${value}:00` : value;
};

const toArray = (data) => {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.data)) return data.data;
  return [];
};

const firstValue = (...values) =>
  values.find((value) => value !== undefined && value !== null && String(value).trim() !== "");

const getSubjectId = (subject) =>
  firstValue(subject?.id, subject?.matiereId, subject?.subjectId, subject?.matiere?.id);

const getSubjectLabel = (subject) =>
  firstValue(subject?.nom, subject?.name, subject?.matiereNom, subject?.matiereName) ||
  "Matiere sans nom";

const getSubjectDetails = (subject) =>
  [
    firstValue(subject?.className, subject?.classeName, subject?.classe?.name, subject?.classe?.nom),
    subject?.classFiliere || subject?.classeFiliere || subject?.classe?.filiere,
    subject?.classNiveau || subject?.classeNiveau || subject?.classe?.niveau,
  ]
    .filter(Boolean)
    .join(" - ");

const getSubjectClassName = (subject) =>
  firstValue(subject?.className, subject?.classeName, subject?.classe?.name, subject?.classe?.nom);

const getSubjectClassFiliere = (subject) =>
  subject?.classFiliere || subject?.classeFiliere || subject?.classe?.filiere || "";

const getSubjectClassNiveau = (subject) =>
  subject?.classNiveau || subject?.classeNiveau || subject?.classe?.niveau || "";

const getSubjectClassLabel = (subject) =>
  [
    getSubjectClassName(subject),
    getSubjectClassFiliere(subject),
    getSubjectClassNiveau(subject),
  ]
    .filter(Boolean)
    .join(" - ");

const CreateQuiz = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const editId = searchParams.get("edit");
  const isEditMode = Boolean(editId);

  const [form, setForm] = useState({
    titre: "",
    theme: "",
    description: "",
    difficulty: "MOYEN",
    timeLimit: 30,
    publishNow: "false",
    availableFrom: "",
    availableUntil: "",
    creationType: "MANUAL",
    matiereId: "",
  });

  const [subjects, setSubjects] = useState([]);
  const [loading, setLoading] = useState(false);
  const [loadingQuiz, setLoadingQuiz] = useState(false);
  const [loadingSubjects, setLoadingSubjects] = useState(false);
  const [error, setError] = useState("");

  const selectedSubject = subjects.find(
    (subject) => String(getSubjectId(subject)) === String(form.matiereId)
  );
  const selectedSubjectLabel = selectedSubject
    ? getSubjectLabel(selectedSubject)
    : form.theme.trim();

  const loadSubjects = useCallback(async () => {
    try {
      setLoadingSubjects(true);

      const data = await teacherQuizApi.getSubjects();
      setSubjects(toArray(data));
    } catch (err) {
      setSubjects([]);
      setError(
        err?.response?.data?.message ||
          err?.response?.data?.error ||
          err?.message ||
          "Impossible de charger les matieres."
      );
    } finally {
      setLoadingSubjects(false);
    }
  }, []);

  const loadQuiz = useCallback(async () => {
    try {
      setLoadingQuiz(true);
      setError("");

      const data = await teacherQuizApi.getQuizById(editId);

      setForm({
        titre: data?.titre || "",
        theme: data?.theme || "",
        description: data?.description || "",
        difficulty: data?.difficulty || "MOYEN",
        timeLimit: data?.timeLimit || 30,
        publishNow: data?.status === "PUBLISHED" ? "true" : "false",
        availableFrom: formatDateForInput(data?.availableFrom),
        availableUntil: formatDateForInput(data?.availableUntil),
        creationType: data?.creationType || "MANUAL",
        matiereId: firstValue(data?.matiereId, data?.subjectId, data?.matiere?.id, data?.subject?.id) || "",
      });
    } catch (err) {
      setError(
        err?.response?.data?.message ||
          err?.response?.data?.error ||
          err?.message ||
          "Impossible de charger le quiz."
      );
    } finally {
      setLoadingQuiz(false);
    }
  }, [editId]);

  useEffect(() => {
    void loadSubjects();
  }, [loadSubjects]);

  useEffect(() => {
    if (!isEditMode) return undefined;

    const timer = window.setTimeout(() => {
      void loadQuiz();
    }, 0);

    return () => window.clearTimeout(timer);
  }, [isEditMode, loadQuiz]);

  const handleChange = (e) => {
    const { name, value } = e.target;

    if (name === "matiereId") {
      const subject = subjects.find((item) => String(getSubjectId(item)) === String(value));

      setForm((prev) => ({
        ...prev,
        matiereId: value,
        theme: subject ? getSubjectLabel(subject) : "",
      }));

      return;
    }

    setForm((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const validateForm = () => {
    if (!form.titre.trim()) return "Le titre du quiz est obligatoire.";
    if (!form.matiereId) return "Veuillez choisir une matiere.";
    if (form.creationType === "AI" && !form.description.trim()) {
      return "La description est obligatoire pour generer un quiz avec IA.";
    }
    if (!form.timeLimit || Number(form.timeLimit) <= 0) {
      return "La durée doit être supérieure à 0 minute.";
    }

    return "";
  };

  const buildPayload = () => ({
    titre: form.titre.trim(),
    theme: selectedSubjectLabel,
    description: form.description.trim(),
    difficulty: form.difficulty,
    timeLimit: Number(form.timeLimit),
    availableFrom: formatDateTimeForBackend(form.availableFrom),
    availableUntil: formatDateTimeForBackend(form.availableUntil),
    status: form.publishNow === "true" ? "PUBLISHED" : "DRAFT",
    creationType: form.creationType,
    matiereId: Number(form.matiereId),
  });

  const buildAiState = () => ({
    quizId: isEditMode ? editId : undefined,
    titre: form.titre.trim(),
    theme: selectedSubjectLabel,
    matiere: selectedSubjectLabel,
    description: form.description.trim(),
    difficulty: form.difficulty,
    timeLimit: Number(form.timeLimit),
    publishNow: form.publishNow,
    availableFrom: formatDateTimeForBackend(form.availableFrom),
    availableUntil: formatDateTimeForBackend(form.availableUntil),
    creationType: "AI",
    matiereId: form.matiereId ? Number(form.matiereId) : null,
    matiereNom: selectedSubjectLabel,
    classeId: selectedSubject?.classId || selectedSubject?.classeId || selectedSubject?.classe?.id || null,
    classe: getSubjectClassLabel(selectedSubject),
    className: getSubjectClassName(selectedSubject),
    classFiliere: getSubjectClassFiliere(selectedSubject),
    classNiveau: getSubjectClassNiveau(selectedSubject),
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    const validationError = validateForm();

    if (validationError) {
      setError(validationError);
      return;
    }

    if (!isEditMode && form.creationType === "AI") {
      navigate("/teacher/ai-generator", { state: buildAiState() });

      return;
    }

    try {
      setLoading(true);

      const payload = buildPayload();

      if (isEditMode) {
        await teacherQuizApi.updateQuiz(editId, payload);
        rememberAiQuizChoice(editId, form.creationType);
        navigate("/teacher/quizzes");
      } else {
        const createdQuiz = await teacherQuizApi.createQuiz(payload);
        const quizId = createdQuiz?.id;

        navigate(quizId ? `/teacher/quizzes/${quizId}/questions` : "/teacher/quizzes");
      }
    } catch (err) {
      setError(
        err?.response?.data?.message ||
          err?.response?.data?.error ||
          err?.message ||
          (isEditMode
            ? "Erreur lors de la modification du quiz."
            : "Erreur lors de la création du quiz.")
      );
    } finally {
      setLoading(false);
    }
  };

  if (loadingQuiz) {
    return <div className={styles.page}>Chargement du quiz...</div>;
  }

  return (
    <div className={styles.page}>
      <div className={`${styles.header} ${isEditMode ? styles.headerWithBack : ""}`}>
        {isEditMode && (
          <button
            type="button"
            className={styles.backBtn}
            onClick={() => navigate("/teacher/quizzes")}
            aria-label="Retour"
          >
            <ArrowLeft size={18} />
          </button>
        )}

        <span className={styles.badge}>{isEditMode ? "Modification" : "Création"}</span>
        <h1>{isEditMode ? "Modifier le quiz" : "Créer un nouveau quiz"}</h1>
        <p>
          {isEditMode
            ? "Ajustez les informations principales avant de republier le quiz."
            : "Préparez le quiz, choisissez la matière et définissez les paramètres."}
        </p>
      </div>

      <form className={styles.card} onSubmit={handleSubmit}>
        {error && <div className={styles.errorBox}>{error}</div>}

        <div className={styles.sectionTitle}>
          <span>01</span>

          <div>
            <h2>Méthode de création</h2>
            <p>Choisissez une création manuelle ou assistée par IA.</p>
          </div>
        </div>

        <div className={styles.modeGrid}>
          <label
            className={`${styles.modeCard} ${
              form.creationType === "MANUAL" ? styles.modeActive : ""
            }`}
          >
            <input
              type="radio"
              name="creationType"
              value="MANUAL"
              checked={form.creationType === "MANUAL"}
              onChange={handleChange}
            />

            <strong>Création manuelle</strong>
            <span>Créer le quiz puis ajouter les questions vous-même.</span>
          </label>

          <label
            className={`${styles.modeCard} ${
              form.creationType === "AI" ? styles.modeActive : ""
            }`}
          >
            <input
              type="radio"
              name="creationType"
              value="AI"
              checked={form.creationType === "AI"}
              onChange={handleChange}
            />

            <strong>Création avec IA</strong>
            <span>Générer les questions automatiquement puis les vérifier.</span>
          </label>
        </div>

        <div className={styles.sectionTitle}>
          <span>02</span>

          <div>
            <h2>Informations principales</h2>
            <p>Definissez la matiere, le titre, la description et la difficulte.</p>
          </div>
        </div>

        <div className={styles.field}>
          <label htmlFor="matiereId">Matiere</label>

          <select
            id="matiereId"
            name="matiereId"
            value={form.matiereId}
            onChange={handleChange}
            disabled={loadingSubjects}
            required
          >
            <option value="">
              {loadingSubjects ? "Chargement des matieres..." : "Choisir une matiere"}
            </option>

            {subjects.map((subject, index) => {
              const subjectId = getSubjectId(subject) || `subject-${index}`;
              const details = getSubjectDetails(subject);

              return (
                <option key={subjectId} value={subjectId}>
                  {[getSubjectLabel(subject), details].filter(Boolean).join(" - ")}
                </option>
              );
            })}
          </select>

          {!loadingSubjects && subjects.length === 0 && (
            <small className={styles.hint}>
              Aucune matiere n'est encore affectee a votre compte par l'admin.
            </small>
          )}
        </div>

        <div className={styles.field}>
          <label htmlFor="titre">Titre du quiz</label>

          <input
            id="titre"
            name="titre"
            type="text"
            value={form.titre}
            onChange={handleChange}
            placeholder="Ex : Sécurité Web"
            required
          />
        </div>

        <div className={styles.gridTwo}>
          <div className={styles.field}>
            <label htmlFor="difficulty">Difficulté du quiz</label>

            <select
              id="difficulty"
              name="difficulty"
              value={form.difficulty}
              onChange={handleChange}
              required
            >
              <option value="FACILE">Facile</option>
              <option value="MOYEN">Moyen</option>
              <option value="DIFFICILE">Difficile</option>
            </select>
          </div>

          <div className={styles.field}>
            <label htmlFor="timeLimit">Durée en minutes</label>

            <input
              id="timeLimit"
              name="timeLimit"
              type="number"
              min="1"
              value={form.timeLimit}
              onChange={handleChange}
              required
            />
          </div>
        </div>

        <div className={styles.field}>
          <label htmlFor="description">Description</label>

          <textarea
            id="description"
            name="description"
            value={form.description}
            onChange={handleChange}
            placeholder="Objectif du quiz"
            rows="5"
            required={form.creationType === "AI"}
          />
        </div>

        <div className={styles.sectionTitle}>
          <span>03</span>

          <div>
            <h2>Paramètres du quiz</h2>
            <p>Configurez la publication et les dates de disponibilité.</p>
          </div>
        </div>

        <div className={styles.gridTwo}>
          <div className={styles.field}>
            <label htmlFor="publishNow">Publier maintenant</label>

            <select
              id="publishNow"
              name="publishNow"
              value={form.publishNow}
              onChange={handleChange}
            >
              <option value="false">Non, enregistrer en brouillon</option>
              <option value="true">Oui, publier immédiatement</option>
            </select>
          </div>

          <div className={styles.field}>
            <label htmlFor="availableFrom">Date début</label>

            <input
              id="availableFrom"
              name="availableFrom"
              type="datetime-local"
              value={form.availableFrom}
              onChange={handleChange}
            />
          </div>
        </div>

        <div className={styles.field}>
          <label htmlFor="availableUntil">Date fin</label>

          <input
            id="availableUntil"
            name="availableUntil"
            type="datetime-local"
            value={form.availableUntil}
            onChange={handleChange}
          />
        </div>

        <div className={styles.actions}>
          <button
            type="button"
            className={styles.secondaryBtn}
            onClick={() => navigate("/teacher/quizzes")}
            disabled={loading}
          >
            Annuler
          </button>

          <button type="submit" className={styles.primaryBtn} disabled={loading}>
            {loading
              ? isEditMode
                ? "Modification..."
                : "Traitement..."
              : isEditMode
              ? "Enregistrer les modifications"
              : form.creationType === "AI"
              ? "Générer avec IA"
              : "Créer le quiz"}
          </button>
        </div>
      </form>
    </div>
  );
};

export default CreateQuiz;
