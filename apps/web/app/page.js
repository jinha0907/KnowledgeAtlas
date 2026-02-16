const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

export default function Home() {
  return (
    <main className="container">
      <h1>Project Knowledge Graph</h1>
      <p>MVP bootstrap is ready.</p>
      <a href={`${apiBaseUrl}/api/health`} target="_blank" rel="noreferrer">
        Open API health check
      </a>
    </main>
  );
}
