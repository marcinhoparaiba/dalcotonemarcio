import type { Metadata } from "next";
import "./globals.css";
export const metadata: Metadata = { title: "Dal Cotone CRM", description: "Gestão comercial de salões" };
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) { return <html lang="pt-BR"><body>{children}</body></html>; }
