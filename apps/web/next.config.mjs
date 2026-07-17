/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  eslint: {
    // CI runs `npm run lint` before the production build.
    ignoreDuringBuilds: true,
  },
};

export default nextConfig;
