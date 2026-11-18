import type { Metadata } from "next";
import "./globals.css";
import ServiceWorker from "../components/ServiceWorker";
export const metadata: Metadata = { title: "Dal Cotone CRM", description: "Gestão comercial de salões", manifest:"/manifest.webmanifest", themeColor:"#9b111e" };
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) { return <html lang="pt-BR"><body><ServiceWorker/>{children}</body></html>; }
