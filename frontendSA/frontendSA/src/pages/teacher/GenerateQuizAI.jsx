import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import {
  ArrowLeft,
  Brain,
  Plus,
  Save,
  Trash2,
  Wand2,
  Lock,
} from "lucide-react";

import teacherQuizApi from "../../api/teacherQuizApi";
import styles from "./GenerateQuizAI.module.css";

const emptyQuestion = (type = "QCM") => {
  if (type === "TEXT") {
    return {
      type: "TEXT",
      questionText: "",
      points: 1,
      correctAnswer: "",
      options: [],
    };
  }

  if (type === "TRUE_FALSE") {
    return {
      type: "TRUE_FALSE",
      questionText: "",
      points: 1,
      correctAnswer: "A",
      options: [
        { label: "A", text: "Vrai", correct: true },
        { label: "B", text: "Faux", correct: false },
      ],
    };
  }

  return {
    type: "QCM",
    questionText: "",
    points: 1,
    correctAnswer: "A",
    options: [
      { label: "A", text: "", correct: true },
      { label: "B", text: "", correct: false },
      { label: "C", text: "", correct: false },
      { label: "D", text: "", correct: false },
    ],
  };
};

export default function GenerateQuizAI() {
  const navigate = useNavigate();
  const { state } = useLocation();

  const quizInfo = state || {};
  const existingQuizId = quizInfo.quizId || quizInfo.id;
  const backPath =
    quizInfo.from ||
    (existingQuizId ? "/teacher/quizzes" : "/teacher/quizzes/create");
  const subjectName = quizInfo.matiereNom || quizInfo.matiere || quizInfo.theme || "";
  const classLabel =
    quizInfo.classe ||
    [quizInfo.className, quizInfo.classFiliere, quizInfo.classNiveau]
      .filter(Boolean)
      .join(" - ");

  const [questions, setQuestions] = useState([]);
  const [deletedQuestionIds, setDeletedQuestionIds] = useState([]);
  const [numberOfQuestions, setNumberOfQuestions] = useState(
    Number(quizInfo.numberOfQuestions) || 15
  );
  const [questionType, setQuestionType] = useState("ALL");
  const [loadingGenerate, setLoadingGenerate] = useState(false);
  const [loadingExistingQuestions, setLoadingExistingQuestions] = useState(false);
  const [loadingSave, setLoadingSave] = useState(false);
  const [error, setError] = useState("");

  const isBlocked =
    !quizInfo?.titre ||
    !subjectName ||
    !quizInfo?.description ||
    !quizInfo?.matiereId;

  useEffect(() => {
    if (!existingQuizId) return;

    const loadExistingQuestions = async () => {
      try {
        setLoadingExistingQuestions(true);
        setError("");

        const data = await teacherQuizApi.getQuestions(existingQuizId);
        const existingQuestions = Array.isArray(data) ? data : [];

        setQuestions(existingQuestions.map(formatExistingQuestion));
      } catch (err) {
        setError(
          err?.response?.data?.message ||
            err?.response?.data?.error ||
            err?.message ||
            "Impossible de charger les questions existantes."
        );
      } finally {
        setLoadingExistingQuestions(false);
      }
    };

    loadExistingQuestions();
  }, [existingQuizId]);

  if (isBlocked) {
    return (
      <div className={styles.blockedPage}>
        <div className={styles.blockedCard}>
          <div className={styles.blockedIcon}>
            <Lock size={34} />
          </div>

          <h1>Quiz non préparé</h1>

          <p>
            Pour utiliser la generation IA, vous devez d'abord creer les
            informations principales du quiz : matiere, titre, description,
            classe, difficulte et duree.
          </p>

          <button onClick={() => navigate("/teacher/quizzes/create")}>
            Créer un quiz
          </button>
        </div>
      </div>
    );
  }

  const normalizeType = (type) => {
    const value = String(type || "QCM")
      .trim()
      .toUpperCase()
      .replace(/[\s/-]+/g, "_");

    if (value === "MCQ" || value === "QCM") return "QCM";
    if (
      value === "TRUE_FALSE" ||
      value === "TRUEFALSE" ||
      value === "VRAI_FAUX" ||
      value === "BOOLEAN" ||
      value === "BOOL"
    ) {
      return "TRUE_FALSE";
    }
    if (value === "TEXT" || value === "TEXTE" || value === "OPEN") return "TEXT";

    return "QCM";
  };

  const normalizeRequestedType = (type) => {
    if (type === "ALL") return "ALL";
    return normalizeType(type);
  };

  const looksLikeTrueFalseQuestion = (q) => {
    const values = Array.isArray(q.options)
      ? q.options.map((opt) => opt.text || opt.value || opt)
      : [q.choixA, q.choixB, q.choixC, q.choixD];

    const cleaned = values
      .filter(Boolean)
      .map((value) => String(value).trim().toLowerCase());

    return (
      cleaned.length === 2 &&
      cleaned.includes("vrai") &&
      cleaned.includes("faux")
    );
  };

  const normalizeAnswerText = (value) =>
    String(value || "")
      .trim()
      .toLowerCase()
      .replace(/\s+/g, " ");

  const resolveCorrectLabel = (answer, options, fallback = "A") => {
    const normalizedAnswer = normalizeAnswerText(answer);

    if (/^[a-d]$/.test(normalizedAnswer)) {
      return normalizedAnswer.toUpperCase();
    }

    const matched = options.find(
      (option) => normalizeAnswerText(option.text) === normalizedAnswer
    );

    return matched?.label || fallback;
  };

  const formatExistingQuestion = (q) => {
    const type =
      normalizeType(q.type) === "QCM" && looksLikeTrueFalseQuestion(q)
        ? "TRUE_FALSE"
        : normalizeType(q.type);
    const correctAnswer = q.reponseCorrecte || q.correctAnswer || "A";

    if (type === "TEXT") {
      return {
        id: q.id || q._id,
        type: "TEXT",
        questionText: q.enonce || q.questionText || q.text || "",
        points: q.points || 1,
        correctAnswer,
        options: [],
      };
    }

    if (type === "TRUE_FALSE") {
      return {
        id: q.id || q._id,
        type: "TRUE_FALSE",
        questionText: q.enonce || q.questionText || q.text || "",
        points: q.points || 1,
        correctAnswer,
        options: [
          { label: "A", text: q.choixA || "Vrai", correct: correctAnswer === "A" },
          { label: "B", text: q.choixB || "Faux", correct: correctAnswer === "B" },
        ],
      };
    }

    return {
      id: q.id || q._id,
      type: "QCM",
      questionText: q.enonce || q.questionText || q.text || "",
      points: q.points || 1,
      correctAnswer,
      options: [
        { label: "A", text: q.choixA || "", correct: correctAnswer === "A" },
        { label: "B", text: q.choixB || "", correct: correctAnswer === "B" },
        { label: "C", text: q.choixC || "", correct: correctAnswer === "C" },
        { label: "D", text: q.choixD || "", correct: correctAnswer === "D" },
      ],
    };
  };

  const formatGeneratedQuestion = (q) => {
    const type =
      normalizeType(q.type) === "QCM" && looksLikeTrueFalseQuestion(q)
        ? "TRUE_FALSE"
        : normalizeType(q.type);

    if (type === "TEXT") {
      return {
        type: "TEXT",
        questionText: q.questionText || q.enonce || q.text || "",
        points: q.points || 1,
        correctAnswer: q.correctAnswer || q.reponseCorrecte || "",
        options: [],
      };
    }

    if (type === "TRUE_FALSE") {
      const answer = q.correctAnswer || q.reponseCorrecte || "A";
      const isFalse = /^(faux|false|b|non|no)$/i.test(String(answer).trim());
      const correctAnswer = isFalse ? "B" : "A";

      return {
        type: "TRUE_FALSE",
        questionText: q.questionText || q.enonce || q.text || "",
        points: q.points || 1,
        correctAnswer,
        options: [
          { label: "A", text: "Vrai", correct: correctAnswer === "A" },
          { label: "B", text: "Faux", correct: correctAnswer === "B" },
        ],
      };
    }

    const options = Array.isArray(q.options)
      ? q.options.map((opt, index) => {
          const label = opt.label || ["A", "B", "C", "D"][index];

          return {
            label,
            text: opt.text || opt.value || opt || "",
            correct: label === (q.correctAnswer || q.reponseCorrecte || "A"),
          };
        })
      : [
          { label: "A", text: q.choixA || "", correct: (q.correctAnswer || q.reponseCorrecte) === "A" },
          { label: "B", text: q.choixB || "", correct: (q.correctAnswer || q.reponseCorrecte) === "B" },
          { label: "C", text: q.choixC || "", correct: (q.correctAnswer || q.reponseCorrecte) === "C" },
          { label: "D", text: q.choixD || "", correct: (q.correctAnswer || q.reponseCorrecte) === "D" },
        ];

    const correctAnswer = resolveCorrectLabel(
      q.correctAnswer || q.reponseCorrecte,
      options
    );

    return {
      type: "QCM",
      questionText: q.questionText || q.enonce || q.text || "",
      points: q.points || 1,
      correctAnswer,
      options: options.map((option) => ({
        ...option,
        correct: option.label === correctAnswer,
      })),
    };
  };

  const forceGeneratedType = (q, type) => {
    const requestedType = normalizeRequestedType(type);
    const formatted = formatGeneratedQuestion(q);

    if (requestedType === "ALL") return formatted;

    if (requestedType === "TEXT") {
      return {
        ...formatted,
        type: "TEXT",
        correctAnswer:
          q.correctAnswer || q.reponseCorrecte || formatted.correctAnswer || "",
        options: [],
      };
    }

    if (requestedType === "TRUE_FALSE") {
      const rawAnswer = String(
        q.correctAnswer || q.reponseCorrecte || formatted.correctAnswer || "A"
      ).trim();
      const isFalse = /^(faux|false|b|non|no)$/i.test(rawAnswer);

      return {
        ...formatted,
        type: "TRUE_FALSE",
        correctAnswer: isFalse ? "B" : "A",
        options: [
          { label: "A", text: "Vrai", correct: !isFalse },
          { label: "B", text: "Faux", correct: isFalse },
        ],
      };
    }

    return {
      ...formatted,
      type: "QCM",
      correctAnswer: /^[A-D]$/i.test(formatted.correctAnswer)
        ? formatted.correctAnswer.toUpperCase()
        : "A",
    };
  };

  const generateQuestions = async () => {
    setError("");
    const requestedCount = Number(numberOfQuestions);

    if (!Number.isFinite(requestedCount) || requestedCount < 15) {
      setError("Le nombre de questions doit etre superieur ou egal a 15.");
      return;
    }

    try {
      setLoadingGenerate(true);

      const payload = {
        titre: quizInfo.titre,
        matiere: subjectName,
        theme: subjectName,
        description: quizInfo.description,
        classe: classLabel,
        difficulty: quizInfo.difficulty,
        numberOfQuestions: requestedCount,
        type: normalizeRequestedType(questionType),
      };

      const response = await teacherQuizApi.generateQuestionsAI(payload);
      const generated = Array.isArray(response) ? response : response?.questions || [];

      if (existingQuizId) {
        const existingIds = questions.map((q) => q.id).filter(Boolean);
        setDeletedQuestionIds((ids) => [...new Set([...ids, ...existingIds])]);
      }

      setQuestions(generated.map((q) => forceGeneratedType(q, questionType)));
    } catch (err) {
      setError(
        err?.response?.data?.message ||
          err?.response?.data?.error ||
          err?.message ||
          "Erreur lors de la génération IA."
      );
    } finally {
      setLoadingGenerate(false);
    }
  };

  const addQuestion = () => {
    setQuestions((prev) => [...prev, emptyQuestion("QCM")]);
  };

  const deleteQuestion = (index) => {
    setQuestions((prev) => {
      const removed = prev[index];

      if (removed?.id) {
        setDeletedQuestionIds((ids) => [...ids, removed.id]);
      }

      return prev.filter((_, i) => i !== index);
    });
  };

  const updateQuestionText = (index, value) => {
    setQuestions((prev) =>
      prev.map((q, i) => (i === index ? { ...q, questionText: value } : q))
    );
  };

  const updatePoints = (index, value) => {
    setQuestions((prev) =>
      prev.map((q, i) =>
        i === index ? { ...q, points: Number(value) || 1 } : q
      )
    );
  };

  const updateQuestionType = (index, type) => {
    setQuestions((prev) =>
      prev.map((q, i) =>
        i === index
          ? {
              ...emptyQuestion(type),
              questionText: q.questionText,
              points: q.points,
            }
          : q
      )
    );
  };

  const updateOption = (questionIndex, optionIndex, value) => {
    setQuestions((prev) =>
      prev.map((q, i) =>
        i === questionIndex
          ? {
              ...q,
              options: q.options.map((opt, j) =>
                j === optionIndex ? { ...opt, text: value } : opt
              ),
            }
          : q
      )
    );
  };

  const updateCorrectAnswer = (questionIndex, label) => {
    setQuestions((prev) =>
      prev.map((q, i) =>
        i === questionIndex
          ? {
              ...q,
              correctAnswer: label,
              options: q.options.map((opt) => ({
                ...opt,
                correct: opt.label === label,
              })),
            }
          : q
      )
    );
  };

  const updateTextAnswer = (index, value) => {
    setQuestions((prev) =>
      prev.map((q, i) =>
        i === index ? { ...q, correctAnswer: value } : q
      )
    );
  };

  const validateQuestions = () => {
    if (questions.length === 0) {
      return "Veuillez générer ou ajouter au moins une question.";
    }

    for (let i = 0; i < questions.length; i++) {
      const q = questions[i];

      if (!q.questionText.trim()) {
        return `La question ${i + 1} est vide.`;
      }

      if (q.type === "TEXT") {
        if (!q.correctAnswer.trim()) {
          return `La réponse attendue de la question ${i + 1} est vide.`;
        }
        continue;
      }

      for (const opt of q.options) {
        if (!opt.text.trim()) {
          return `Le choix ${opt.label} de la question ${i + 1} est vide.`;
        }
      }
    }

    return "";
  };

  const saveQuiz = async () => {
    setError("");

    const validationError = validateQuestions();

    if (validationError) {
      setError(validationError);
      return;
    }

    try {
      setLoadingSave(true);

      const quizPayload = {
        titre: quizInfo.titre,
        theme: subjectName,
        description: quizInfo.description || "",
        classe: classLabel,
        difficulty: quizInfo.difficulty,
        timeLimit: Number(quizInfo.timeLimit),
        classeId: quizInfo.classeId ? Number(quizInfo.classeId) : null,
        matiereId: Number(quizInfo.matiereId),
        availableFrom: quizInfo.availableFrom || null,
        availableUntil: quizInfo.availableUntil || null,
        status: quizInfo.publishNow === "true" ? "PUBLISHED" : "DRAFT",
        creationType: "AI",
      };

      let quizId = existingQuizId;

      if (quizId) {
        await teacherQuizApi.updateQuiz(quizId, quizPayload);
      } else {
        const createdQuiz = await teacherQuizApi.createQuiz(quizPayload);
        quizId = createdQuiz?.id;
      }

      if (!quizId) {
        throw new Error("Impossible de récupérer l'ID du quiz.");
      }

      for (const questionId of deletedQuestionIds) {
        await teacherQuizApi.deleteQuestion(quizId, questionId);
      }

      for (const q of questions) {
        const payload = {
          enonce: q.questionText.trim(),
          type: q.type === "QCM" ? "MCQ" : q.type,
          points: Number(q.points),
          reponseCorrecte: q.correctAnswer,
        };

        if (q.type === "TEXT") {
          payload.choixA = null;
          payload.choixB = null;
          payload.choixC = null;
          payload.choixD = null;
        } else if (q.type === "TRUE_FALSE") {
          payload.choixA = q.options[0]?.text || "Vrai";
          payload.choixB = q.options[1]?.text || "Faux";
          payload.choixC = null;
          payload.choixD = null;
        } else {
          payload.choixA = q.options[0]?.text || null;
          payload.choixB = q.options[1]?.text || null;
          payload.choixC = q.options[2]?.text || null;
          payload.choixD = q.options[3]?.text || null;
        }

        if (q.id) {
          await teacherQuizApi.updateQuestion(quizId, q.id, payload);
        } else {
          await teacherQuizApi.addQuestion(quizId, payload);
        }
      }

      if (quizInfo.publishNow === "true") {
        await teacherQuizApi.publishQuiz(quizId);
      }

      navigate(`/teacher/quizzes/${quizId}/questions`);
    } catch (err) {
      setError(
        err?.response?.data?.message ||
          err?.response?.data?.error ||
          err?.message ||
          "Erreur lors de l'enregistrement du quiz."
      );
    } finally {
      setLoadingSave(false);
    }
  };

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <button
          type="button"
          className={styles.backBtn}
          onClick={() => navigate(backPath)}
          aria-label="Retour"
        >
          <ArrowLeft size={22} />
        </button>

        <div className={styles.headerContent}>
          <span className={styles.badge}>
            <Brain size={16} />
            Génération IA intelligente
          </span>

          <h1>Préparer le quiz avec IA</h1>

          <p>
            Générez automatiquement des questions, vérifiez le contenu, modifiez
            les réponses puis validez le quiz final.
          </p>
        </div>
      </div>

      {error && <div className={styles.errorBox}>{error}</div>}

      <section className={styles.summaryCard}>
        <div>
          <span>Titre</span>
          <strong>{quizInfo.titre}</strong>
        </div>

        <div>
          <span>Matiere</span>
          <strong>{subjectName}</strong>
        </div>

        <div>
          <span>Classe</span>
          <strong>{classLabel || "Non definie"}</strong>
        </div>

        <div>
          <span>Difficulté</span>
          <strong>{quizInfo.difficulty || "MOYEN"}</strong>
        </div>
      </section>

      <section className={styles.generatorCard}>
        <div>
          <h2>Génération automatique</h2>
          <p>Choisissez le nombre de questions à générer.</p>
        </div>

        <div className={styles.generatorActions}>
          <select
            value={questionType}
            onChange={(e) => setQuestionType(e.target.value)}
            aria-label="Type de questions"
          >
            <option value="ALL">Tous</option>
            <option value="QCM">QCM</option>
            <option value="TRUE_FALSE">Vrai / Faux</option>
            <option value="TEXT">Texte</option>
          </select>

          <input
            type="number"
            min="15"
            max="30"
            value={numberOfQuestions}
            onChange={(e) => setNumberOfQuestions(e.target.value)}
            required
          />

          <button
            type="button"
            onClick={generateQuestions}
            disabled={loadingGenerate}
          >
            <Wand2 size={18} />
            {loadingGenerate ? "Génération..." : "Générer avec IA"}
          </button>
        </div>
      </section>

      <section className={styles.questionsSection}>
        <div className={styles.sectionHeader}>
          <div>
            <h2>Questions générées</h2>
            <p>Vous pouvez modifier, supprimer ou ajouter une question.</p>
          </div>

          <button type="button" className={styles.addBtn} onClick={addQuestion}>
            <Plus size={18} />
            Ajouter une question
          </button>
        </div>

        {questions.length === 0 ? (
          <div className={styles.emptyBox}>
            {loadingExistingQuestions
              ? "Chargement des questions existantes..."
              : "Aucune question générée pour le moment."}
          </div>
        ) : (
          <div className={styles.questionsList}>
            {questions.map((question, questionIndex) => (
              <div className={styles.questionCard} key={questionIndex}>
                <div className={styles.questionTop}>
                  <span>Question {questionIndex + 1}</span>

                  <div className={styles.questionTypeRow}>
                    <label>Type</label>

                    <select
                      value={question.type}
                      onChange={(e) =>
                        updateQuestionType(questionIndex, e.target.value)
                      }
                    >
                      <option value="QCM">QCM</option>
                      <option value="TRUE_FALSE">Vrai / Faux</option>
                      <option value="TEXT">Réponse texte</option>
                    </select>
                  </div>

                  <button
                    type="button"
                    onClick={() => deleteQuestion(questionIndex)}
                  >
                    <Trash2 size={18} />
                  </button>
                </div>

                <textarea
                  value={question.questionText}
                  placeholder="Énoncé de la question"
                  onChange={(e) =>
                    updateQuestionText(questionIndex, e.target.value)
                  }
                />

                <div className={styles.pointsRow}>
                  <label>Points</label>

                  <input
                    type="number"
                    min="1"
                    value={question.points}
                    onChange={(e) =>
                      updatePoints(questionIndex, e.target.value)
                    }
                  />
                </div>

                {question.type === "TEXT" ? (
                  <div className={styles.textAnswerBlock}>
                    <label>Réponse attendue</label>

                    <textarea
                      value={question.correctAnswer}
                      placeholder="Réponse correcte attendue"
                      onChange={(e) =>
                        updateTextAnswer(questionIndex, e.target.value)
                      }
                    />
                  </div>
                ) : (
                  <div className={styles.optionsList}>
                    {question.options.map((option, optionIndex) => (
                      <div className={styles.optionRow} key={option.label}>
                        <div className={styles.optionLetter}>{option.label}</div>

                        <input
                          type="text"
                          value={option.text}
                          placeholder={`Choix ${option.label}`}
                          onChange={(e) =>
                            updateOption(
                              questionIndex,
                              optionIndex,
                              e.target.value
                            )
                          }
                        />

                        <label className={styles.correctLabel}>
                          <input
                            type="radio"
                            name={`correct-${questionIndex}`}
                            checked={question.correctAnswer === option.label}
                            onChange={() =>
                              updateCorrectAnswer(questionIndex, option.label)
                            }
                          />
                          Bonne réponse
                        </label>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </section>

      <div className={styles.footerActions}>
        <button
          type="button"
          className={styles.secondaryBtn}
          onClick={() => navigate(backPath)}
        >
          Annuler
        </button>

        <button
          type="button"
          className={styles.primaryBtn}
          onClick={saveQuiz}
          disabled={loadingSave}
        >
          <Save size={18} />
          {loadingSave ? "Enregistrement..." : "Enregistrer le quiz"}
        </button>
      </div>
    </div>
  );
}
