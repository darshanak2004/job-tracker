const nextJest = require('next/jest');

// next/jest wires up SWC, CSS mocks and .env loading for us
const createJestConfig = nextJest({ dir: './' });

module.exports = createJestConfig({
  testEnvironment: 'jest-environment-jsdom',
  setupFilesAfterEnv: ['<rootDir>/jest.setup.ts'],
  moduleNameMapper: { '^@/(.*)$': '<rootDir>/src/$1' },
  collectCoverageFrom: ['src/components/**/*.tsx', 'src/lib/**/*.{ts,tsx}'],
});
