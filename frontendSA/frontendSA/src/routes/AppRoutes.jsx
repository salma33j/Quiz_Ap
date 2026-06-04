import { Routes, Route, Navigate } from "react-router-dom";

import Login from "../pages/auth/Login";
import RoleRoute from "./RoleRoute";

import DashboardLayout from "../components/layout/DashboardLayout";
import AdminLayout from "../components/layout/AdminLayout";
import StudentLayout from "../components/layout/StudentLayout";
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
import AdminWorkspace from "../pages/admin/AdminWorkspace";

import StudentDashboard from "../pages/student/StudentDashboard";
import AvailableQuizzes from "../pages/student/AvailableQuizzes";
import MyHistory from "../pages/student/MyHistory";
import MyPerformance from "../pages/student/MyPerformance";
import QuizCorrections from "../pages/student/QuizCorrections";
import QuizDetailsStudent from "../pages/student/QuizDetailsStudent";
import QuizResult from "../pages/student/QuizResult";
import TakeQuiz from "../pages/student/TakeQuiz";

import Profile from "../pages/profile/Profile";
import Unauthorized from "../pages/errors/Unauthorized";
import NotFound from "../pages/errors/NotFound";

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<Login />} />
      <Route path="/unauthorized" element={<Unauthorized />} />

      <Route element={<RoleRoute allowedRoles={["ENSEIGNANT", "TEACHER"]} />}>
        <Route path="/teacher" element={<DashboardLayout />}>
          <Route index element={<TeacherDashboard />} />
          <Route path="dashboard" element={<TeacherDashboard />} />
          <Route path="quizzes" element={<MyQuizzes />} />
          <Route path="quizzes/create" element={<CreateQuiz />} />
          <Route path="quizzes/:id" element={<QuizDetailsTeacher />} />
          <Route path="quizzes/:id/questions" element={<ManageQuestions />} />
          <Route path="quizzes/:id/questions/add" element={<AddQuestion />} />
          <Route path="ai-generator" element={<GenerateQuizAI />} />
          <Route path="quizzes/:id/assign" element={<AssignStudents />} />
          <Route path="students" element={<AssignStudents />} />
          <Route path="ranking" element={<QuizRanking />} />
          <Route path="results" element={<QuizResults />} />
          <Route path="statistics" element={<QuizStatistics />} />
          <Route path="quizzes/:id/results" element={<QuizResults />} />
          <Route path="quizzes/:id/ranking" element={<QuizRanking />} />
          <Route path="quizzes/:id/statistics" element={<QuizStatistics />} />
          <Route path="profile" element={<Profile />} />
        </Route>
      </Route>

      <Route element={<RoleRoute allowedRoles={["ADMIN"]} />}>
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<AdminWorkspace section="dashboard" />} />
          <Route path="dashboard" element={<AdminWorkspace section="dashboard" />} />
          <Route path="classes" element={<AdminWorkspace section="classes" />} />
          <Route path="users" element={<AdminWorkspace section="users" />} />
          <Route path="subjects" element={<AdminWorkspace section="subjects" />} />
          <Route path="quizzes" element={<AdminWorkspace section="quizzes" />} />
          <Route path="results" element={<AdminWorkspace section="results" />} />
          <Route path="emails" element={<AdminWorkspace section="emails" />} />
          <Route path="profile" element={<Profile />} />
        </Route>
      </Route>

      <Route element={<RoleRoute allowedRoles={["ETUDIANT", "STUDENT"]} />}>
        <Route path="/student" element={<StudentLayout />}>
          <Route index element={<StudentDashboard />} />
          <Route path="dashboard" element={<StudentDashboard />} />
          <Route path="available-quizzes" element={<AvailableQuizzes />} />
          <Route path="quizzes" element={<AvailableQuizzes />} />
          <Route path="history" element={<MyHistory />} />
          <Route path="performance" element={<MyPerformance />} />
          <Route path="corrections" element={<QuizCorrections />} />
          <Route path="corrections/:quizId" element={<QuizCorrections />} />
          <Route path="quizzes/:id" element={<QuizDetailsStudent />} />
          <Route path="quizzes/:id/take" element={<TakeQuiz />} />
          <Route path="quizzes/:id/result" element={<QuizResult />} />
          <Route path="profile" element={<Profile />} />
        </Route>
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
