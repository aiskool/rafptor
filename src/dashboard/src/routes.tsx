import { lazy, Suspense } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { AppLayout } from "@/components/layout/AppLayout";
import { useAuthStore } from "@/store/authStore";
import { Spinner } from "@/components/ui/Spinner";

const LoginPage = lazy(() => import("@/pages/auth/LoginPage"));

const WelcomePage = lazy(() => import("@/pages/onboarding/WelcomePage"));
const ConnectSystemPage = lazy(() => import("@/pages/onboarding/ConnectSystemPage"));
const ScanningPage = lazy(() => import("@/pages/onboarding/ScanningPage"));
const ReviewFoundPage = lazy(() => import("@/pages/onboarding/ReviewFoundPage"));
const MigrationStartedPage = lazy(() => import("@/pages/onboarding/MigrationStartedPage"));

const OverviewPage = lazy(() => import("@/pages/dashboard/OverviewPage"));
const DocumentsPage = lazy(() => import("@/pages/documents/DocumentsPage"));
const DocumentViewPage = lazy(() => import("@/pages/documents/DocumentViewPage"));
const ReviewQueuePage = lazy(() => import("@/pages/review/ReviewQueuePage"));
const ReviewComparePage = lazy(() => import("@/pages/review/ReviewComparePage"));
const SettingsPage = lazy(() => import("@/pages/settings/SettingsPage"));

function RequireAuth({ children }: { children: JSX.Element }) {
  const token = useAuthStore((s) => s.accessToken);
  if (!token) return <Navigate to="/login" replace />;
  return children;
}

function Fallback() {
  return (
    <div className="flex min-h-[60vh] items-center justify-center">
      <Spinner />
    </div>
  );
}

export function AppRoutes() {
  return (
    <Suspense fallback={<Fallback />}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />

        <Route
          path="/onboarding/welcome"
          element={
            <RequireAuth>
              <WelcomePage />
            </RequireAuth>
          }
        />
        <Route
          path="/onboarding/connect"
          element={
            <RequireAuth>
              <ConnectSystemPage />
            </RequireAuth>
          }
        />
        <Route
          path="/onboarding/scanning"
          element={
            <RequireAuth>
              <ScanningPage />
            </RequireAuth>
          }
        />
        <Route
          path="/onboarding/review"
          element={
            <RequireAuth>
              <ReviewFoundPage />
            </RequireAuth>
          }
        />
        <Route
          path="/onboarding/started"
          element={
            <RequireAuth>
              <MigrationStartedPage />
            </RequireAuth>
          }
        />

        <Route
          element={
            <RequireAuth>
              <AppLayout />
            </RequireAuth>
          }
        >
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<OverviewPage />} />
          <Route path="/documents" element={<DocumentsPage />} />
          <Route path="/documents/:id" element={<DocumentViewPage />} />
          <Route path="/review" element={<ReviewQueuePage />} />
          <Route path="/review/:id" element={<ReviewComparePage />} />
          <Route path="/settings" element={<SettingsPage />} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  );
}
