/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  // Produces a tiny self-contained server in .next/standalone - perfect for Docker
  output: 'standalone',
};

module.exports = nextConfig;
