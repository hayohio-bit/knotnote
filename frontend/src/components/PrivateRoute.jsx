import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../store/AuthContext.jsx'

export default function PrivateRoute() {
  const { user } = useAuth()
  return user ? <Outlet /> : <Navigate to="/login" replace />
}
