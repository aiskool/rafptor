import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { StepIndicator } from "@/components/onboarding/StepIndicator";
import { CredentialForm, type CredentialFormValues } from "@/components/onboarding/CredentialForm";
import { useOnboardingStore } from "@/store/onboardingStore";
import { useToastStore } from "@/store/toastStore";
import * as onboardingApi from "@/api/onboarding";
import { t } from "@/i18n";

export default function ConnectSystemPage() {
  const navigate = useNavigate();
  const setConnection = useOnboardingStore((s) => s.setConnection);
  const pushToast = useToastStore((s) => s.push);
  const [loading, setLoading] = useState(false);

  const onSubmit = async (values: CredentialFormValues) => {
    setLoading(true);
    try {
      const credential = values.credentialType === "password" ? values.password! : values.sshKey!;
      const res = await onboardingApi.connect({
        hostname: values.hostname,
        port: values.port,
        username: values.username,
        credential,
        credentialType: values.credentialType,
        systemType: values.systemType,
      });
      setConnection(res.connectionId, values.hostname, values.systemType, values.credentialType);
      navigate("/onboarding/scanning");
    } catch {
      pushToast({
        title: t("onboarding.connect.error"),
        description: t("errors.connection"),
        variant: "danger",
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-bg-base px-4 py-12">
      <div className="w-full max-w-xl space-y-8">
        <div className="space-y-2 text-center">
          <p className="text-xs uppercase tracking-widest text-text-muted">
            {t("onboarding.connect.step")}
          </p>
          <h1 className="text-2xl font-semibold text-text-primary">Connecter votre système</h1>
          <p className="text-sm text-text-secondary">
            {t("onboarding.connect.description")}
          </p>
        </div>
        <CredentialForm loading={loading} onSubmit={onSubmit} />
        <StepIndicator current={1} total={5} label="Étape 2 sur 5" />
      </div>
    </div>
  );
}
