// src/pages/student/MyHistory.jsx

import React, { useEffect, useState } from "react";
import {
  Clock,
  Trophy,
  CalendarDays,
} from "lucide-react";

import axios from "../../api/axiosInstance";
import styles from "./MyHistory.module.css";

const MyHistory = () => {
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchHistory();
  }, []);

  const fetchHistory = async () => {
    try {
      const response = await axios.get(
        "/student/history"
      );

      setHistory(response.data);
    } catch (error) {
      console.error(
        "Erreur historique :",
        error
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles["history-container"]}>
      <div className={styles["history-header"]}>
        <h1>My Quiz History</h1>

        <p>
          View all quizzes you have completed
          and your scores.
        </p>
      </div>

      {loading ? (
        <div className={styles.loading}>
          Loading history...
        </div>
      ) : history.length === 0 ? (
        <div className={styles["empty-history"]}>
          No quiz history found.
        </div>
      ) : (
        <div className={styles["history-grid"]}>
          {history.map((item) => (
            <div
              className={styles["history-card"]}
              key={item.id}
            >
              <div className={styles["history-top"]}>
                <h2>{item.quizTitle}</h2>

                <div
                  className={styles["score-badge"]}
                >
                  <Trophy size={18} />

                  <span>{item.score}%</span>
                </div>
              </div>

              <div className={styles["history-info"]}>
                <div className={styles["info-item"]}>
                  <Clock size={16} />

                  <span>
                    Duration :{" "}
                    {item.durationMinutes} min
                  </span>
                </div>

                <div className={styles["info-item"]}>
                  <CalendarDays size={16} />

                  <span>
                    {new Date(
                      item.completedAt
                    ).toLocaleDateString()}
                  </span>
                </div>
              </div>

              <div
                className={styles["history-footer"]}
              >
                <button
                  className={styles["details-btn"]}
                >
                  View Details
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default MyHistory;