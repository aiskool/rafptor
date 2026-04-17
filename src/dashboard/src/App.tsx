import { BrowserRouter } from "react-router-dom";
import { TooltipProvider } from "@/components/ui/Tooltip";
import { ToastViewport } from "@/components/ui/Toast";
import { AppRoutes } from "@/routes";

export function App() {
  return (
    <TooltipProvider>
      <BrowserRouter>
        <AppRoutes />
        <ToastViewport />
      </BrowserRouter>
    </TooltipProvider>
  );
}
