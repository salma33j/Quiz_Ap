import axiosInstance from "./axiosInstance";

const unwrap = (res) => res.data?.data ?? res.data;

const multipart = (file) => {
  const formData = new FormData();
  formData.append("file", file);
  return formData;
};

const adminApi = {
  getUsers: () => axiosInstance.get("/auth/users").then(unwrap),
  createStudent: (payload) =>
    axiosInstance.post("/auth/admin/create-etudiant", payload).then(unwrap),
  createTeacher: (payload) =>
    axiosInstance.post("/auth/admin/create-enseignant", payload).then(unwrap),
  createAdmin: (payload) =>
    axiosInstance.post("/auth/admin/create-admin", payload).then(unwrap),
  updateUser: (id, payload) =>
    axiosInstance.put(`/auth/profile/${id}`, payload).then(unwrap),
  deleteUser: (id) => axiosInstance.delete(`/auth/user/${id}`).then(unwrap),
  blockUser: (id) =>
    axiosInstance.post(`/auth/admin/users/${id}/block`).then(unwrap),
  unblockUser: (id) =>
    axiosInstance.post(`/auth/admin/users/${id}/unblock`).then(unwrap),
  resetPassword: (id) =>
    axiosInstance.post(`/auth/admin/users/${id}/reset-password`).then(unwrap),

  getClasses: () => axiosInstance.get("/teacher/classes/all").then(unwrap),
  createClass: (payload) =>
    axiosInstance.post("/teacher/classes", payload).then(unwrap),
  updateClass: (id, payload) =>
    axiosInstance.put(`/teacher/classes/${id}`, payload).then(unwrap),
  deleteClass: (id) => axiosInstance.delete(`/teacher/classes/${id}`).then(unwrap),
  assignTeacherToClass: (classId, teacherIds) =>
    axiosInstance
      .post(`/teacher/classes/${classId}/teacher`, { teacherIds })
      .then(unwrap),
  importStudentsToClass: (classId, file) =>
    axiosInstance
      .post(`/teacher/classes/${classId}/students/import`, multipart(file), {
        headers: { "Content-Type": "multipart/form-data" },
      })
      .then(unwrap),

  getSubjects: () => axiosInstance.get("/teacher/matieres").then(unwrap),
  createSubject: (payload) =>
    axiosInstance.post("/teacher/matieres", payload).then(unwrap),
  updateSubject: (id, payload) =>
    axiosInstance.put(`/teacher/matieres/${id}`, payload).then(unwrap),
  deleteSubject: (id) =>
    axiosInstance.delete(`/teacher/matieres/${id}`).then(unwrap),

  getQuizzes: () => axiosInstance.get("/auth/admin/quizzes").then(unwrap),
  deleteQuiz: (id) =>
    axiosInstance.delete(`/auth/admin/quiz/${id}/permanent`).then(unwrap),
  softDeleteQuiz: (id) =>
    axiosInstance.put(`/auth/admin/quiz/${id}/soft-delete`).then(unwrap),

  getGlobalStats: () => axiosInstance.get("/statistiques/admin/global").then(unwrap),
  getAllStatistics: () => axiosInstance.get("/statistiques/admin/all").then(unwrap),
  getQuizStatistics: (quizId) =>
    axiosInstance.get(`/statistiques/quiz/${quizId}`).then(unwrap),
  getQuizRanking: (quizId) =>
    axiosInstance.get(`/resultats/quiz/${quizId}/ranking`).then(unwrap),
  getQuizResults: (quizId) =>
    axiosInstance.get(`/resultats/quiz/${quizId}`).then(unwrap),

  sendEmail: (payload) =>
    axiosInstance.post("/auth/admin/emails/send", payload).then(unwrap),
  importUsersExcel: (role, file) =>
    axiosInstance
      .post(`/auth/admin/import-users/${role}`, multipart(file), {
        headers: { "Content-Type": "multipart/form-data" },
      })
      .then(unwrap),
};

export const {
  getUsers,
  createStudent,
  createTeacher,
  createAdmin,
  updateUser,
  deleteUser,
  blockUser,
  unblockUser,
  resetPassword,
  getClasses,
  createClass,
  updateClass,
  deleteClass,
  assignTeacherToClass,
  importStudentsToClass,
  getSubjects,
  createSubject,
  updateSubject,
  deleteSubject,
  getQuizzes,
  deleteQuiz,
  softDeleteQuiz,
  getGlobalStats,
  getAllStatistics,
  getQuizStatistics,
  getQuizRanking,
  getQuizResults,
  sendEmail,
  importUsersExcel,
} = adminApi;

export default adminApi;
