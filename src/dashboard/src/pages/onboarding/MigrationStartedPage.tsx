import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { Check } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Confetti } from "@/components/onboarding/Confetti";
import { useOnboardingStore } from "@/store/onboardingStore";
import { t } from "@/i18n";

export default function MigrationStartedPage() {
  const navigate = useNavigate();
  const { counters } = useOnboardingStore();

  useEffect(() => {
    const timer = setTimeout(() => navigate("/dashboard"), 5000);
    return () => clearTimeout(timer);
  }, [navigate]);

  return (
    <div className="relative flex min-h-screen items-center justify-center bg-bg-base px-4 text-center">
      <Confetti pieces={60} />
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.5, ease: [0.4, 0, 0.2, 1] }}
        className="relative z-10 flex flex-col items-center gap-6 max-w-md"
      >
        <motion.span
          initial={{ scale: 0 }}
          animate={{ scale: 1 }}
          transition={{ delay: 0.2, duration: 0.6, type: "spring", bounce: 0.3 }}
          className="flex h-16 w-16 items-center justify-center rounded-full bg-success-subtle text-success"
        >
          <Check className="h-8 w-8" strokeWidth={3} />
        </motion.span>
        <div className="space-y-2">
          <h1 className="text-3xl font-semibold tracking-tight text-text-primary">
            {t("onboarding.started.title")}
          </h1>
          <p className="text-text-secondary">
            {t("onboarding.started.subtitle", {
              count: counters.documents.toLocaleString("fr-FR"),
            })}
          </p>
        </div>
        <Button size="lg" onClick={() => navigate("/dashboard")}>
          {t("onboarding.started.view")}
        </Button>
        <p className="text-xs text-text-muted">{t("onboarding.started.safeToClose")}</p>
      </motion.div>
    </div>
  );
}
