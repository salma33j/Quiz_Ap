import axiosInstance from './axiosInstance';
const unwrap = (res) => res.data?.data ?? res.data;
export const getQuizResults = async (quizId) => unwrap(await axiosInstance.get(`/resultats/quiz/${quizId}`));
export const getQuizRanking = async (quizId) => unwrap(await axiosInstance.get(`/resultats/quiz/${quizId}/ranking`));
export const getQuizStatistics = async (quizId) => unwrap(await axiosInstance.get(`/resultats/quiz/${quizId}/statistics`));
