import { Navigate, Route, Routes } from "react-router-dom";
import { AppLayout } from "@/components/layout/AppLayout";
import { LoginPage } from "@/pages/LoginPage";
import { DashboardPage } from "@/pages/DashboardPage";
import { DocumentsPage } from "@/pages/DocumentsPage";
import { DocumentDetailPage } from "@/pages/DocumentDetailPage";
import { ReviewQueuePage } from "@/pages/ReviewQueuePage";
import { ReviewPage } from "@/pages/ReviewPage";
import { PipelinesPage } from "@/pages/PipelinesPage";
import { PlaceholderPage } from "@/pages/PlaceholderPage";
import { useAuthStore } from "@/store/authStore";

function RequireAuth({ children }: { children: JSX.Element }) {
  const token = useAuthStore((s) => s.accessToken);
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <RequireAuth>
            <AppLayout />
          </RequireAuth>
        }
      >
        <Route path="/" element={<DashboardPage />} />
        <Route path="/pipelines" element={<PipelinesPage />} />
        <Route path="/documents" element={<DocumentsPage />} />
        <Route path="/documents/:id" element={<DocumentDetailPage />} />
        <Route path="/review" element={<ReviewQueuePage />} />
        <Route path="/review/:id" element={<ReviewPage />} />
        <Route
          path="/fonts"
          element={<PlaceholderPage title="Fonts" description="Mapping explorer — coming with Module 4 wiring." />}
        />
        <Route
          path="/audit"
          element={<PlaceholderPage title="Audit" description="Admin-only — event log viewer." />}
        />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
