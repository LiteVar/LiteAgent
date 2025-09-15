import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router";
import AppLayout from "./layout/AppLayout";
import { ScrollToTop } from "./components/common/ScrollToTop";
import RobotManagement from "./pages/RobotManagement";
// import UserManagement from "./pages/UserManagement";
import SystemInfo from "./pages/SystemInfo";
import NotFound from "./pages/OtherPage/NotFound";
import SingIn from "./pages/SingIn";
import { AuthProvider } from "./context/AuthContext";
import { ThemeProvider } from "./context/ThemeContext";

export default function App() {
  return (
    <AuthProvider>
      <ThemeProvider>
        <Router>
          <ScrollToTop />
          <Routes>
            <Route element={<AppLayout />}>
              <Route path="/" element={<Navigate to="/robots" replace />} />
              <Route path="/robots" element={<RobotManagement />} />
              {/* <Route path="/users" element={<UserManagement />} /> */}
              <Route path="/system" element={<SystemInfo />} />
            </Route>

            {/* Auth Layout */}
            <Route path="/signin" element={<SingIn />} />

            {/* Fallback Route */}
            <Route path="*" element={<NotFound />} />
          </Routes>
        </Router>
      </ThemeProvider>
    </AuthProvider>
  );
}
