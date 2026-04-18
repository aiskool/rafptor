import { Check } from "lucide-react";
import { DocumentCard, type DocumentCardData } from "@/components/data/DocumentCard";
import { EmptyState } from "@/components/ui/EmptyState";
import { t } from "@/i18n";

// Swap for a /api/review/queue fetch once backend data is wired.
const items: DocumentCardData[] = [];

export default function ReviewQueuePage() {
  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-semibold">{t("review.queue.title")}</h1>
      {items.length === 0 ? (
        <EmptyState icon={<Check className="h-5 w-5" />} title={t("review.queue.empty")} />
      ) : (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
          {items.map((d) => (
            <DocumentCard key={d.id} doc={d} />
          ))}
        </div>
      )}
    </div>
  );
}
