import { useEffect, useMemo, useState } from "react";
import {
  Trophy,
  CheckCircle2,
  BarChart3,
  Award,
  BookOpen,
} from "lucide-react";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";

import studentQuizApi from "../../api/studentQuizApi";
import styles from "./MyPerformance.module.css";

const getSubjectName = (item) =>
  item?.matiereNom ||
  item?.matiereName ||
  item?.subjectName ||
  item?.nom ||
  item?.name ||
  item?.quizTheme ||
  item?.theme ||
  item?.matiere?.nom ||
  "Matière générale";

const getSubjectId = (item) =>
  item?.matiereId ||
  item?.subjectId ||
  item?.matiere?.id ||
  item?.subject?.id ||
  item?.id ||
  getSubjectName(item);

const getQuizSubjectId = (item) =>
  item?.matiereId ||
  item?.subjectId ||
  item?.matiere?.id ||
  item?.subject?.id ||
  getSubjectName(item);

const getScore = (item) => {
  const raw =
    item?.scorePercentage ??
    item?.percentage ??
    item?.score ??
    (item?.noteSur20 != null ? Number(item.noteSur20) * 5 : 0);

  const value = Number(raw);
  return Number.isFinite(value) ? value : 0;
};

export default function MyPerformance() {
  const [perf, setPerf] = useState({
    moyenneScore: 0,
    meilleurScore: 0,
    pireScore: 0,
    totalParticipants: 0,
  });

  const [history, setHistory] = useState([]);
  const [matieres, setMatieres] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      setError("");

      const [perfRes, historyRes, matieresData] = await Promise.all([
        studentQuizApi.getMyPerformance(),
        studentQuizApi.getMyResultsHistory(),
        studentQuizApi.getMySubjects().catch(() => []),
      ]);

      const perfData = perfRes;
      const historyData = historyRes;

      const completed = Array.isArray(historyData)
        ? historyData.filter((item) => item.isCompleted !== false)
        : [];

      setPerf(perfData || {});
      setHistory(completed);
      setMatieres(Array.isArray(matieresData) ? matieresData : []);
    } catch (err) {
      console.error("Erreur performances:", err);
      setError("Impossible de charger vos performances.");
    } finally {
      setLoading(false);
    }
  };

  const subjectAverages = useMemo(() => {
    const map = new Map();

    matieres.forEach((matiere) => {
      const id = String(getSubjectId(matiere));

      map.set(id, {
        id,
        subject: getSubjectName(matiere),
        total: 0,
        count: 0,
        average: 0,
      });
    });

    history.forEach((item) => {
      const id = String(getQuizSubjectId(item));

      if (!map.has(id)) {
        map.set(id, {
          id,
          subject: getSubjectName(item),
          total: 0,
          count: 0,
          average: 0,
        });
      }

      const row = map.get(id);
      row.total += getScore(item);
      row.count += 1;
      row.average = Math.round(row.total / row.count);
    });

    return Array.from(map.values()).sort((a, b) =>
      a.subject.localeCompare(b.subject)
    );
  }, [matieres, history]);

  const cards = [
    {
      label: "Moyenne générale",
      value: `${Math.round(perf.moyenneScore || 0)}%`,
      icon: BarChart3,
      color: "purple",
    },
    {
      label: "Meilleur score",
      value: `${Math.round(perf.meilleurScore || 0)}%`,
      icon: Trophy,
      color: "green",
    },
    {
      label: "Score le plus bas",
      value: `${Math.round(perf.pireScore || 0)}%`,
      icon: Award,
      color: "orange",
    },
    {
      label: "Total tentatives",
      value: history.length,
      icon: CheckCircle2,
      color: "blue",
    },
  ];

  if (loading) {
    return (
      <div className={styles.loading}>
        Chargement de vos performances...
      </div>
    );
  }

  if (error) {
    return <div className={styles.error}>{error}</div>;
  }

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <span className={styles.badge}>Étudiant</span>
        <h1>Mes performances</h1>
        <p>
          Suivez vos statistiques générales et votre moyenne par matière assignée.
        </p>
      </div>

      <div className={styles.statsGrid}>
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
              </div>
            </div>
          );
        })}
      </div>

      {subjectAverages.length === 0 ? (
        <div className={styles.empty}>
          <p>Aucune matière assignée pour le moment.</p>
        </div>
      ) : (
        <>
          <div className={styles.chartCard}>
            <h2>Moyenne du score par matière</h2>
            <p>
              Les matières sans quiz répondu restent à 0%.
            </p>

            <div className={styles.chartBox}>
              <ResponsiveContainer width="100%" height={320}>
                <BarChart data={subjectAverages}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="subject" />
                  <YAxis domain={[0, 100]} />
                  <Tooltip formatter={(value) => [`${value}%`, "Moyenne"]} />
                  <Bar dataKey="average" />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className={styles.subjectCard}>
            <h2>Détail par matière</h2>

            <div className={styles.subjectList}>
              {subjectAverages.map((item) => (
                <div className={styles.subjectRow} key={item.id}>
                  <div className={styles.subjectInfo}>
                    <div className={styles.subjectIcon}>
                      <BookOpen size={20} />
                    </div>

                    <div>
                      <strong>{item.subject}</strong>
                      <span>{item.count} quiz répondu(s)</span>
                    </div>
                  </div>

                  <div className={styles.averageBadge}>
                    {item.average}%
                  </div>
                </div>
              ))}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
