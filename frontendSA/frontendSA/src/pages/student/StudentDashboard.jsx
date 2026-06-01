import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  BookOpen,
  CheckCircle2,
  Activity,
  BarChart3,
  Trophy,
  Award,
  TrendingUp,
  Clock,
  Brain,
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
  Cell,
} from "recharts";

import studentQuizApi from "../../api/studentQuizApi";
import axiosInstance from "../../api/axiosInstance";
import styles from "./StudentDashboard.module.css";

const StudentDashboard = () => {
  const navigate = useNavigate();

  const user = JSON.parse(localStorage.getItem("user"));
  const studentName = user?.firstName || user?.username || "Étudiant";

  const [stats, setStats] = useState({
    totalQuizzes: 0,
    completedQuizzes: 0,
    averageScore: 0,
    bestScore: 0,
    worstScore: 0,
    totalAttempts: 0,
    ranking: 0,
    totalStudents: 0,
  });

  const [recentQuizzes, setRecentQuizzes] = useState([]);
  const [performanceBySubject, setPerformanceBySubject] = useState([]);
  const [weeklyActivity, setWeeklyActivity] = useState([]);
  const [loading, setLoading] = useState(true);

  const COLORS = ["#4F46E5", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6", "#EC4899"];

  useEffect(() => {
    loadDashboard();
  }, []);

  const loadDashboard = async () => {
    try {
      // 1. Quiz disponibles
      const availableQuizzes = await studentQuizApi.getAvailableQuizzes();

      // 2. Historique réel depuis /api/resultats/my-history
      const historyRes = await axiosInstance.get("/resultats/my-history");
      const history = historyRes.data?.data ?? historyRes.data ?? [];

      // 3. Performance depuis /api/statistiques/student/my-performance
      const perfRes = await axiosInstance.get("/statistiques/student/my-performance");
      const perf = perfRes.data?.data ?? perfRes.data ?? {};

      // 4. Calcul classement global (meilleur rang parmi tous les quiz)
      let bestRanking = 0;
      let totalStudentsCount = 0;
      if (history.length > 0) {
        const rankingPromises = history
          .filter(h => h.quizId)
          .map(h =>
            axiosInstance
              .get(`/statistiques/student/ranking/${h.quizId}`)
              .then(r => r.data?.data ?? r.data)
              .catch(() => null)
          );
        const rankings = await Promise.all(rankingPromises);
        const validRankings = rankings.filter(r => r && r.rang);
        if (validRankings.length > 0) {
          bestRanking = Math.min(...validRankings.map(r => r.rang));
        }
      }

      // 5. Performances par matière/thème depuis l'historique
      const themeMap = {};
      history.forEach((h, idx) => {
        const theme = h.quizTheme || h.subjectName || `Quiz ${idx + 1}`;
        if (!themeMap[theme]) {
          themeMap[theme] = { total: 0, count: 0 };
        }
        if (h.scorePercentage != null) {
          themeMap[theme].total += h.scorePercentage;
          themeMap[theme].count += 1;
        }
      });

      const subjectPerformance = Object.entries(themeMap)
        .slice(0, 6)
        .map(([subject, data], idx) => ({
          subject: subject.length > 12 ? subject.slice(0, 12) + "…" : subject,
          score: data.count > 0 ? Math.round(data.total / data.count) : 0,
          color: COLORS[idx % COLORS.length],
        }));

      // 6. Activité hebdomadaire depuis l'historique
      const weekDays = ["Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"];
      const activityCount = [0, 0, 0, 0, 0, 0, 0];
      const now = new Date();
      history.forEach(h => {
        if (h.completedDate) {
          const date = new Date(h.completedDate);
          const diffDays = Math.floor((now - date) / (1000 * 60 * 60 * 24));
          if (diffDays < 7) {
            const dayIndex = (date.getDay() + 6) % 7; // Lundi = 0
            activityCount[dayIndex]++;
          }
        }
      });
      setWeeklyActivity(weekDays.map((day, i) => ({ day, attempts: activityCount[i] })));

      // 7. Derniers quiz passés
      const recent = history.slice(0, 4).map(h => ({
        title: h.quizTitle || "Quiz",
        score: h.scorePercentage ? Math.round(h.scorePercentage) : null,
        date: h.completedDate
          ? new Date(h.completedDate).toLocaleDateString("fr-FR")
          : "—",
        questions: h.totalPoints || 0,
        quizId: h.quizId,
        isCompleted: h.isCompleted,
      }));

      setPerformanceBySubject(subjectPerformance);
      setRecentQuizzes(recent);

      setStats({
        totalQuizzes: availableQuizzes?.length ?? 0,
        completedQuizzes: history.filter(h => h.isCompleted).length,
        averageScore: perf.moyenneScore ? Math.round(perf.moyenneScore) : 0,
        bestScore: perf.meilleurScore ? Math.round(perf.meilleurScore) : 0,
        totalAttempts: history.length,
        ranking: bestRanking,
        totalStudents: totalStudentsCount,
      });

    } catch (error) {
      console.error("Erreur dashboard étudiant :", error);
    } finally {
      setLoading(false);
    }
  };

  const cards = [
    {
      label: "Quiz disponibles",
      value: stats.totalQuizzes,
      icon: BookOpen,
      hint: "à compléter",
      color: "blue",
    },
    {
      label: "Quiz terminés",
      value: stats.completedQuizzes,
      icon: CheckCircle2,
      hint: "complétés",
      color: "green",
    },
    {
      label: "Moyenne générale",
      value: `${stats.averageScore}%`,
      icon: BarChart3,
      hint: "score général",
      color: "purple",
    },
    {
      label: "Total tentatives",
      value: stats.totalAttempts,
      icon: Activity,
      hint: "quiz passés",
      color: "cyan",
    },
    {
      label: "Classement",
      value: stats.ranking > 0 ? `#${stats.ranking}` : "—",
      icon: Trophy,
      hint: "meilleur rang",
      color: "orange",
    },
    {
      label: "Meilleur score",
      value: `${stats.bestScore}%`,
      icon: Award,
      hint: "meilleure tentative",
      color: "pink",
    },
  ];

  if (loading) {
    return <div className={styles.loading}>Chargement...</div>;
  }

  return (
    <div className={styles.page}>

      {/* Top Section */}
      <section className={styles.topSection}>
        <div>
          <p className={styles.hello}>Bonjour {studentName} 👋</p>
          <p className={styles.subtitle}>Résumé de vos quiz, résultats et activités.</p>
        </div>
        <div className={styles.todayBox}>
          <span>Aujourd'hui</span>
          <strong>{new Date().toLocaleDateString("fr-FR")}</strong>
        </div>
      </section>

      {/* Stats Grid */}
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
                <strong>{card.value}</strong>
                <p>{card.hint}</p>
              </div>
            </div>
          );
        })}
      </section>

      {/* Analytics Grid */}
      <section className={styles.analyticsGrid}>
        <div className={styles.chartCard}>
          <div className={styles.cardHeader}>
            <div>
              <h2>Progression par matière</h2>
              <p>Score moyen par thème de quiz.</p>
            </div>
          </div>
          <div className={styles.chartBox}>
            {performanceBySubject.length > 0 ? (
              <ResponsiveContainer width="100%" height={260}>
                <BarChart data={performanceBySubject}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="subject" />
                  <YAxis domain={[0, 100]} />
                  <Tooltip formatter={(v) => `${v}%`} />
                  <Bar dataKey="score" radius={[14, 14, 0, 0]}>
                    {performanceBySubject.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className={styles.emptyBox}>
                Aucune donnée de performance disponible.
              </div>
            )}
          </div>
        </div>

        <div className={styles.chartCard}>
          <div className={styles.cardHeader}>
            <div>
              <h2>Activité hebdomadaire</h2>
              <p>Nombre de quiz complétés cette semaine.</p>
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
                  stroke="#4F46E5"
                  strokeWidth={3}
                  dot={{ r: 5 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
      </section>

      {/* Bottom Grid */}
      <section className={styles.bottomGrid}>
        <div className={styles.panel}>
          <div className={styles.cardHeader}>
            <div>
              <h2>Derniers quiz passés</h2>
              <p>Activité récente de vos tentatives.</p>
            </div>
            <button onClick={() => navigate("/student/history")}>Voir tout</button>
          </div>
          <div className={styles.quizList}>
            {recentQuizzes.length > 0 ? (
              recentQuizzes.map((quiz, idx) => (
                <div className={styles.quizItem} key={idx}>
                  <div className={styles.quizIcon}>
                    <BookOpen size={20} />
                  </div>
                  <div>
                    <h3>{quiz.title}</h3>
                    <p>{quiz.date} • {quiz.score != null ? `${quiz.score}%` : "En cours"}</p>
                  </div>
                  {quiz.score != null ? (
                    <button className={styles.viewBtn}>{quiz.score}%</button>
                  ) : (
                    <button
                      className={styles.editBtn}
                      onClick={() => navigate(`/student/quiz/${quiz.quizId}`)}
                    >
                      Reprendre
                    </button>
                  )}
                </div>
              ))
            ) : (
              <div className={styles.emptyBox}>
                Aucun quiz passé pour le moment.
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
              Obtenez des recommandations personnalisées pour améliorer vos scores.
            </p>
            <button onClick={() => navigate("/student/ai-recommendations")}>
              Générer avec IA
            </button>
          </div>

          <div className={styles.activityCard}>
            <h2>Indicateurs utiles</h2>
            <div className={styles.activityItem}>
              <CheckCircle2 size={18} />
              <span>{stats.completedQuizzes} quiz complétés</span>
            </div>
            <div className={styles.activityItem}>
              <Clock size={18} />
              <span>{stats.totalQuizzes} quiz disponibles</span>
            </div>
            <div className={styles.activityItem}>
              <TrendingUp size={18} />
              <span>Moyenne: {stats.averageScore}%</span>
            </div>
            <div className={styles.activityItem}>
              <Trophy size={18} />
              <span>
                Meilleur rang: {stats.ranking > 0 ? `#${stats.ranking}` : "—"}
              </span>
            </div>
          </div>
        </div>
      </section>

    </div>
  );
};

export default StudentDashboard;