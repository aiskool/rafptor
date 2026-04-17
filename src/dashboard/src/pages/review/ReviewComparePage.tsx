import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, ArrowRight, Eye, GitCompareArrows } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Kbd } from "@/components/ui/Kbd";
import { ScoreBadge } from "@/components/data/ScoreBadge";
import { PageDots } from "@/components/review/PageDots";
import { ReviewActions } from "@/components/review/ReviewActions";
import { SplitView } from "@/components/review/SplitView";
import { ZoomableImage } from "@/components/review/ZoomableImage";
import { OverlayToggle } from "@/components/review/OverlayToggle";
import { useSyncScroll } from "@/hooks/useSyncScroll";
import { useToastStore } from "@/store/toastStore";
import { cn } from "@/lib/utils";
import { t } from "@/i18n";

export default function ReviewComparePage() {
  const { id = "REL_00847" } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const pushToast = useToastStore((s) => s.push);
  const totalPages = 12;
  const [page, setPage] = useState(0);
  const [zoom, setZoom] = useState(1);
  const [mode, setMode] = useState<"split" | "overlay" | "diff">("split");
  const [opacity, setOpacity] = useState(0.5);
  const score = 0.87;

  const leftScrollRef = useRef<HTMLDivElement>(null);
  const rightScrollRef = useRef<HTMLDivElement>(null);
  useSyncScroll([leftScrollRef, rightScrollRef]);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return;
      if (e.key === "ArrowLeft") setPage((p) => Math.max(0, p - 1));
      if (e.key === "ArrowRight") setPage((p) => Math.min(totalPages - 1, p + 1));
      if (e.key.toLowerCase() === "o") setMode((m) => (m === "overlay" ? "split" : "overlay"));
      if (e.key.toLowerCase() === "d") setMode((m) => (m === "diff" ? "split" : "diff"));
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [totalPages]);

  const approve = () =>
    pushToast({
      title: "Document validé",
      description: `${id} · page ${page + 1}`,
      variant: "success",
    });
  const reject = (reason: string) =>
    pushToast({ title: "Document refusé", description: reason, variant: "warning" });

  const refImage = `/api/documents/${id}/page/${page + 1}/reference`;
  const convImage = `/api/documents/${id}/page/${page + 1}/image`;
  const diffImage = `/api/documents/${id}/page/${page + 1}/diff`;

  return (
    <div className="flex h-[calc(100vh-4rem)] flex-col gap-4">
      <div className="flex items-center justify-between gap-3">
        <Button
          variant="ghost"
          leftIcon={<ArrowLeft className="h-4 w-4" />}
          onClick={() => navigate(-1)}
        >
          {t("common.back")}
        </Button>
        <div className="flex items-center gap-3 text-sm text-text-secondary">
          <span className="font-medium text-text-primary">{id}</span>
          <span>·</span>
          <span>
            {t("review.compare.page", { current: page + 1, total: totalPages })}
          </span>
          <ScoreBadge score={score} animated={false} />
        </div>
        <div className="flex items-center gap-2">
          <Button
            size="sm"
            variant={mode === "overlay" ? "primary" : "outline"}
            leftIcon={<Eye className="h-3.5 w-3.5" />}
            onClick={() => setMode((m) => (m === "overlay" ? "split" : "overlay"))}
          >
            {t("review.compare.overlay")} <Kbd className="ml-1">O</Kbd>
          </Button>
          <Button
            size="sm"
            variant={mode === "diff" ? "primary" : "outline"}
            leftIcon={<GitCompareArrows className="h-3.5 w-3.5" />}
            onClick={() => setMode((m) => (m === "diff" ? "split" : "diff"))}
          >
            {t("review.compare.diff")} <Kbd className="ml-1">D</Kbd>
          </Button>
        </div>
      </div>

      {mode === "split" && (
        <SplitView
          className="flex-1"
          leftLabel={t("review.compare.original")}
          rightLabel={t("review.compare.converted")}
          left={
            <div ref={leftScrollRef} className="h-full w-full overflow-auto">
              <div style={{ height: `${100 * zoom}%` }}>
                <ZoomableImage src={refImage} zoom={zoom} onZoomChange={setZoom} />
              </div>
            </div>
          }
          right={
            <div ref={rightScrollRef} className="h-full w-full overflow-auto">
              <div style={{ height: `${100 * zoom}%` }}>
                <ZoomableImage src={convImage} zoom={zoom} onZoomChange={setZoom} />
              </div>
            </div>
          }
        />
      )}

      {mode === "overlay" && (
        <div className="relative flex-1 overflow-hidden rounded-lg border border-border bg-bg-raised">
          <img
            src={refImage}
            alt=""
            className="absolute inset-0 h-full w-full object-contain"
          />
          <img
            src={convImage}
            alt=""
            className={cn("absolute inset-0 h-full w-full object-contain transition-opacity duration-100")}
            style={{ opacity }}
          />
          <div className="absolute bottom-4 right-4 w-60 rounded-md border border-border bg-bg-overlay/80 p-3 backdrop-blur">
            <OverlayToggle opacity={opacity} onChange={setOpacity} />
          </div>
        </div>
      )}

      {mode === "diff" && (
        <div className="relative flex-1 overflow-hidden rounded-lg border border-border bg-bg-raised">
          <img
            src={diffImage}
            alt=""
            className="absolute inset-0 h-full w-full object-contain"
          />
        </div>
      )}

      <div className="flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-2">
          <Button
            size="icon"
            variant="outline"
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            aria-label="Page précédente"
          >
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <PageDots current={page} total={totalPages} onChange={setPage} />
          <Button
            size="icon"
            variant="outline"
            disabled={page === totalPages - 1}
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            aria-label="Page suivante"
          >
            <ArrowRight className="h-4 w-4" />
          </Button>
        </div>
        <ReviewActions onApprove={approve} onReject={reject} />
      </div>
    </div>
  );
}
