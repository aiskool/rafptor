import { Check } from "lucide-react";
import { DocumentCard, type DocumentCardData } from "@/components/data/DocumentCard";
import { EmptyState } from "@/components/ui/EmptyState";
import { t } from "@/i18n";

const demo: DocumentCardData[] = [
  {
    id: "REL_00847",
    name: "REL_00847",
    pageCount: 12,
    fidelityScore: 0.87,
    createdAt: new Date(Date.now() - 12 * 60_000).toISOString(),
  },
  {
    id: "FAC_01203",
    name: "FAC_01203",
    pageCount: 4,
    fidelityScore: 0.82,
    createdAt: new Date(Date.now() - 30 * 60_000).toISOString(),
  },
  {
    id: "REL_00912",
    name: "REL_00912",
    pageCount: 8,
    fidelityScore: 0.79,
    createdAt: new Date(Date.now() - 45 * 60_000).toISOString(),
  },
];

export default function ReviewQueuePage() {
  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-semibold">{t("review.queue.title")}</h1>
      {demo.length === 0 ? (
        <EmptyState
          icon={<Check className="h-5 w-5" />}
          title={t("review.queue.empty")}
        />
      ) : (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
          {demo.map((d) => (
            <DocumentCard key={d.id} doc={d} />
          ))}
        </div>
      )}
    </div>
  );
}
