import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import PrivateRoute from './components/PrivateRoute.jsx'
import QuickCapture from './components/QuickCapture.jsx'
import ToastHost from './components/ToastHost.jsx'
import { AuthProvider, useAuth } from './store/AuthContext.jsx'

import ActivityPage from './pages/ActivityPage.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import EditorPage from './pages/EditorPage.jsx'
import GraphPage from './pages/GraphPage.jsx'
import LandingPage from './pages/LandingPage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import SharedNotePage from './pages/SharedNotePage.jsx'
import SignupPage from './pages/SignupPage.jsx'
import StatsPage from './pages/StatsPage.jsx'

// QuickCapture는 로그인된 사용자에게만 전역 활성화
function AppRoutes() {
  const { user } = useAuth()
  return (
    <>
      <ToastHost />
      {user && <QuickCapture />}
      <Routes>
        {/* 공개 */}
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/shared/:shareToken" element={<SharedNotePage />} />

        {/* 인증 필요 */}
        <Route element={<PrivateRoute />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/notes/new" element={<EditorPage />} />
          <Route path="/notes/:id" element={<EditorPage />} />
          <Route path="/graph" element={<GraphPage />} />
          <Route path="/activity" element={<ActivityPage />} />
          <Route path="/stats" element={<StatsPage />} />
          <Route path="/search" element={<Navigate to="/dashboard" replace />} />
        </Route>

        {/* 404 → 랜딩 */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </AuthProvider>
  )
}
