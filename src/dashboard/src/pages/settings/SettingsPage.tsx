import * as Switch from "@radix-ui/react-switch";
import * as Slider from "@radix-ui/react-slider";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/Card";
import { useSettingsStore } from "@/store/settingsStore";
import { useAuth } from "@/hooks/useAuth";
import { t } from "@/i18n";

export default function SettingsPage() {
  const {
    expertMode,
    acceptThreshold,
    reviewThreshold,
    notifyOnComplete,
    notifyOnError,
    setExpertMode,
    setAcceptThreshold,
    setReviewThreshold,
    setNotifyOnComplete,
    setNotifyOnError,
  } = useSettingsStore();
  const { user } = useAuth();

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-semibold">{t("settings.title")}</h1>

      <Card>
        <CardHeader>
          <CardTitle>{t("settings.thresholds")}</CardTitle>
          <CardDescription>
            Ajuster la sévérité de la validation automatique.
          </CardDescription>
        </CardHeader>
        <div className="flex flex-col gap-6">
          <ThresholdRow
            label={t("settings.acceptThreshold")}
            value={acceptThreshold}
            min={0.7}
            max={1}
            onChange={setAcceptThreshold}
          />
          <ThresholdRow
            label={t("settings.reviewThreshold")}
            value={reviewThreshold}
            min={0.5}
            max={0.95}
            onChange={setReviewThreshold}
          />
        </div>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>{t("settings.notifications")}</CardTitle>
        </CardHeader>
        <div className="flex flex-col gap-4">
          <ToggleRow
            label={t("settings.notifyOnComplete")}
            checked={notifyOnComplete}
            onChange={setNotifyOnComplete}
          />
          <ToggleRow
            label={t("settings.notifyOnError")}
            checked={notifyOnError}
            onChange={setNotifyOnError}
          />
        </div>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>{t("settings.account")}</CardTitle>
        </CardHeader>
        <div className="text-sm text-text-secondary">{user?.email ?? "—"}</div>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>{t("settings.expertMode")}</CardTitle>
          <CardDescription>{t("settings.expertModeHelp")}</CardDescription>
        </CardHeader>
        <ToggleRow
          label={t("settings.expertMode")}
          checked={expertMode}
          onChange={setExpertMode}
        />
      </Card>
    </div>
  );
}

function ThresholdRow({
  label,
  value,
  min,
  max,
  onChange,
}: {
  label: string;
  value: number;
  min: number;
  max: number;
  onChange: (v: number) => void;
}) {
  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-center justify-between">
        <span className="text-sm text-text-secondary">{label}</span>
        <span className="text-sm font-medium tabular-nums text-text-primary">
          {(value * 100).toFixed(0)}%
        </span>
      </div>
      <Slider.Root
        min={min}
        max={max}
        step={0.01}
        value={[value]}
        onValueChange={([v]) => v != null && onChange(v)}
        className="relative flex h-5 w-full touch-none select-none items-center"
      >
        <Slider.Track className="relative h-1 w-full grow rounded-full bg-bg-overlay">
          <Slider.Range className="absolute h-full rounded-full bg-accent" />
        </Slider.Track>
        <Slider.Thumb
          aria-label={label}
          className="block h-3.5 w-3.5 rounded-full border-2 border-accent bg-white focus:outline-none focus:ring-2 focus:ring-accent-ring"
        />
      </Slider.Root>
    </div>
  );
}

function ToggleRow({
  label,
  checked,
  onChange,
}: {
  label: string;
  checked: boolean;
  onChange: (v: boolean) => void;
}) {
  return (
    <label className="flex items-center justify-between gap-4">
      <span className="text-sm text-text-secondary">{label}</span>
      <Switch.Root
        checked={checked}
        onCheckedChange={onChange}
        className="relative h-5 w-9 cursor-pointer rounded-full bg-bg-overlay data-[state=checked]:bg-accent transition-colors"
      >
        <Switch.Thumb className="block h-4 w-4 translate-x-0.5 rounded-full bg-white transition-transform data-[state=checked]:translate-x-[18px]" />
      </Switch.Root>
    </label>
  );
}
