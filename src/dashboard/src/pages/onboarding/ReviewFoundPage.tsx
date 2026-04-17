import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { FileText, Type, Layers } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { StepIndicator } from "@/components/onboarding/StepIndicator";
import { useOnboardingStore } from "@/store/onboardingStore";
import { useToastStore } from "@/store/toastStore";
import * as onboardingApi from "@/api/onboarding";
import { t } from "@/i18n";
import { cn } from "@/lib/utils";

function estimateMinutes(docs: number) {
  return Math.max(1, Math.round((docs * 2) / 60));
}

export default function ReviewFoundPage() {
  const navigate = useNavigate();
  const { counters, outputFormat, quality, setOutputFormat, setQuality, connectionId } =
    useOnboardingStore();
  const pushToast = useToastStore((s) => s.push);
  const [launching, setLaunching] = useState(false);

  const launch = async () => {
    if (!connectionId) return;
    setLaunching(true);
    try {
      await onboardingApi.startMigration(connectionId);
      navigate("/onboarding/started");
    } catch {
      pushToast({ title: t("errors.unknown"), variant: "danger" });
    } finally {
      setLaunching(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-bg-base px-4 py-12">
      <div className="w-full max-w-xl space-y-8">
        <div className="space-y-2 text-center">
          <p className="text-xs uppercase tracking-widest text-text-muted">
            {t("onboarding.review.step")}
          </p>
          <h1 className="text-2xl font-semibold text-text-primary">Voici ce qu'on a trouvé</h1>
        </div>

        <Card className="flex flex-col gap-5" padding="lg">
          <Row icon={<FileText className="h-5 w-5" />} label={`${counters.documents.toLocaleString("fr-FR")} documents`}>
            Relevés, factures, avis — {(counters.bytes / 1_073_741_824).toFixed(1)} GB
          </Row>
          <Row icon={<Type className="h-5 w-5" />} label={`${counters.fonts} polices`}>
            {counters.fonts - 2} reconnues · 2 à analyser
          </Row>
          <Row icon={<Layers className="h-5 w-5" />} label={`${counters.templates} modèles de pages`}>
            En-têtes, pieds de page, logos
          </Row>
        </Card>

        <p className="text-center text-sm text-text-secondary">
          {t("onboarding.review.estimate", { minutes: estimateMinutes(counters.documents) })}
        </p>

        <Card padding="md" className="flex flex-col gap-4">
          <Selector
            label={t("onboarding.review.outputFormat")}
            options={[
              { value: "pdf", label: "PDF" },
              { value: "pdfa", label: "PDF/A (archivage)" },
            ]}
            value={outputFormat}
            onChange={(v) => setOutputFormat(v as "pdf" | "pdfa")}
          />
          <Selector
            label={t("onboarding.review.quality")}
            options={[
              { value: "standard", label: "Standard" },
              { value: "high", label: "Haute" },
            ]}
            value={quality}
            onChange={(v) => setQuality(v as "standard" | "high")}
          />
        </Card>

        <div className="flex flex-col items-center gap-4">
          <Button size="lg" loading={launching} onClick={launch} className="min-w-[240px]">
            {t("onboarding.review.launch")}
          </Button>
          <StepIndicator current={3} total={5} label="Étape 4 sur 5" />
        </div>
      </div>
    </div>
  );
}

function Row({
  icon,
  label,
  children,
}: {
  icon: React.ReactNode;
  label: string;
  children: React.ReactNode;
}) {
  return (
    <div className="flex items-start gap-3">
      <span className="mt-0.5 text-accent">{icon}</span>
      <div>
        <div className="text-base font-medium text-text-primary">{label}</div>
        <div className="text-sm text-text-secondary">{children}</div>
      </div>
    </div>
  );
}

function Selector<T extends string>({
  label,
  options,
  value,
  onChange,
}: {
  label: string;
  options: { value: T; label: string }[];
  value: T;
  onChange: (v: T) => void;
}) {
  return (
    <div className="flex items-center justify-between gap-4">
      <span className="text-sm text-text-secondary">{label}</span>
      <div className="inline-flex rounded-full border border-border bg-bg-elevated p-0.5">
        {options.map((o) => (
          <button
            key={o.value}
            type="button"
            onClick={() => onChange(o.value)}
            className={cn(
              "rounded-full px-3 py-1 text-xs transition-colors",
              value === o.value ? "bg-accent text-white" : "text-text-secondary hover:text-text-primary"
            )}
          >
            {o.label}
          </button>
        ))}
      </div>
    </div>
  );
}
