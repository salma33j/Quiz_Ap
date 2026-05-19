import { Navigate, Outlet } from 'react-router-dom';
import useAuth from '../hooks/useAuth';

export default function RoleRoute({ allowedRoles = [] }) {
  const { user, isAuthenticated } = useAuth();

  if (!isAuthenticated) return <Navigate to="/login" replace />;

  const role = user?.role || user?.data?.role || user?.user?.role;

  if (allowedRoles.length && !allowedRoles.includes(role))
    return <Navigate to="/unauthorized" replace />;

  return <Outlet />;
}
