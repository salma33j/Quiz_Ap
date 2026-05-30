import { useEffect, useState } from "react";
import { ArrowLeft } from "lucide-react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import teacherQuizApi from "../../api/teacherQuizApi";
import styles from "./AddQuestion.module.css";

const AddQuestion = () => {
  const { id: quizId } = useParams();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const editId = searchParams.get("edit");
  const isEditMode = Boolean(editId);

  const [form, setForm] = useState({
    questionText: "",
    type: "QCM",
    points: 1,
    optionA: "",
    optionB: "",
    optionC: "",
    optionD: "",
    correctAnswer: "A",
  });

  const [loading, setLoading] = useState(false);
  const [loadingQuestion, setLoadingQuestion] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (isEditMode) {
      loadQuestion();
    }
  }, [editId]);

  const normalizeType = (type) => {
    if (type === "MCQ") return "QCM";
    return type || "QCM";
  };

  const loadQuestion = async () => {
    try {
      setLoadingQuestion(true);
      setError("");

      const questions = await teacherQuizApi.getQuestions(quizId);
      const question = Array.isArray(questions)
        ? questions.find((q) => String(q.id || q._id) === String(editId))
        : null;

      if (!question) {
        setError("Question introuvable.");
        return;
      }

      const type = normalizeType(question.type);

      setForm({
        questionText: question.enonce || question.questionText || "",
        type,
        points: question.points || 1,
        optionA: type === "TRUE_FALSE" ? "Vrai" : question.choixA || "",
        optionB: type === "TRUE_FALSE" ? "Faux" : question.choixB || "",
        optionC: question.choixC || "",
        optionD: question.choixD || "",
        correctAnswer: question.reponseCorrecte || question.correctAnswer || "A",
      });
    } catch (err) {
      setError(
        err?.response?.data?.message ||
          err?.response?.data?.error ||
          err?.message ||
          "Impossible de charger la question."
      );
    } finally {
      setLoadingQuestion(false);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;

    setForm((prev) => {
      if (name === "type") {
        if (value === "TRUE_FALSE") {
          return {
            ...prev,
            type: value,
            optionA: "Vrai",
            optionB: "Faux",
            optionC: "",
            optionD: "",
            correctAnswer: "A",
          };
        }

        if (value === "TEXT") {
          return {
            ...prev,
            type: value,
            optionA: "",
            optionB: "",
            optionC: "",
            optionD: "",
            correctAnswer: "",
          };
        }

        return {
          ...prev,
          type: value,
          correctAnswer: "A",
        };
      }

      return {
        ...prev,
        [name]: value,
      };
    });
  };

  const buildPayload = () => {
    if (form.type === "TRUE_FALSE") {
      return {
        enonce: form.questionText.trim(),
        type: "TRUE_FALSE",
        points: Number(form.points),
        choixA: "Vrai",
        choixB: "Faux",
        choixC: null,
        choixD: null,
        reponseCorrecte: form.correctAnswer,
      };
    }

    if (form.type === "TEXT") {
      return {
        enonce: form.questionText.trim(),
        type: "TEXT",
        points: Number(form.points),
        choixA: null,
        choixB: null,
        choixC: null,
        choixD: null,
        reponseCorrecte: form.correctAnswer.trim(),
      };
    }

    return {
      enonce: form.questionText.trim(),
      type: "MCQ",
      points: Number(form.points),
      choixA: form.optionA.trim(),
      choixB: form.optionB.trim(),
      choixC: form.optionC.trim(),
      choixD: form.optionD.trim(),
      reponseCorrecte: form.correctAnswer,
    };
  };

  const validateForm = () => {
    if (!form.questionText.trim()) {
      return "L’énoncé de la question est obligatoire.";
    }

    if (!form.points || Number(form.points) <= 0) {
      return "Les points doivent être supérieurs à 0.";
    }

    if (form.type === "QCM") {
      if (
        !form.optionA.trim() ||
        !form.optionB.trim() ||
        !form.optionC.trim() ||
        !form.optionD.trim()
      ) {
        return "Veuillez remplir les quatre choix A, B, C et D.";
      }
    }

    if (form.type === "TEXT" && !form.correctAnswer.trim()) {
      return "Veuillez saisir la réponse correcte attendue.";
    }

    if (!form.correctAnswer) {
      return "Veuillez choisir la bonne réponse.";
    }

    return "";
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    const validationError = validateForm();

    if (validationError) {
      setError(validationError);
      return;
    }

    try {
      setLoading(true);

      const payload = buildPayload();

      if (isEditMode) {
        await teacherQuizApi.updateQuestion(quizId, editId, payload);
      } else {
        await teacherQuizApi.addQuestion(quizId, payload);
      }

      navigate(`/teacher/quizzes/${quizId}/questions`);
    } catch (err) {
      setError(
        err?.response?.data?.message ||
          err?.response?.data?.error ||
          err?.message ||
          isEditMode
          ? "Erreur lors de la modification de la question."
          : "Erreur lors de l’ajout de la question."
      );
    } finally {
      setLoading(false);
    }
  };

  if (loadingQuestion) {
    return <div className={styles.page}>Chargement de la question...</div>;
  }

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <div>
          <span className={styles.badge}>Quiz #{quizId}</span>

          <h1>{isEditMode ? "Modifier la question" : "Ajouter une question"}</h1>

          <p>
            {isEditMode
              ? "Modifiez le contenu, le type, les points et la bonne réponse."
              : "Créez une question claire avec ses choix et sa bonne réponse."}
          </p>
        </div>

        <button
          type="button"
          className={styles.backBtn}
          onClick={() => navigate(`/teacher/quizzes/${quizId}/questions`)}
          aria-label="Retour"
        >
          <ArrowLeft size={18} />
        </button>
      </div>

      <form className={styles.card} onSubmit={handleSubmit}>
        {error && <div className={styles.errorBox}>{error}</div>}

        <div className={styles.section}>
          <div className={styles.sectionHeader}>
            <span>01</span>

            <div>
              <h2>Informations de la question</h2>
              <p>Définissez l’énoncé, le type et le nombre de points.</p>
            </div>
          </div>

          <div className={styles.field}>
            <label htmlFor="questionText">Énoncé</label>

            <textarea
              id="questionText"
              name="questionText"
              value={form.questionText}
              onChange={handleChange}
              placeholder="Exemple : Qu’est-ce que la cryptographie ?"
              rows="5"
              required
            />
          </div>

          <div className={styles.gridTwo}>
            <div className={styles.field}>
              <label htmlFor="type">Type</label>

              <select
                id="type"
                name="type"
                value={form.type}
                onChange={handleChange}
              >
                <option value="QCM">QCM</option>
                <option value="TRUE_FALSE">Vrai / Faux</option>
                <option value="TEXT">Réponse courte</option>
              </select>
            </div>

            <div className={styles.field}>
              <label htmlFor="points">Points</label>

              <input
                id="points"
                name="points"
                type="number"
                min="1"
                value={form.points}
                onChange={handleChange}
                required
              />
            </div>
          </div>
        </div>

        {form.type === "QCM" && (
          <div className={styles.section}>
            <div className={styles.sectionHeader}>
              <span>02</span>

              <div>
                <h2>Choix de réponse</h2>
                <p>Chaque choix est affiché dans une ligne séparée.</p>
              </div>
            </div>

            <div className={styles.choicesList}>
              {[
                ["A", "optionA"],
                ["B", "optionB"],
                ["C", "optionC"],
                ["D", "optionD"],
              ].map(([label, name]) => (
                <div className={styles.choiceRow} key={label}>
                  <div className={styles.choiceLetter}>{label}</div>

                  <input
                    name={name}
                    value={form[name]}
                    onChange={handleChange}
                    placeholder={`Choix ${label}`}
                    required
                  />

                  <label className={styles.correctChoice}>
                    <input
                      type="radio"
                      name="correctAnswer"
                      value={label}
                      checked={form.correctAnswer === label}
                      onChange={handleChange}
                    />
                    Bonne réponse
                  </label>
                </div>
              ))}
            </div>
          </div>
        )}

        {form.type === "TRUE_FALSE" && (
          <div className={styles.section}>
            <div className={styles.sectionHeader}>
              <span>02</span>

              <div>
                <h2>Bonne réponse</h2>
                <p>Sélectionnez la réponse correcte attendue.</p>
              </div>
            </div>

            <div className={styles.trueFalseChoices}>
              <button
                type="button"
                className={`${styles.trueFalseBtn} ${
                  form.correctAnswer === "A" ? styles.trueFalseActive : ""
                }`}
                onClick={() =>
                  setForm((prev) => ({
                    ...prev,
                    correctAnswer: "A",
                    optionA: "Vrai",
                    optionB: "Faux",
                  }))
                }
              >
                ✓ Vrai
              </button>

              <button
                type="button"
                className={`${styles.trueFalseBtn} ${
                  form.correctAnswer === "B" ? styles.trueFalseActive : ""
                }`}
                onClick={() =>
                  setForm((prev) => ({
                    ...prev,
                    correctAnswer: "B",
                    optionA: "Vrai",
                    optionB: "Faux",
                  }))
                }
              >
                ✕ Faux
              </button>
            </div>
          </div>
        )}

        {form.type === "TEXT" && (
          <div className={styles.section}>
            <div className={styles.sectionHeader}>
              <span>02</span>

              <div>
                <h2>Réponse attendue</h2>
                <p>Indiquez la réponse correcte ou une réponse modèle.</p>
              </div>
            </div>

            <div className={styles.field}>
              <label htmlFor="correctAnswer">Réponse correcte</label>

              <textarea
                id="correctAnswer"
                name="correctAnswer"
                value={form.correctAnswer}
                onChange={handleChange}
                placeholder="Exemple : La cryptographie protège les données par chiffrement."
                rows="4"
                required
              />
            </div>
          </div>
        )}

        <div className={styles.actions}>
          <button
            type="button"
            className={styles.secondaryBtn}
            onClick={() => navigate(`/teacher/quizzes/${quizId}/questions`)}
            disabled={loading}
          >
            Annuler
          </button>

          <button type="submit" className={styles.primaryBtn} disabled={loading}>
            {loading
              ? isEditMode
                ? "Modification..."
                : "Ajout en cours..."
              : isEditMode
              ? "Modifier la question"
              : "Ajouter la question"}
          </button>
        </div>
      </form>
    </div>
  );
};

export default AddQuestion;
