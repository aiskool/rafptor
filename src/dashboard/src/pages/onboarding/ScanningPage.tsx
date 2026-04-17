import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import { ArrowRight, FileText, Type, Layers, HardDrive, Timer } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Counter } from "@/components/ui/Counter";
import { PulseAnimation } from "@/components/onboarding/PulseAnimation";
import { StepIndicator } from "@/components/onboarding/StepIndicator";
import { useOnboardingStore } from "@/store/onboardingStore";
import { t } from "@/i18n";

function formatBytes(n: number) {
  if (n < 1024) return `${n} B`;
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`;
  if (n < 1024 * 1024 * 1024) return `${(n / (1024 * 1024)).toFixed(1)} MB`;
  return `${(n / (1024 * 1024 * 1024)).toFixed(1)} GB`;
}

function formatDuration(ms: number) {
  const sec = Math.floor(ms / 1000);
  const mm = String(Math.floor(sec / 60)).padStart(2, "0");
  const ss = String(sec % 60).padStart(2, "0");
  return `${mm}:${ss}`;
}

export default function ScanningPage() {
  const navigate = useNavigate();
  const { counters, scanCompleted, updateCounters, setScanCompleted } = useOnboardingStore();
  const [visibleKeys, setVisibleKeys] = useState<string[]>([]);

  useEffect(() => {
    // Demo/simulation when no WebSocket is available; replaced by real WS in production.
    const schedule = [
      { delay: 600, key: "documents", val: 1247 },
      { delay: 1400, key: "fonts", val: 23 },
      { delay: 2200, key: "templates", val: 8 },
      { delay: 3000, key: "bytes", val: 4_200_000_000 },
      { delay: 3800, key: "duration", val: 47_000 },
    ];
    const timers = schedule.map(({ delay, key, val }) =>
      setTimeout(() => {
        setVisibleKeys((v) => [...v, key]);
        updateCounters(
          key === "duration"
            ? { durationMs: val }
            : key === "bytes"
              ? { bytes: val }
              : key === "documents"
                ? { documents: val }
                : key === "fonts"
                  ? { fonts: val }
                  : { templates: val }
        );
        if (key === "duration") setScanCompleted(true);
      }, delay)
    );
    return () => timers.forEach(clearTimeout);
  }, [updateCounters, setScanCompleted]);

  const rows = [
    {
      key: "documents",
      icon: <FileText className="h-4 w-4" />,
      label: t("onboarding.scanning.counters.documents"),
      value: <Counter value={counters.documents} />,
    },
    {
      key: "fonts",
      icon: <Type className="h-4 w-4" />,
      label: t("onboarding.scanning.counters.fonts"),
      value: <Counter value={counters.fonts} />,
    },
    {
      key: "templates",
      icon: <Layers className="h-4 w-4" />,
      label: t("onboarding.scanning.counters.templates"),
      value: <Counter value={counters.templates} />,
    },
    {
      key: "bytes",
      icon: <HardDrive className="h-4 w-4" />,
      label: t("onboarding.scanning.counters.bytes"),
      value: formatBytes(counters.bytes),
    },
    {
      key: "duration",
      icon: <Timer className="h-4 w-4" />,
      label: t("onboarding.scanning.counters.duration"),
      value: formatDuration(counters.durationMs ?? 0),
    },
  ];

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-bg-base px-4 py-12">
      <div className="w-full max-w-xl space-y-10">
        <div className="space-y-2 text-center">
          <p className="text-xs uppercase tracking-widest text-text-muted">
            {t("onboarding.scanning.step")}
          </p>
          <h1 className="text-2xl font-semibold text-text-primary">
            {t("onboarding.scanning.subtitle")}
          </h1>
        </div>
        <div className="flex justify-center">
          <PulseAnimation size={140} />
        </div>
        <div className="rounded-lg border border-border bg-bg-raised">
          <AnimatePresence initial={false}>
            {rows
              .filter((r) => visibleKeys.includes(r.key))
              .map((r) => (
                <motion.div
                  key={r.key}
                  initial={{ opacity: 0, x: -12 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0 }}
                  transition={{ duration: 0.3, ease: [0.4, 0, 0.2, 1] }}
                  className="flex items-center justify-between border-b border-border px-4 py-3 last:border-b-0"
                >
                  <span className="flex items-center gap-2 text-sm text-text-secondary">
                    {r.icon}
                    {r.label}
                  </span>
                  <span className="tabular-nums text-sm font-medium text-text-primary">
                    {r.value}
                  </span>
                </motion.div>
              ))}
          </AnimatePresence>
        </div>
        <div className="flex flex-col items-center gap-4">
          <Button
            size="lg"
            rightIcon={<ArrowRight className="h-4 w-4" />}
            disabled={!scanCompleted}
            onClick={() => navigate("/onboarding/review")}
          >
            Continuer
          </Button>
          <StepIndicator current={2} total={5} label="Étape 3 sur 5" />
        </div>
      </div>
    </div>
  );
}
