import axiosInstance from "./axiosInstance";

const unwrap = (res) => res.data?.data ?? res.data;

const studentQuizApi = {
  getAvailableQuizzes: async () =>
    unwrap(await axiosInstance.get("/student/quizzes/available")),

  getQuizHistory: async () =>
    unwrap(await axiosInstance.get("/student/quizzes/history")),

  getQuizDetails: async (quizId) =>
    unwrap(await axiosInstance.get(`/student/quizzes/${quizId}`)),

  canParticipate: async (quizId) =>
    unwrap(await axiosInstance.get(`/student/quizzes/${quizId}/can-participate`)),

  startQuiz: async (quizId) =>
    unwrap(await axiosInstance.post(`/student/quizzes/${quizId}/start`)),

  getRemainingSeconds: async (quizId) =>
    unwrap(await axiosInstance.get(`/student/quizzes/${quizId}/remaining-seconds`)),

  getQuestions: async (quizId) =>
    unwrap(await axiosInstance.get(`/student/quizzes/${quizId}/questions`)),
  deleteQuestion: async (quizId, questionId) =>
  unwrap(
    await axiosInstance.delete(
      `/teacher/quizzes/${quizId}/questions/${questionId}`
    )
  ),

  submitQuiz: async (quizId, answers) =>
    unwrap(await axiosInstance.post(`/student/quizzes/${quizId}/submit`, { answers })),
};

export default studentQuizApi;