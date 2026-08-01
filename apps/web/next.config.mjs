/** @type {import('next').NextConfig} */
const apiBaseUrl = (process.env.API_BASE_URL || process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080")
  .replace(/\/$/, "");

const nextConfig = {
  reactStrictMode: true,
  eslint: {
    // CI runs `npm run lint` before the production build.
    ignoreDuringBuilds: true,
  },
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${apiBaseUrl}/api/:path*`,
      },
    ];
  },
};

export default nextConfig;
