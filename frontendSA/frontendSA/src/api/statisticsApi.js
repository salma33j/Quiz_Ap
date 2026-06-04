import axiosInstance from './axiosInstance';
const unwrap = (res) => res.data?.data ?? res.data;
export const getTeacherDashboardStats = async () => unwrap(await axiosInstance.get('/statistiques/teacher/dashboard'));
