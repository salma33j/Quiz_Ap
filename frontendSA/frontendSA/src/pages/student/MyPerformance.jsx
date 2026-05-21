// src/pages/student/MyPerformance.jsx

import React, { useEffect, useState } from "react";

import {
  Trophy,
  CheckCircle,
  XCircle,
  BarChart3,
} from "lucide-react";

import axios from "../../api/axiosInstance";
import styles from "./MyPerformance.module.css";

const MyPerformance = () => {
  const [stats, setStats] = useState({
    averageScore: 0,
    completedQuizzes: 0,
    successRate: 0,
    failedQuizzes: 0,
  });

  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchPerformance();
  }, []);

  const fetchPerformance = async () => {
    try {
      const response = await axios.get(
        "/student/performance"
      );

      setStats(response.data);
    } catch (error) {
      console.error(
        "Erreur performances :",
        error
      );
    } finally {
      setLoading(false);
    }
  };

  const cards = [
    {
      title: "Average Score",
      value: `${stats.averageScore}%`,
      icon: Trophy,
    },
    {
      title: "Completed Quizzes",
      value: stats.completedQuizzes,
      icon: CheckCircle,
    },
    {
      title: "Failed Quizzes",
      value: stats.failedQuizzes,
      icon: XCircle,
    },
    {
      title: "Success Rate",
      value: `${stats.successRate}%`,
      icon: BarChart3,
    },
  ];

  return (
    <div
      className={styles["performance-container"]}
    >
      <div
        className={styles["performance-header"]}
      >
        <h1>My Performance</h1>

        <p>
          Track your quiz statistics and
          progress.
        </p>
      </div>

      {loading ? (
        <div className={styles.loading}>
          Loading performance...
        </div>
      ) : (
        <>
          <div
            className={styles["performance-grid"]}
          >
            {cards.map((card, index) => {
              const Icon = card.icon;

              return (
                <div
                  className={
                    styles["performance-card"]
                  }
                  key={index}
                >
                  <div
                    className={styles["card-top"]}
                  >
                    <div
                      className={styles["icon-box"]}
                    >
                      <Icon size={26} />
                    </div>
                  </div>

                  <h2>{card.value}</h2>

                  <p>{card.title}</p>
                </div>
              );
            })}
          </div>

          <div
            className={
              styles["performance-summary"]
            }
          >
            <div
              className={styles["summary-card"]}
            >
              <h2>Performance Summary</h2>

              <p>
                Your average score is{" "}
                <strong>
                  {stats.averageScore}%
                </strong>
                . Keep practicing to improve your
                performance and achieve higher
                quiz scores.
              </p>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default MyPerformance;