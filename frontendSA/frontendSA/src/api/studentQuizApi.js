import axiosInstance from "./axiosInstance";

const unwrap = (res) => res?.data?.data ?? res?.data ?? res;

const studentQuizApi = {
  getMySubjects: async () => {
    const res = await axiosInstance.get("/student/matieres");
    return unwrap(res);
  },

  getAvailableQuizzes: async () => {
    const res = await axiosInstance.get("/student/quizzes/available");
    return unwrap(res);
  },

  getQuizDetails: async (quizId) => {
    const res = await axiosInstance.get(`/student/quizzes/${quizId}`);
    return unwrap(res);
  },

  getQuestions: async (quizId) => {
    const res = await axiosInstance.get(`/student/quizzes/${quizId}/questions`);
    return unwrap(res);
  },

  getQuizQuestions: async (quizId) => {
    const res = await axiosInstance.get(`/student/quizzes/${quizId}/questions`);
    return unwrap(res);
  },

  startQuiz: async (quizId) => {
    const res = await axiosInstance.post(`/student/quizzes/${quizId}/start`);
    return unwrap(res);
  },

  getRemainingSeconds: async (quizId) => {
    const res = await axiosInstance.get(
      `/student/quizzes/${quizId}/remaining-seconds`
    );

    const data = unwrap(res);

    if (typeof data === "number") return data;
    if (data?.remainingSeconds !== undefined) return data.remainingSeconds;
    if (data?.seconds !== undefined) return data.seconds;

    return null;
  },

  saveAnswer: async (data) => {
    const res = await axiosInstance.post("/reponses", data);
    return unwrap(res);
  },

  submitQuiz: async (quizId) => {
    const res = await axiosInstance.post(`/reponses/quiz/${quizId}/submit`);
    return unwrap(res);
  },

  submitQuizWithAnswers: async (quizId, answers = []) => {
    const res = await axiosInstance.post(
      `/reponses/quiz/${quizId}/submit-with-answers`,
      answers
    );
    return unwrap(res);
  },

  getCorrections: async (quizId) => {
    const res = await axiosInstance.get(`/reponses/quiz/${quizId}/corrections`);
    return unwrap(res);
  },

  getResult: async (quizId) => {
    const res = await axiosInstance.get(`/resultats/quiz/${quizId}/my-resultat`);
    return unwrap(res);
  },

  getQuizHistory: async () => {
    const res = await axiosInstance.get("/student/quizzes/history");
    return unwrap(res);
  },

  getMyResultsHistory: async () => {
    const res = await axiosInstance.get("/resultats/my-history");
    return unwrap(res);
  },

  getMyPerformance: async () => {
    const res = await axiosInstance.get("/statistiques/student/my-performance");
    return unwrap(res);
  },

  getRanking: async (quizId) => {
    const res = await axiosInstance.get(`/statistiques/student/ranking/${quizId}`);
    return unwrap(res);
  },
};

export default studentQuizApi;
