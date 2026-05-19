import axiosInstance from './axiosInstance';
const unwrap = (res) => res.data?.data ?? res.data;
export const submitResponse = async (payload) => unwrap(await axiosInstance.post('/responses', payload));
export const getResponsesByResult = async (resultId) => unwrap(await axiosInstance.get(`/responses/result/${resultId}`));
