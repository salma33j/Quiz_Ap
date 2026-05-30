import React, { useEffect, useState } from "react";
import axios from "../../api/axiosInstance";
import styles from "./AvailableQuizzes.module.css";

const AvailableQuizzes = () => {
  const [quizzes, setQuizzes] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchQuizzes();
  }, []);

  const fetchQuizzes = async () => {
    try {
      const response = await axios.get("/quiz");
      setQuizzes(response.data);
    } catch (error) {
      console.error(
        "Erreur lors du chargement des quiz :",
        error
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles["available-quizzes-container"]}>
      <div className={styles["available-quizzes-header"]}>
        <h2>Available Quizzes</h2>

        <p>
          Choose a quiz and start testing your knowledge.
        </p>
      </div>

      {loading ? (
        <p className={styles["loading-text"]}>
          Loading quizzes...
        </p>
      ) : quizzes.length === 0 ? (
        <p className={styles["empty-text"]}>
          No quizzes available.
        </p>
      ) : (
        <div className={styles["quiz-grid"]}>
          {quizzes.map((quiz) => (
            <div
              className={styles["quiz-card"]}
              key={quiz.id}
            >
              <div
                className={styles["quiz-card-content"]}
              >
                <h3>{quiz.title}</h3>

                <p
                  className={
                    styles["quiz-description"]
                  }
                >
                  {quiz.description ||
                    "No description available"}
                </p>

                <div className={styles["quiz-info"]}>
                  <span>
                    ⏱ {quiz.durationMinutes || 0} min
                  </span>

                  <span>
                    ❓{" "}
                    {quiz.questions?.length || 0}{" "}
                    questions
                  </span>
                </div>

                <button
                  className={styles["start-btn"]}
                >
                  Start Quiz
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default AvailableQuizzes;