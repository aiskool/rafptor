import { motion } from "framer-motion";
import { useNavigate } from "react-router-dom";
import { ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { StepIndicator } from "@/components/onboarding/StepIndicator";
import { t } from "@/i18n";

export default function WelcomePage() {
  const navigate = useNavigate();
  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-bg-base text-center">
      <ParticleBackdrop />
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6, ease: [0.4, 0, 0.2, 1] }}
        className="relative z-10 flex flex-col items-center gap-10 px-6"
      >
        <div className="space-y-3">
          <h1 className="text-4xl font-semibold tracking-tight sm:text-5xl">
            {t("onboarding.welcome.title")}
          </h1>
          <p className="max-w-md text-lg text-text-secondary">
            {t("onboarding.welcome.subtitle")}
          </p>
        </div>
        <Button
          size="lg"
          rightIcon={<ArrowRight className="h-4 w-4" />}
          onClick={() => navigate("/onboarding/connect")}
          className="min-w-[220px]"
        >
          {t("onboarding.welcome.cta")}
        </Button>
        <StepIndicator current={0} total={5} label={`Étape 1 sur 5`} />
      </motion.div>
    </div>
  );
}

function ParticleBackdrop() {
  return (
    <div className="absolute inset-0 opacity-60">
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,rgba(99,102,241,0.08),transparent_70%)]" />
      <svg className="absolute inset-0 h-full w-full">
        <defs>
          <radialGradient id="spot" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stopColor="rgba(99,102,241,0.4)" />
            <stop offset="100%" stopColor="rgba(99,102,241,0)" />
          </radialGradient>
        </defs>
        {Array.from({ length: 30 }).map((_, i) => {
          const cx = (i * 137) % 100;
          const cy = (i * 53) % 100;
          const r = 1 + (i % 3);
          return (
            <circle
              key={i}
              cx={`${cx}%`}
              cy={`${cy}%`}
              r={r}
              fill="rgba(255,255,255,0.15)"
            >
              <animate
                attributeName="opacity"
                values="0.15;0.5;0.15"
                dur={`${4 + (i % 5)}s`}
                repeatCount="indefinite"
              />
            </circle>
          );
        })}
      </svg>
    </div>
  );
}
