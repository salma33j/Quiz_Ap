import axiosInstance from './axiosInstance';
const unwrap = (res) => res.data?.data ?? res.data;
export const getUsers = async () => unwrap(await axiosInstance.get('/admin/users'));
export const createTeacher = async (payload) => unwrap(await axiosInstance.post('/admin/teachers', payload));
export const createStudent = async (payload) => unwrap(await axiosInstance.post('/admin/students', payload));
export const deleteUser = async (id) => unwrap(await axiosInstance.delete(`/admin/users/${id}`));
