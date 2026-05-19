import axiosInstance from './axiosInstance';

const unwrap = (res) => res.data?.data ?? res.data;

export const teacherQuizApi = {
  getDashboard: async () => unwrap(await axiosInstance.get('/statistiques/teacher/dashboard')),
  generateQuestionsAI: async (payload) => unwrap(await axiosInstance.post('/ai/generate-quiz', payload)),
  getMyQuizzes: async () => unwrap(await axiosInstance.get('/teacher/quizzes')),
  publishQuiz: async (id) => unwrap(await axiosInstance.post(`/teacher/quizzes/${id}/publish`)),
  getQuizById: async (id) => unwrap(await axiosInstance.get(`/teacher/quizzes/${id}`)),
  createQuiz: async (payload) => unwrap(await axiosInstance.post('/teacher/quizzes', payload)),
  updateQuiz: async (id, payload) => unwrap(await axiosInstance.put(`/teacher/quizzes/${id}`, payload)),
  deleteQuiz: async (id) => unwrap(await axiosInstance.delete(`/teacher/quizzes/${id}`)),
  getQuestions: async (quizId) => unwrap(await axiosInstance.get(`/teacher/quizzes/${quizId}/questions`)),
  addQuestion: async (quizId, payload) => unwrap(await axiosInstance.post(`/teacher/quizzes/${quizId}/questions`, payload)),
  updateQuestion: async (quizId, questionId, payload) => unwrap(await axiosInstance.put(`/teacher/quizzes/${quizId}/questions/${questionId}`, payload)),
  deleteQuestion: async (quizId, questionId) => unwrap(await axiosInstance.delete(`/teacher/questions/${questionId}`)),
  getResults: async (quizId) => unwrap(await axiosInstance.get(`/resultats/quiz/${quizId}`)),
  getStatistics: async (quizId) => unwrap(await axiosInstance.get(`/resultats/quiz/${quizId}/statistics`)),
  getRanking: async (quizId) => unwrap(await axiosInstance.get(`/resultats/quiz/${quizId}/ranking`)),
  assignStudents: async (quizId, studentIds) => unwrap(await axiosInstance.post(`/teacher/quizzes/${quizId}/assign-students`, { studentIds })),
  getStudents: async () => unwrap(await axiosInstance.get('/teacher/students')),
  getQuizStudents: async (quizId) => unwrap(await axiosInstance.get(`/teacher/quizzes/${quizId}/students`)),
  getClasses: async () =>
  unwrap(await axiosInstance.get("/teacher/classes")),

createClass: async (payload) =>
  unwrap(await axiosInstance.post("/teacher/classes", payload)),

deleteClass: async (classId) =>
  unwrap(await axiosInstance.delete(`/teacher/classes/${classId}`)),

getClassStudents: async (classId) =>
  unwrap(await axiosInstance.get(`/teacher/classes/${classId}/students`)),

addStudentToClass: async (classId, payload) =>
  unwrap(await axiosInstance.post(`/teacher/classes/${classId}/students`, payload)),

deleteStudentFromClass: async (classId, studentId) =>
  unwrap(await axiosInstance.delete(`/teacher/classes/${classId}/students/${studentId}`)),

importStudentsToClass: async (classId, file) => {
  const formData = new FormData();
  formData.append("file", file);

  return unwrap(
    await axiosInstance.post(`/teacher/classes/${classId}/students/import`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
    })
  );
},

assignQuizToClass: async (quizId, classId) =>
  unwrap(await axiosInstance.post(`/teacher/quizzes/${quizId}/assign-class`, { classId })),
};

export default teacherQuizApi;
