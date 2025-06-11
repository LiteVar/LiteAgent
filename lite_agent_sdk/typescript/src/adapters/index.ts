// import { BrowserAdapter } from './browser-adapter';
// import { NodeAdapter } from './node-adapter';
// import { ReactNativeAdapter } from './react-native-adapter';
import { ApiAdapter } from '../types/api-types';
import { UniversalAdapter } from './universalAdapter';

/**
 * Determines the current platform and returns the appropriate adapter
 */
// function detectPlatform(): ApiAdapter {
  // Check if we're in Node.js
  // if (typeof process !== 'undefined' && process.versions && process.versions.node) {
    // return new NodeAdapter();
  // }
  
  // Check if we're in React Native
  // if (typeof navigator !== 'undefined' && navigator.product === 'ReactNative') {
  //   return new ReactNativeAdapter();
  // }
  
  // Default to browser
  // return new BrowserAdapter();
// }

export const defaultAdapter = new UniversalAdapter();
