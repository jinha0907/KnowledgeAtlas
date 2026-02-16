import "./globals.css";

export const metadata = {
  title: "Project KG",
  description: "Project Knowledge Graph MVP",
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
