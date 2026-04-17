import { useState } from "react";
import { Check, X } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";
import { Input } from "@/components/ui/Input";

const REJECT_REASONS = [
  "Texte coupé ou tronqué",
  "Police incorrecte",
  "Positionnement des éléments erroné",
  "Image ou logo manquant",
  "Couleurs incorrectes",
];

export interface ReviewActionsProps {
  onApprove: () => void | Promise<void>;
  onReject: (reason: string) => void | Promise<void>;
  loading?: boolean;
}

export function ReviewActions({ onApprove, onReject, loading }: ReviewActionsProps) {
  const [rejectOpen, setRejectOpen] = useState(false);
  const [reason, setReason] = useState(REJECT_REASONS[0] ?? "");
  const [custom, setCustom] = useState("");

  const submitReject = () => {
    onReject(custom.trim() || reason);
    setRejectOpen(false);
  };

  return (
    <>
      <div className="flex items-center gap-3">
        <Button
          variant="primary"
          size="lg"
          leftIcon={<Check className="h-4 w-4" />}
          onClick={onApprove}
          loading={loading}
        >
          C'est bon
        </Button>
        <Button
          variant="outline"
          size="lg"
          leftIcon={<X className="h-4 w-4" />}
          onClick={() => setRejectOpen(true)}
        >
          À refaire
        </Button>
      </div>

      <Modal
        open={rejectOpen}
        onOpenChange={setRejectOpen}
        title="Pourquoi refaire ce document ?"
        description="Indiquez ce qui ne va pas. Nous l'utiliserons pour améliorer la conversion."
      >
        <div className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-text-secondary">Raison la plus proche</label>
            <select
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              className="rounded-md border border-border bg-bg-elevated px-3 py-2 text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-accent-ring"
            >
              {REJECT_REASONS.map((r) => (
                <option key={r} value={r}>
                  {r}
                </option>
              ))}
            </select>
          </div>
          <Input
            label="Détail (facultatif)"
            placeholder="Ajoutez un commentaire…"
            value={custom}
            onChange={(e) => setCustom(e.target.value)}
          />
          <div className="flex justify-end gap-2">
            <Button variant="ghost" onClick={() => setRejectOpen(false)}>
              Annuler
            </Button>
            <Button variant="danger" onClick={submitReject} loading={loading}>
              Refaire
            </Button>
          </div>
        </div>
      </Modal>
    </>
  );
}
