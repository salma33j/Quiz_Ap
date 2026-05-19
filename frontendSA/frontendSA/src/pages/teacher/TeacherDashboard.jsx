import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  BookOpen,
  Users,
  BarChart3,
  Brain,
  CheckCircle2,
  Clock,
  Activity,
  FilePenLine,
} from "lucide-react";
import {
  BarChart,
  Bar,
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
} from "recharts";

import teacherQuizApi from "../../api/teacherQuizApi";
import styles from "./TeacherDashboard.module.css";

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
    totalStudents: 0,
    totalAttempts: 0,
    averageScore: 0,
  });

  const [quizzes, setQuizzes] = useState([]);
  const [students, setStudents] = useState([]);
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

    if (
      normalized.includes("facile") ||
      normalized.includes("easy") ||
      normalized === "1"
    ) {
      return "Facile";
    }

    if (
      normalized.includes("difficile") ||
      normalized.includes("hard") ||
      normalized === "3"
    ) {
      return "Difficile";
    }

    return "Moyen";
  };

  useEffect(() => {
    const loadData = async () => {
      try {
        const [dashboardData, quizzesData, studentsData] = await Promise.all([
          teacherQuizApi.getDashboard().catch(() => null),
          teacherQuizApi.getMyQuizzes().catch(() => []),
          teacherQuizApi.getStudents().catch(() => []),
        ]);

        const safeQuizzes = Array.isArray(quizzesData) ? quizzesData : [];
        const safeStudents = Array.isArray(studentsData) ? studentsData : [];

        setQuizzes(safeQuizzes);
        setStudents(safeStudents);

        const resultsResponses = await Promise.all(
          safeQuizzes.map((quiz) =>
            teacherQuizApi.getResults(quiz.id).catch(() => [])
          )
        );

        const allResults = resultsResponses.flat();

        const weekDays = ["Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"];

        const activityMap = {
          Lun: 0,
          Mar: 0,
          Mer: 0,
          Jeu: 0,
          Ven: 0,
          Sam: 0,
          Dim: 0,
        };

        allResults.forEach((result) => {
          const dateValue =
            result.createdAt ||
            result.submittedAt ||
            result.datePassage ||
            result.completedAt ||
            result.updatedAt;

          if (!dateValue) return;

          const dayIndex = new Date(dateValue).getDay();

          const dayName = dayIndex === 0 ? "Dim" : weekDays[dayIndex - 1];

          activityMap[dayName] += 1;
        });

        setWeeklyActivity(
          weekDays.map((day) => ({
            day,
            attempts: activityMap[day],
          }))
        );

        setStats({
          totalQuizzes:
            dashboardData?.totalQuizzes ??
            dashboardData?.quizCount ??
            safeQuizzes.length,

          publishedQuizzes:
            dashboardData?.publishedQuizzes ??
            safeQuizzes.filter((q) => q.status === "PUBLISHED").length,

          draftQuizzes:
            dashboardData?.draftQuizzes ??
            safeQuizzes.filter((q) => q.status !== "PUBLISHED").length,

          totalStudents:
            dashboardData?.totalStudents ??
            dashboardData?.studentsCount ??
            safeStudents.length,

          totalAttempts:
            dashboardData?.totalAttempts ??
            dashboardData?.attemptsCount ??
            allResults.length,

          averageScore:
            dashboardData?.averageScore ??
            dashboardData?.moyenneScore ??
            0,
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
      const difficulty = getDifficulty(quiz);
      result[difficulty] += 1;
    });

    return [
      { name: "Facile", value: result.Facile },
      { name: "Moyen", value: result.Moyen },
      { name: "Difficile", value: result.Difficile },
    ];
  }, [quizzes]);

  const recentQuizzes = quizzes.slice(0, 4);

  const cards = [
    {
      label: "Quiz créés",
      value: stats.totalQuizzes,
      icon: BookOpen,
      hint: "total dans votre espace",
      color: "blue",
    },
    {
      label: "Quiz publiés",
      value: stats.publishedQuizzes,
      icon: CheckCircle2,
      hint: "visibles aux étudiants",
      color: "green",
    },
    {
      label: "Brouillons",
      value: stats.draftQuizzes,
      icon: FilePenLine,
      hint: "à compléter",
      color: "orange",
    },
    {
      label: "Étudiants",
      value: stats.totalStudents,
      icon: Users,
      hint: "assignés ou inscrits",
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
          <p className={styles.hello}>Bonjour {teacherName} 👋</p>
          <p className={styles.subtitle}>
            Résumé clair de vos quiz, étudiants et activités.
          </p>
        </div>

        <div className={styles.todayBox}>
          <span>Aujourd’hui</span>
          <strong>{new Date().toLocaleDateString("fr-FR")}</strong>
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
              <h2>Difficulté des quiz</h2>
              <p>Répartition réelle des quiz par niveau.</p>
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
              <h2>Activité hebdomadaire</h2>
              <p>Nombre réel de tentatives par jour.</p>
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
              <h2>Quiz récents</h2>
              <p>Les derniers quiz créés dans votre espace.</p>
            </div>

            <button onClick={() => navigate("/teacher/quizzes")}>
              Voir tout
            </button>
          </div>

          <div className={styles.quizList}>
            {recentQuizzes.length > 0 ? (
              recentQuizzes.map((quiz) => {
                const isPublished = quiz.status === "PUBLISHED";

                return (
                  <div className={styles.quizItem} key={quiz.id}>
                    <div className={styles.quizIcon}>
                      <BookOpen size={20} />
                    </div>

                    <div>
                      <h3>{quiz.titre || quiz.title || "Quiz sans titre"}</h3>
                      <p>
                        {quiz.theme || "Sans thème"} • {getDifficulty(quiz)} •{" "}
                        {isPublished ? "Publié" : "Brouillon"}
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
                        onClick={() =>
                          navigate(`/teacher/quizzes/${quiz.id}/questions`)
                        }
                      >
                        Modifier
                      </button>
                    )}
                  </div>
                );
              })
            ) : (
              <div className={styles.emptyBox}>
                Aucun quiz créé pour le moment.
              </div>
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
              Utilisez l’IA pour générer des questions, des remarques et des
              feedbacks personnalisés.
            </p>

            <button onClick={() => navigate("/teacher/ai-generator")}>
              Générer avec IA
            </button>
          </div>

          <div className={styles.activityCard}>
            <h2>Indicateurs utiles</h2>

            <div className={styles.activityItem}>
              <CheckCircle2 size={18} />
              <span>{stats.publishedQuizzes} quiz disponibles</span>
            </div>

            <div className={styles.activityItem}>
              <Clock size={18} />
              <span>{stats.draftQuizzes} quiz à finaliser</span>
            </div>

            <div className={styles.activityItem}>
              <Users size={18} />
              <span>{stats.totalStudents} étudiants concernés</span>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
};

export default TeacherDashboard;