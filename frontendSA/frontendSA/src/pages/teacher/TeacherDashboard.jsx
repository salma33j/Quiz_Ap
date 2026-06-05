import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Activity,
  BarChart3,
  BookOpen,
  Brain,
  CheckCircle2,
  Clock,
  FilePenLine,
  MailCheck,
  Users,
} from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

import teacherQuizApi from "../../api/teacherQuizApi";
import styles from "./TeacherDashboard.module.css";

const asArray = (value) => {
  if (Array.isArray(value)) return value;
  if (Array.isArray(value?.data)) return value.data;
  if (Array.isArray(value?.items)) return value.items;
  if (Array.isArray(value?.content)) return value.content;
  return [];
};

const getStatus = (quiz) => String(quiz?.status || "DRAFT").toUpperCase();
const getQuestionCount = (quiz) =>
  Number(quiz?.questionCount ?? quiz?.questionsCount ?? quiz?.nombreQuestions ?? 0);
const getAllowedCount = (quiz) =>
  Number(quiz?.totalStudentsAllowed ?? quiz?.allowedStudentsCount ?? quiz?.studentCount ?? 0);
const getClassKey = (quiz) =>
  quiz?.classeId ??
  quiz?.classId ??
  quiz?.classe?.id ??
  quiz?.className ??
  quiz?.classeName ??
  null;

const countStudentsByUniqueClass = (quizzes) => {
  const classCounts = new Map();
  let withoutClassCount = 0;

  quizzes.forEach((quiz) => {
    const allowedCount = getAllowedCount(quiz);
    if (allowedCount <= 0) return;

    const classKey = getClassKey(quiz);
    if (!classKey) {
      withoutClassCount += allowedCount;
      return;
    }

    const key = String(classKey);
    classCounts.set(key, Math.max(classCounts.get(key) || 0, allowedCount));
  });

  return [...classCounts.values()].reduce((sum, count) => sum + count, 0) + withoutClassCount;
};

const TeacherDashboard = () => {
  const navigate = useNavigate();

  const teacherName =
    localStorage.getItem("firstName") ||
    localStorage.getItem("name") ||
    "Professeur";

  const [stats, setStats] = useState({
    totalQuizzes: 0,
    publishedQuizzes: 0,
    draftQuizzes: 0,
    readyToPublish: 0,
    totalStudents: 0,
    totalAttempts: 0,
    averageScore: 0,
  });

  const [quizzes, setQuizzes] = useState([]);
  const [weeklyActivity, setWeeklyActivity] = useState([]);
  const [loading, setLoading] = useState(true);

  const getDifficulty = (quiz) => {
    const value =
      quiz.difficulty ||
      quiz.difficulte ||
      quiz.level ||
      quiz.niveau ||
      quiz.difficultyLevel ||
      "";

    const normalized = String(value).toLowerCase();

    if (normalized.includes("facile") || normalized.includes("easy") || normalized === "1") {
      return "Facile";
    }

    if (normalized.includes("difficile") || normalized.includes("hard") || normalized === "3") {
      return "Difficile";
    }

    return "Moyen";
  };

  useEffect(() => {
    const loadData = async () => {
      try {
        const [dashboardData, quizzesData] = await Promise.all([
          teacherQuizApi.getDashboard().catch(() => []),
          teacherQuizApi.getMyQuizzes().catch(() => []),
        ]);

        const safeQuizzes = asArray(quizzesData);
        const dashboardRows = asArray(dashboardData);

        setQuizzes(safeQuizzes);

        const resultsResponses = await Promise.all(
          safeQuizzes.map((quiz) => teacherQuizApi.getResults(quiz.id).catch(() => []))
        );
        const allResults = resultsResponses.flatMap(asArray);

        const weekDays = ["Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"];
        const activityMap = Object.fromEntries(weekDays.map((day) => [day, 0]));

        allResults.forEach((result) => {
          const dateValue =
            result.createdAt ||
            result.submittedAt ||
            result.datePassage ||
            result.completedAt ||
            result.completedDate ||
            result.updatedAt;

          if (!dateValue) return;

          const dayIndex = new Date(dateValue).getDay();
          const dayName = dayIndex === 0 ? "Dim" : weekDays[dayIndex - 1];
          activityMap[dayName] += 1;
        });

        setWeeklyActivity(weekDays.map((day) => ({ day, attempts: activityMap[day] })));

        const totalAttemptsFromStats = dashboardRows.reduce(
          (sum, row) => sum + Number(row.totalParticipants ?? 0),
          0
        );
        const totalStudentsByClass = countStudentsByUniqueClass(safeQuizzes);
        const weightedScore = dashboardRows.reduce((sum, row) => {
          const participants = Number(row.totalParticipants ?? 0);
          return sum + Number(row.moyenneScore ?? 0) * participants;
        }, 0);
        const simpleAverage =
          dashboardRows.length > 0
            ? dashboardRows.reduce((sum, row) => sum + Number(row.moyenneScore ?? 0), 0) /
              dashboardRows.length
            : 0;

        setStats({
          totalQuizzes: safeQuizzes.length || dashboardRows.length,
          publishedQuizzes: safeQuizzes.filter((q) => getStatus(q) === "PUBLISHED").length,
          draftQuizzes: safeQuizzes.filter((q) => getStatus(q) === "DRAFT").length,
          readyToPublish: safeQuizzes.filter(
            (q) => getStatus(q) === "DRAFT" && getQuestionCount(q) >= 15 && getAllowedCount(q) > 0
          ).length,
          totalStudents: totalStudentsByClass,
          totalAttempts: totalAttemptsFromStats || allResults.length,
          averageScore:
            totalAttemptsFromStats > 0 ? weightedScore / totalAttemptsFromStats : simpleAverage,
        });
      } catch (error) {
        console.error("Erreur dashboard professeur :", error);
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, []);

  const difficultyData = useMemo(() => {
    const result = {
      Facile: 0,
      Moyen: 0,
      Difficile: 0,
    };

    quizzes.forEach((quiz) => {
      result[getDifficulty(quiz)] += 1;
    });

    return [
      { name: "Facile", value: result.Facile },
      { name: "Moyen", value: result.Moyen },
      { name: "Difficile", value: result.Difficile },
    ];
  }, [quizzes]);

  const recentQuizzes = useMemo(
    () =>
      [...quizzes]
        .sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))
        .slice(0, 4),
    [quizzes]
  );

  const cards = [
    {
      label: "Quiz crees",
      value: stats.totalQuizzes,
      icon: BookOpen,
      hint: "total dans votre espace",
      color: "blue",
    },
    {
      label: "Quiz publies",
      value: stats.publishedQuizzes,
      icon: CheckCircle2,
      hint: "visibles aux etudiants",
      color: "green",
    },
    {
      label: "Brouillons",
      value: stats.draftQuizzes,
      icon: FilePenLine,
      hint: "a completer",
      color: "orange",
    },
    {
      label: "Prets a publier",
      value: stats.readyToPublish,
      icon: MailCheck,
      hint: "questions et etudiants OK",
      color: "cyan",
    },
    {
      label: "Étudiants",
      value: stats.totalStudents,
      icon: Users,
      hint: "dans vos classes",
      color: "purple",
    },
    {
      label: "Tentatives",
      value: stats.totalAttempts,
      icon: Activity,
      hint: "réponses envoyées",
      color: "cyan",
    },
    {
      label: "Moyenne",
      value: `${Math.round(stats.averageScore || 0)}%`,
      icon: BarChart3,
      hint: "score général",
      color: "pink",
    },
  ];

  return (
    <div className={styles.page}>
      <section className={styles.topSection}>
        <div>
          <span className={styles.badge}>Professeur</span>
          <h1 className={styles.hello}>Tableau de bord enseignant</h1>
          <p className={styles.subtitle}>
            Bonjour {teacherName}, suivez vos quiz, étudiants, publications et activités.
          </p>
        </div>
      </section>

      <section className={styles.statsGrid}>
        {cards.map((card) => {
          const Icon = card.icon;

          return (
            <div className={styles.statCard} key={card.label}>
              <div className={`${styles.statIcon} ${styles[card.color]}`}>
                <Icon size={22} />
              </div>

              <div>
                <span>{card.label}</span>
                <strong>{loading ? "..." : card.value}</strong>
                <p>{card.hint}</p>
              </div>
            </div>
          );
        })}
      </section>

      <section className={styles.analyticsGrid}>
        <div className={styles.chartCard}>
          <div className={styles.cardHeader}>
            <div>
              <h2>Difficulte des quiz</h2>
              <p>Repartition reelle des quiz par niveau.</p>
            </div>
          </div>

          <div className={styles.chartBox}>
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={difficultyData}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="name" />
                <YAxis allowDecimals={false} />
                <Tooltip />
                <Bar dataKey="value" radius={[14, 14, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className={styles.chartCard}>
          <div className={styles.cardHeader}>
            <div>
              <h2>Activite hebdomadaire</h2>
              <p>Nombre reel de tentatives par jour.</p>
            </div>
          </div>

          <div className={styles.chartBox}>
            <ResponsiveContainer width="100%" height={260}>
              <LineChart data={weeklyActivity}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="day" />
                <YAxis allowDecimals={false} />
                <Tooltip />
                <Line
                  type="monotone"
                  dataKey="attempts"
                  strokeWidth={3}
                  dot={{ r: 5 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
      </section>

      <section className={styles.bottomGrid}>
        <div className={styles.panel}>
          <div className={styles.cardHeader}>
            <div>
              <h2>Quiz recents</h2>
              <p>Les derniers quiz crees dans votre espace.</p>
            </div>

            <button onClick={() => navigate("/teacher/quizzes")}>Voir tout</button>
          </div>

          <div className={styles.quizList}>
            {recentQuizzes.length > 0 ? (
              recentQuizzes.map((quiz) => {
                const isPublished = getStatus(quiz) === "PUBLISHED";

                return (
                  <div className={styles.quizItem} key={quiz.id}>
                    <div className={styles.quizIcon}>
                      <BookOpen size={20} />
                    </div>

                    <div>
                      <h3>{quiz.titre || quiz.title || "Quiz sans titre"}</h3>
                      <p>
                        {quiz.theme || "Sans theme"} - {getDifficulty(quiz)} -{" "}
                        {isPublished ? "Publie" : "Brouillon"}
                      </p>
                    </div>

                    {isPublished ? (
                      <button
                        className={styles.viewBtn}
                        onClick={() => navigate(`/teacher/quizzes/${quiz.id}`)}
                      >
                        Voir
                      </button>
                    ) : (
                      <button
                        className={styles.editBtn}
                        onClick={() => navigate(`/teacher/quizzes/${quiz.id}/questions`)}
                      >
                        Modifier
                      </button>
                    )}
                  </div>
                );
              })
            ) : (
              <div className={styles.emptyBox}>Aucun quiz cree pour le moment.</div>
            )}
          </div>
        </div>

        <div className={styles.sideColumn}>
          <div className={styles.aiCard}>
            <div className={styles.aiIcon}>
              <Brain size={24} />
            </div>

            <h2>Assistant IA</h2>
            <p>
              Utilisez l'IA pour generer des questions, des remarques et des feedbacks
              personnalises.
            </p>

            <button onClick={() => navigate("/teacher/ai-generator")}>
              Generer avec IA
            </button>
          </div>

          <div className={styles.activityCard}>
            <h2>Indicateurs utiles</h2>

            <div className={styles.activityItem}>
              <CheckCircle2 size={18} />
              <span>{stats.publishedQuizzes} quiz disponibles</span>
            </div>

            <div className={styles.activityItem}>
              <MailCheck size={18} />
              <span>{stats.readyToPublish} quiz prets a notifier</span>
            </div>

            <div className={styles.activityItem}>
              <Clock size={18} />
              <span>{stats.draftQuizzes} quiz a finaliser</span>
            </div>

            <div className={styles.activityItem}>
              <Users size={18} />
              <span>{stats.totalStudents} etudiants concernes</span>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
};

export default TeacherDashboard;
