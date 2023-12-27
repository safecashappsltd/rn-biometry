import LocalAuthentication
import Security
import CryptoKit

@objc(RnBiometry)
class RnBiometry: NSObject {

    @objc(multiply:withB:withResolver:withRejecter:)
    func multiply(a: Float, b: Float, resolve: RCTPromiseResolveBlock, reject: RCTPromiseRejectBlock) -> Void {
        resolve(a * b)
    }

    @objc(isSensorAvailable:resolver:rejecter:)
    func isSensorAvailable(params: NSDictionary, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        let context = LAContext()
        var error: NSError?
        
        let allowDeviceCredentials = params["allowDeviceCredentials"] as? Bool ?? false
        let policy: LAPolicy = allowDeviceCredentials ? .deviceOwnerAuthentication : .deviceOwnerAuthenticationWithBiometrics

        if context.canEvaluatePolicy(policy, error: &error) {
            let biometryType = getBiometryType(context: context)
            let result: [String: Any] = ["available": true, "biometryType": biometryType]
            resolve(result)
        } else {
            let errorMessage = error?.localizedDescription ?? "Unknown error"
            let result: [String: Any] = ["available": false, "error": errorMessage]
            resolve(result)
        }
    }

@objc(showBiometricPromptForDecryption:resolver:rejecter:)
func showBiometricPromptForDecryption(params: NSDictionary, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    let context = LAContext()
    context.localizedCancelTitle = params["cancelButtonText"] as? String ?? "Cancel"

    // Check if biometric authentication is available
    var error: NSError?
    guard context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) else {
        reject("Biometric_Error", "Biometric authentication is not available", error)
        return
    }

    let promptMessage = params["promptMessage"] as? String ?? "Authenticate to decrypt and retrieve the data"

    context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: promptMessage) { success, evaluateError in
        DispatchQueue.main.async {
            if success {
                // Retrieve the symmetric key from the keychain
                let keyQuery: [String: Any] = [
                    kSecClass as String: kSecClassGenericPassword,
                    kSecAttrService as String: "YourService",
                    kSecAttrAccount as String: "SymmetricKey",
                    kSecReturnData as String: kCFBooleanTrue as Any,
                    kSecMatchLimit as String: kSecMatchLimitOne
                ]

                var item: CFTypeRef?
                let status = SecItemCopyMatching(keyQuery as CFDictionary, &item)
                if status == errSecSuccess, let keyData = item as? Data, let decryptedToken = decryptToken(encryptedToken, with: keyData) {
                    resolve(decryptedToken)
                } else {
                    reject("Keychain_Error", "Could not retrieve symmetric key", NSError(domain: NSOSStatusErrorDomain, code: Int(status)))
                }
            } else {
                // Handle the error
                if let error = evaluateError as NSError? {
                    reject("Authentication_Error", "Authentication failed", error)
                }
            }
        }
    }
}


  @objc(showBiometricPromptForEncryption:resolver:rejecter:)
func showBiometricPromptForEncryption(params: NSDictionary, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    let context = LAContext()
    context.localizedCancelTitle = params["cancelButtonText"] as? String ?? "Cancel"

    // Check if biometric authentication is available
    var error: NSError?
    guard context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) else {
        reject("Biometric_Error", "Biometric authentication is not available", error)
        return
    }

    let promptMessage = params["promptMessage"] as? String ?? "Authenticate to encrypt and store the data"
    let token = params["token"] as? String ?? ""

    // Access Control for the keychain item
    var accessControlError: Unmanaged<CFError>?
    guard let accessControl = SecAccessControlCreateWithFlags(nil, kSecAttrAccessibleWhenUnlockedThisDeviceOnly, .userPresence, &accessControlError) else {
        reject("AccessControl_Error", "Could not create access control", accessControlError?.takeRetainedValue())
        return
    }

    context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: promptMessage) { success, evaluateError in
        DispatchQueue.main.async {
            if success {
                let symmetricKey = generateSymmetricKey() // Implement this function
                guard let encryptedToken = encryptToken(token, with: symmetricKey) else {
                    reject("Encryption_Error", "Failed to encrypt the token", nil)
                    return
                }

                // Store the symmetric key in the keychain
                let keyQuery: [String: Any] = [
                    kSecClass as String: kSecClassGenericPassword,
                    kSecAttrService as String: "YourService",
                    kSecAttrAccount as String: "SymmetricKey",
                    kSecValueData as String: symmetricKey
                ]
                SecItemAdd(keyQuery as CFDictionary, nil)

                // Return the encrypted token to JavaScript
                resolve(encryptedToken)
            } else {
                // Handle the error
                if let error = evaluateError as NSError? {
                    reject("Authentication_Error", "Authentication failed", error)
                }
            }
        }
    }
}

    private func generateSymmetricKey() -> Data {
    let key = SymmetricKey(size: .bits256) // Generates a 256-bit key
    return key.withUnsafeBytes { Data($0) }
}

import CryptoKit

private func encryptToken(_ token: String, with key: Data) -> Data? {
    let key = SymmetricKey(data: key)
    let data = Data(token.utf8)
    do {
        let sealedBox = try AES.GCM.seal(data, using: key)
        return sealedBox.combined
    } catch {
        return nil
    }
}


private func decryptToken(_ encryptedToken: Data, with key: Data) -> String? {
    let key = SymmetricKey(data: key)
    do {
        let sealedBox = try AES.GCM.SealedBox(combined: encryptedToken)
        let decryptedData = try AES.GCM.open(sealedBox, using: key)
        return String(data: decryptedData, encoding: .utf8)
    } catch {
        return nil
    }
}

    private func getBiometryType(context: LAContext) -> String {
        if #available(iOS 11.0, *) {
            return context.biometryType == .faceID ? "FaceID" : "TouchID"
        } else {
            // Fallback on earlier versions
            return "TouchID"
        }
    }
}