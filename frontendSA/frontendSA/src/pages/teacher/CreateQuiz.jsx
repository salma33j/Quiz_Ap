import { useEffect, useState } from "react";
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
  });

  const [loading, setLoading] = useState(false);
  const [loadingQuiz, setLoadingQuiz] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (isEditMode) loadQuiz();
  }, [editId]);

  const formatDateForInput = (value) => {
    if (!value) return "";
    return value.length >= 16 ? value.slice(0, 16) : value;
  };

  const formatDateTimeForBackend = (value) => {
    if (!value) return null;
    return value.length === 16 ? `${value}:00` : value;
  };

  const loadQuiz = async () => {
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
  };

  const handleChange = (e) => {
    const { name, value } = e.target;

    setForm((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const validateForm = () => {
    if (!form.titre.trim()) return "Le titre du quiz est obligatoire.";
    if (!form.theme.trim()) return "Le thème du quiz est obligatoire.";

    if (!form.timeLimit || Number(form.timeLimit) <= 0) {
      return "La durée doit être supérieure à 0 minute.";
    }

    return "";
  };

  const buildPayload = () => ({
    titre: form.titre.trim(),
    theme: form.theme.trim(),
    description: form.description.trim(),
    difficulty: form.difficulty,
    timeLimit: Number(form.timeLimit),
    availableFrom: formatDateTimeForBackend(form.availableFrom),
    availableUntil: formatDateTimeForBackend(form.availableUntil),
    status: form.publishNow === "true" ? "PUBLISHED" : "DRAFT",
    creationType: form.creationType,
  });

  const buildAiState = () => ({
    quizId: isEditMode ? editId : undefined,
    titre: form.titre.trim(),
    theme: form.theme.trim(),
    description: form.description.trim(),
    difficulty: form.difficulty,
    timeLimit: Number(form.timeLimit),
    publishNow: form.publishNow,
    availableFrom: formatDateTimeForBackend(form.availableFrom),
    availableUntil: formatDateTimeForBackend(form.availableUntil),
    creationType: "AI",
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

        <h1>{isEditMode ? "Modifier le quiz" : "Créer un nouveau quiz"}</h1>
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
            <p>Définissez le titre, le thème et la difficulté.</p>
          </div>
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

        <div className={styles.field}>
          <label htmlFor="theme">Thème</label>

          <input
            id="theme"
            name="theme"
            type="text"
            value={form.theme}
            onChange={handleChange}
            placeholder="Réseaux, Java, sécurité..."
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
