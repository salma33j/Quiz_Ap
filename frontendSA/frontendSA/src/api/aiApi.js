import axiosInstance from './axiosInstance';
const unwrap = (res) => res.data?.data ?? res.data;
export const generateQuizWithAI = async (payload) => unwrap(await axiosInstance.post('/ai/generate-quiz', payload));
export const generateFeedbackWithAI = async (payload) => unwrap(await axiosInstance.post('/ai/feedback', payload));
