import { NativeModules, Platform } from 'react-native';

export type BiometryType = 'TouchID' | 'FaceID' | 'Biometrics';

interface RNBiometricsOptions {
  allowDeviceCredentials?: boolean;
}

interface IsSensorAvailableResult {
  available: boolean;
  biometryType?: BiometryType;
  error?: string;
}

interface EncryptPromptOptions {
  promptMessage: string;
  token: string;
  fallbackPromptMessage?: string;
  cancelButtonText?: string;
}

interface EncryptPromptResult {
  success: boolean;
  encryptedToken: string;
  error?: string;
}

interface DecryptPromptOptions {
  promptMessage: string;
  encryptedToken: string;
  fallbackPromptMessage?: string;
  cancelButtonText?: string;
}

interface DecryptPromptResult {
  success: boolean;
  token: string;
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

export default class ReactNativeBiometrics {
  allowDeviceCredentials = false;

  /**
   * @param {Object} rnBiometricsOptions
   * @param {boolean} rnBiometricsOptions.allowDeviceCredentials
   */
  constructor(rnBiometricsOptions?: RNBiometricsOptions) {
    const allowDeviceCredentials =
      rnBiometricsOptions?.allowDeviceCredentials ?? false;
    this.allowDeviceCredentials = allowDeviceCredentials;
  }

  /**
   * Returns promise that resolves to an object with object.biometryType = Biometrics | TouchID | FaceID
   * @returns {Promise<Object>} Promise that resolves to an object with details about biometrics available
   */
  isSensorAvailable(): Promise<IsSensorAvailableResult> {
    return RnBiometry.isSensorAvailable({
      allowDeviceCredentials: this.allowDeviceCredentials,
    });
  }

  encryptPrompt(
    encryptPromptOptions: EncryptPromptOptions
  ): Promise<EncryptPromptResult> {
    encryptPromptOptions.cancelButtonText =
      encryptPromptOptions.cancelButtonText ?? 'Cancel';
    encryptPromptOptions.fallbackPromptMessage =
      encryptPromptOptions.fallbackPromptMessage ?? 'Use Passcode';

    return RnBiometry.showBiometricPromptForEncryption({
      allowDeviceCredentials: this.allowDeviceCredentials,
      ...encryptPromptOptions,
    });
  }

  decryptPrompt(
    decryptPromptOptions: DecryptPromptOptions
  ): Promise<DecryptPromptResult> {
    decryptPromptOptions.cancelButtonText =
      decryptPromptOptions.cancelButtonText ?? 'Cancel';
    decryptPromptOptions.fallbackPromptMessage =
      decryptPromptOptions.fallbackPromptMessage ?? 'Use Passcode';

    return RnBiometry.showBiometricPromptForDecryption({
      allowDeviceCredentials: this.allowDeviceCredentials,
      ...decryptPromptOptions,
    });
  }
}
