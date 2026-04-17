import { useState, type FormEvent } from "react";
import { KeyRound, Lock, Server, User, Upload } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Card } from "@/components/ui/Card";
import { cn } from "@/lib/utils";

export type SystemType = "ibmi" | "zos";
export type CredentialType = "password" | "ssh_key";

export interface CredentialFormValues {
  systemType: SystemType;
  hostname: string;
  port: number;
  username: string;
  credentialType: CredentialType;
  password?: string;
  sshKey?: string;
}

export interface CredentialFormProps {
  loading?: boolean;
  onSubmit: (values: CredentialFormValues) => void;
}

export function CredentialForm({ loading, onSubmit }: CredentialFormProps) {
  const [systemType, setSystemType] = useState<SystemType>("ibmi");
  const [credentialType, setCredentialType] = useState<CredentialType>("password");
  const [hostname, setHostname] = useState("");
  const [port, setPort] = useState(22);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [sshKey, setSshKey] = useState("");

  const submit = (e: FormEvent) => {
    e.preventDefault();
    onSubmit({
      systemType,
      hostname: hostname.trim(),
      port,
      username: username.trim(),
      credentialType,
      password: credentialType === "password" ? password : undefined,
      sshKey: credentialType === "ssh_key" ? sshKey : undefined,
    });
  };

  const onFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) setSshKey(await file.text());
  };

  return (
    <form onSubmit={submit} className="flex flex-col gap-6">
      <fieldset className="flex flex-col gap-2">
        <legend className="text-sm font-medium text-text-secondary mb-1">Votre système</legend>
        <div className="grid grid-cols-2 gap-3">
          <SystemButton
            selected={systemType === "ibmi"}
            onClick={() => setSystemType("ibmi")}
            label="IBM i"
            sub="AS/400"
          />
          <SystemButton
            selected={systemType === "zos"}
            onClick={() => setSystemType("zos")}
            label="z/OS"
            sub="Mainframe"
          />
        </div>
      </fieldset>

      <div className="grid grid-cols-[1fr_auto] gap-3">
        <Input
          label="Adresse du serveur"
          placeholder="mainframe.mabanque.fr"
          prefix={<Server className="h-4 w-4" />}
          value={hostname}
          onChange={(e) => setHostname(e.target.value)}
          required
          autoComplete="off"
        />
        <Input
          label="Port"
          type="number"
          value={port}
          onChange={(e) => setPort(Number(e.target.value) || 22)}
          min={1}
          max={65_535}
          className="w-24"
        />
      </div>

      <Input
        label="Identifiant"
        prefix={<User className="h-4 w-4" />}
        value={username}
        onChange={(e) => setUsername(e.target.value)}
        required
        autoComplete="off"
      />

      <div className="flex flex-col gap-3">
        <div className="flex gap-2 text-xs">
          <MiniToggle active={credentialType === "password"} onClick={() => setCredentialType("password")}>
            Mot de passe
          </MiniToggle>
          <MiniToggle active={credentialType === "ssh_key"} onClick={() => setCredentialType("ssh_key")}>
            Clé SSH
          </MiniToggle>
        </div>
        {credentialType === "password" ? (
          <Input
            label="Mot de passe"
            type="password"
            prefix={<Lock className="h-4 w-4" />}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            autoComplete="new-password"
          />
        ) : (
          <label className="flex flex-col gap-1.5">
            <span className="text-sm font-medium text-text-secondary">Clé SSH privée</span>
            <div className="flex items-center gap-3 rounded-md border border-dashed border-border bg-bg-elevated px-4 py-3 text-sm">
              <KeyRound className="h-4 w-4 text-text-muted" />
              <span className="flex-1 text-text-secondary">
                {sshKey ? `Clé chargée (${sshKey.length} caractères)` : "Importer une clé privée"}
              </span>
              <label className="cursor-pointer text-accent hover:text-accent-hover transition-colors">
                <Upload className="h-4 w-4" />
                <input type="file" accept=".pem,.key" className="hidden" onChange={onFileChange} />
              </label>
            </div>
          </label>
        )}
      </div>

      <Card padding="sm" className="flex items-start gap-2 text-xs text-text-secondary">
        <Lock className="h-4 w-4 text-success mt-0.5" />
        <p>Connexion chiffrée de bout en bout. Vos identifiants ne sont jamais stockés.</p>
      </Card>

      <Button type="submit" size="lg" loading={loading} className="self-stretch">
        Connecter →
      </Button>
    </form>
  );
}

function SystemButton({
  selected,
  onClick,
  label,
  sub,
}: {
  selected: boolean;
  onClick: () => void;
  label: string;
  sub: string;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "flex flex-col items-start gap-1 rounded-md border p-4 text-left transition-all duration-base",
        selected
          ? "border-accent bg-accent-subtle text-text-primary"
          : "border-border bg-bg-raised text-text-secondary hover:border-border-hover"
      )}
    >
      <span className="text-sm font-semibold">{label}</span>
      <span className="text-xs text-text-muted">{sub}</span>
    </button>
  );
}

function MiniToggle({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "rounded-full border px-3 py-1 transition-colors",
        active
          ? "border-accent bg-accent-subtle text-accent"
          : "border-border text-text-secondary hover:border-border-hover"
      )}
    >
      {children}
    </button>
  );
}
