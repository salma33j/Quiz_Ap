import axiosInstance from './axiosInstance';
const unwrap = (res) => res.data?.data ?? res.data;
export const getQuestionsByQuiz = async (quizId) => unwrap(await axiosInstance.get(`/teacher/quizzes/${quizId}/questions`));
export const createQuestion = async (quizId, payload) => unwrap(await axiosInstance.post(`/teacher/quizzes/${quizId}/questions`, payload));
export const updateQuestion = async (quizId, questionId, payload) => unwrap(await axiosInstance.put(`/teacher/quizzes/${quizId}/questions/${questionId}`, payload));
export const deleteQuestion = async (quizId, questionId) => unwrap(await axiosInstance.delete(`/teacher/quizzes/${quizId}/questions/${questionId}`));
