import React, { useEffect, useState } from "react";
import {
  CheckCircle2,
  XCircle,
  HelpCircle,
} from "lucide-react";

import axios from "../../api/axiosInstance";
import styles from "./QuizCorrections.module.css";

const QuizCorrections = () => {
  const [corrections, setCorrections] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchCorrections();
  }, []);

  const fetchCorrections = async () => {
    try {
      const response = await axios.get(
        "/student/corrections"
      );

      setCorrections(response.data);
    } catch (error) {
      console.error(
        "Erreur corrections :",
        error
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles["corrections-container"]}>
      <div className={styles["corrections-header"]}>
        <h1>Quiz Corrections</h1>

        <p>
          Review your answers and compare them
          with the correct ones.
        </p>
      </div>

      {loading ? (
        <div className={styles.loading}>
          Loading corrections...
        </div>
      ) : corrections.length === 0 ? (
        <div className={styles["empty-state"]}>
          No corrections available.
        </div>
      ) : (
        <div className={styles["corrections-grid"]}>
          {corrections.map((item, index) => (
            <div
              className={styles["correction-card"]}
              key={index}
            >
              <div className={styles["question-header"]}>
                <div className={styles["question-icon"]}>
                  <HelpCircle size={22} />
                </div>

                <h2>
                  Question {index + 1}
                </h2>
              </div>

              <div className={styles["question-text"]}>
                {item.question}
              </div>

              <div className={styles["answer-section"]}>
                <div
                  className={`${styles["student-answer"]} ${styles.wrong}`}
                >
                  <XCircle size={18} />

                  <div>
                    <span>Your Answer</span>

                    <p>{item.studentAnswer}</p>
                  </div>
                </div>

                <div
                  className={`${styles["correct-answer"]} ${styles.correct}`}
                >
                  <CheckCircle2 size={18} />

                  <div>
                    <span>Correct Answer</span>

                    <p>{item.correctAnswer}</p>
                  </div>
                </div>
              </div>

              {item.explanation && (
                <div className={styles["explanation-box"]}>
                  <h3>Explanation</h3>

                  <p>{item.explanation}</p>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default QuizCorrections;