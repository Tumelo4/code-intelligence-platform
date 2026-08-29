import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Code Intelligence",
  description: "Evidence-backed code understanding and verified refactoring"
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body>{children}</body></html>;
}
