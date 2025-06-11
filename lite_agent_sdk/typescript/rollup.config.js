import typescript from 'rollup-plugin-typescript2';
import terser from '@rollup/plugin-terser';
import { createRequire } from 'module';
import { fileURLToPath } from 'url';
import path from 'path';

// Set up __dirname equivalent for ES modules
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Use createRequire to load JSON files in ESM
const require = createRequire(import.meta.url);
const pkg = require('./package.json');

// Constants for the build
const input = 'src/index.ts';
const external = [
  ...Object.keys(pkg.dependencies || {}),
  ...Object.keys(pkg.peerDependencies || {})
];

// Shared TypeScript configuration
const typescriptConfig = {
  typescript: require('typescript'),
  useTsconfigDeclarationDir: true,
  tsconfigOverride: {
    compilerOptions: {
      declaration: true,
      declarationDir: 'dist/types'
    },
    exclude: ['node_modules', 'test', 'dist', '**/*.test.ts']
  }
};

export default [
  // CommonJS build (for Node.js)
  {
    input,
    external,
    output: {
      file: pkg.main,
      format: 'cjs',
      sourcemap: true,
      exports: 'auto',
    },
    plugins: [
      typescript(typescriptConfig),
      terser()
    ]
  },
  
  // ESM build (for modern browsers and bundlers)
  {
    input,
    external,
    output: {
      file: pkg.module,
      format: 'esm',
      sourcemap: true,
      exports: 'named',
    },
    plugins: [
      typescript(typescriptConfig),
      terser()
    ]
  },
  
  // UMD build (for browsers, exposing global variable)
  {
    input,
    output: {
      file: 'dist/index.umd.js',
      format: 'umd',
      name: 'LiteAgent',
      sourcemap: true,
      globals: {
      }
    },
    external,
    plugins: [
      typescript({
        ...typescriptConfig,
        tsconfigOverride: {
          ...typescriptConfig.tsconfigOverride,
          compilerOptions: {
            ...typescriptConfig.tsconfigOverride.compilerOptions,
            target: 'es5'
          }
        }
      }),
      terser()
    ]
  }
];