import "./globals.css";

export const metadata = {
  title: "Knowledge Atlas",
  description: "Project knowledge and decision evidence map",
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
