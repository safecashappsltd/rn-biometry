import { NativeModules, Platform } from 'react-native';

export type BiometryType = 'TouchID' | 'FaceID' | 'Biometrics';

interface IsSensorAvailableResult {
  available: boolean;
  biometryType?: BiometryType;
  error?: string;
}

interface SimplePromptOptions {
  promptMessage: string;
  fallbackPromptMessage?: string;
  cancelButtonText?: string;
}

interface SimplePromptResult {
  success: boolean;
  error?: string;
}

const LINKING_ERROR =
  `The package 'rn-biometry' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const RnBiometry = NativeModules.RnBiometry
  ? NativeModules.RnBiometry
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export function multiply(a: number, b: number): Promise<number> {
  return RnBiometry.multiply(a, b);
}

/**
 * Returns promise that resolves to an object with object.biometryType = Biometrics | TouchID | FaceID
 * @returns {Promise<Object>} Promise that resolves to an object with details about biometrics available
 */
export function isSensorAvailable(): Promise<IsSensorAvailableResult> {
  return RnBiometry.isSensorAvailable();
}

/**
 * Prompts user with biometrics dialog using the passed in prompt message and
 * returns promise that resolves to an object with object.success = true if the user passes,
 * object.success = false if the user cancels, and rejects if anything fails
 * @param {Object} simplePromptOptions
 * @param {string} simplePromptOptions.promptMessage
 * @param {string} simplePromptOptions.fallbackPromptMessage
 * @returns {Promise<Object>}  Promise that resolves an object with details about the biometrics result
 */
export function simplePrompt(
  simplePromptOptions: SimplePromptOptions
): Promise<SimplePromptResult> {
  return RnBiometry.simplePrompt(simplePromptOptions);
}
