import { Routes, Route, Navigate } from "react-router-dom";

import Login from "../pages/auth/Login";
import RoleRoute from "./RoleRoute";

import DashboardLayout from "../components/layout/DashboardLayout";

import TeacherDashboard from "../pages/teacher/TeacherDashboard";
import MyQuizzes from "../pages/teacher/MyQuizzes";
import CreateQuiz from "../pages/teacher/CreateQuiz";
import QuizDetailsTeacher from "../pages/teacher/QuizDetailsTeacher";
import ManageQuestions from "../pages/teacher/ManageQuestions";
import AddQuestion from "../pages/teacher/AddQuestion";
import GenerateQuizAI from "../pages/teacher/GenerateQuizAI";
import QuizResults from "../pages/teacher/QuizResults";
import QuizRanking from "../pages/teacher/QuizRanking";
import QuizStatistics from "../pages/teacher/QuizStatistics";
import AssignStudents from "../pages/teacher/AssignStudents";

import StudentDashboard from "../pages/student/StudentDashboard";
import QuizDetailsStudent from "../pages/student/QuizDetailsStudent";
import TakeQuiz from "../pages/student/TakeQuiz";

import Profile from "../pages/profile/Profile";
import Unauthorized from "../pages/errors/Unauthorized";
import NotFound from "../pages/errors/NotFound";

export default function AppRoutes() {
  return (
    <Routes>
      {/* Auth */}
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<Login />} />

      {/* Errors */}
      <Route path="/unauthorized" element={<Unauthorized />} />

      {/* Teacher protected routes */}
      <Route element={<RoleRoute allowedRoles={["ENSEIGNANT", "TEACHER"]} />}>
        <Route path="/teacher" element={<DashboardLayout />}>
          <Route index element={<TeacherDashboard />} />
          <Route path="dashboard" element={<TeacherDashboard />} />

          {/* Quiz */}
          <Route path="quizzes" element={<MyQuizzes />} />
          <Route path="quizzes/create" element={<CreateQuiz />} />
          <Route path="quizzes/:id" element={<QuizDetailsTeacher />} />

          {/* Questions */}
          <Route path="quizzes/:id/questions" element={<ManageQuestions />} />
          <Route path="quizzes/:id/questions/add" element={<AddQuestion />} />

          {/* AI quiz generator */}
          <Route path="ai-generator" element={<GenerateQuizAI />} />

          {/* Students assignment */}
          <Route path="quizzes/:id/assign" element={<AssignStudents />} />
          <Route path="students" element={<AssignStudents />} />

          {/* Results */}
          <Route path="quizzes/:id/results" element={<QuizResults />} />
          <Route path="quizzes/:id/ranking" element={<QuizRanking />} />
          <Route path="quizzes/:id/statistics" element={<QuizStatistics />} />

          {/* Profile */}
          <Route path="profile" element={<Profile />} />
        </Route>
      </Route>

      {/* Student protected routes */}
      <Route element={<RoleRoute allowedRoles={["ETUDIANT", "STUDENT"]} />}>
        <Route path="/student">
          <Route index element={<StudentDashboard />} />
          <Route path="dashboard" element={<StudentDashboard />} />
          <Route path="quizzes/:id" element={<QuizDetailsStudent />} />
          <Route path="quizzes/:id/take" element={<TakeQuiz />} />
        </Route>
      </Route>

      {/* 404 */}
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
