import { FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { ArrowRight, Lock, Mail } from "lucide-react";
import { useAuth } from "@/hooks/useAuth";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { useToastStore } from "@/store/toastStore";
import { t } from "@/i18n";

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const pushToast = useToastStore((s) => s.push);
  const [tenant, setTenant] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    try {
      await login(email, password, tenant);
      navigate("/");
    } catch {
      pushToast({ title: t("errors.auth"), variant: "danger" });
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-bg-base px-4">
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, ease: [0.4, 0, 0.2, 1] }}
        className="w-full max-w-md space-y-10"
      >
        <div className="text-center">
          <span className="text-3xl font-bold tracking-tight">
            <span className="text-text-primary">raf</span>
            <span className="text-accent">ptor</span>
          </span>
          <p className="mt-3 text-text-secondary">{t("auth.login.title")}</p>
        </div>

        <form onSubmit={onSubmit} className="flex flex-col gap-4">
          <Input
            label={t("auth.login.tenantHint")}
            value={tenant}
            onChange={(e) => setTenant(e.target.value)}
            required
            autoComplete="organization"
          />
          <Input
            label={t("auth.login.email")}
            type="email"
            prefix={<Mail className="h-4 w-4" />}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoComplete="email"
          />
          <Input
            label={t("auth.login.password")}
            type="password"
            prefix={<Lock className="h-4 w-4" />}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            autoComplete="current-password"
          />
          <Button
            size="lg"
            type="submit"
            loading={busy}
            rightIcon={<ArrowRight className="h-4 w-4" />}
          >
            {t("auth.login.submit")}
          </Button>
          <a href="#" className="text-center text-xs text-text-muted hover:text-text-secondary">
            {t("auth.login.forgot")}
          </a>
        </form>

        <figure className="border-t border-border pt-8 text-center text-xs text-text-muted">
          <blockquote>{t("auth.login.testimonial")}</blockquote>
        </figure>
      </motion.div>
    </div>
  );
}
