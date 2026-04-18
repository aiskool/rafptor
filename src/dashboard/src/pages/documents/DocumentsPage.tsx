import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { FileText, Plus } from "lucide-react";
import { DocumentCard, type DocumentCardData } from "@/components/data/DocumentCard";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/Tabs";
import { Skeleton } from "@/components/ui/Skeleton";
import { EmptyState } from "@/components/ui/EmptyState";
import { Button } from "@/components/ui/Button";
import { t } from "@/i18n";

type Filter = "all" | "converted" | "toCheck" | "errors";

// Swap for a real /api/documents?filter= fetch once the backend is wired.
const HAS_DATA = false;

function makeDemo(n: number): DocumentCardData[] {
  if (!HAS_DATA) return [];
  return Array.from({ length: n }).map((_, i) => ({
    id: `doc_${i + 1}`,
    name: `REL_${String(i + 1).padStart(5, "0")}`,
    pageCount: 1 + (i % 12),
    fidelityScore: 0.7 + (i % 30) / 100,
    createdAt: new Date(Date.now() - i * 60_000 * 5).toISOString(),
  }));
}

export default function DocumentsPage() {
  const [filter, setFilter] = useState<Filter>("all");
  const [items, setItems] = useState<DocumentCardData[]>([]);
  const [loading, setLoading] = useState(true);
  const sentinel = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setLoading(true);
    const id = setTimeout(() => {
      setItems(makeDemo(12));
      setLoading(false);
    }, 300);
    return () => clearTimeout(id);
  }, [filter]);

  useEffect(() => {
    if (!sentinel.current) return;
    const io = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting && !loading && items.length > 0 && items.length < 120) {
          setItems((prev) => [...prev, ...makeDemo(12).map((d, i) => ({ ...d, id: `${d.id}_${prev.length + i}` }))]);
        }
      },
      { rootMargin: "400px" }
    );
    io.observe(sentinel.current);
    return () => io.disconnect();
  }, [loading, items.length]);

  const filtered = items.filter((d) => {
    if (filter === "converted") return d.fidelityScore >= 0.9;
    if (filter === "toCheck") return d.fidelityScore >= 0.7 && d.fidelityScore < 0.9;
    if (filter === "errors") return d.fidelityScore < 0.7;
    return true;
  });

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-2xl font-semibold">{t("documents.title")}</h1>
        <Tabs value={filter} onValueChange={(v) => setFilter(v as Filter)}>
          <TabsList>
            <TabsTrigger value="all">{t("documents.filters.all")}</TabsTrigger>
            <TabsTrigger value="converted">{t("documents.filters.converted")}</TabsTrigger>
            <TabsTrigger value="toCheck">{t("documents.filters.toCheck")}</TabsTrigger>
            <TabsTrigger value="errors">{t("documents.filters.errors")}</TabsTrigger>
          </TabsList>
        </Tabs>
      </div>

      {loading && items.length === 0 ? (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
          {Array.from({ length: 10 }).map((_, i) => (
            <Skeleton key={i} className="aspect-[5/7] w-full" />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <EmptyState
          icon={<FileText className="h-5 w-5" />}
          title={items.length === 0 ? "Aucun document pour l'instant" : "Aucun document dans ce filtre"}
          description={
            items.length === 0
              ? "Lancez une première analyse pour voir vos documents convertis ici."
              : undefined
          }
          action={
            items.length === 0 ? (
              <Link to="/onboarding/welcome">
                <Button leftIcon={<Plus className="h-4 w-4" />}>Nouvelle analyse</Button>
              </Link>
            ) : undefined
          }
        />
      ) : (
        <>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
            {filtered.map((d) => (
              <DocumentCard key={d.id} doc={d} />
            ))}
          </div>
          <div ref={sentinel} className="h-8 w-full" />
        </>
      )}
    </div>
  );
}
