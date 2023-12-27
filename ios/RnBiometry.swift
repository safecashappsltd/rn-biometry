import LocalAuthentication
import Security
import CommonCrypto

extension Data {
    var bytes: UnsafeRawPointer {
        return (self as NSData).bytes
    }
}


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
    guard let encryptedTokenData = params["encryptedToken"] as? Data else {
    reject("Parameter_Error", "Encrypted token not found in parameters", nil)
    return
}

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
                if status == errSecSuccess, let keyData = item as? Data, let decryptedToken = self.decryptToken(encryptedToken: encryptedTokenData, with: keyData) {
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

    print("Token: \(token)")

    // Access Control for the keychain item
    var accessControlError: Unmanaged<CFError>?
    guard let accessControl = SecAccessControlCreateWithFlags(nil, kSecAttrAccessibleWhenUnlockedThisDeviceOnly, .userPresence, &accessControlError) else {
        print("accessControl failed")
        reject("AccessControl_Error", "Could not create access control", accessControlError?.takeRetainedValue())
        return
    }

    context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: promptMessage) { success, evaluateError in
        DispatchQueue.main.async {
            if success {
                let symmetricKey = self.generateSymmetricKey() 
                print("Symmetric Key Generated")

                guard let encryptedToken = self.encryptToken(token: token, with: symmetricKey) else {
                    print("Encryption failed")
                    reject("Encryption_Error", "Failed to encrypt the token", nil)
                    return
                }

                let encryptedTokenBase64 = encryptedToken.base64EncodedString()
                print("Encrypted Token: \(encryptedTokenBase64)")

                // Store the symmetric key in the keychain
                let keyQuery: [String: Any] = [
                    kSecClass as String: kSecClassGenericPassword,
                    kSecAttrService as String: "YourService",
                    kSecAttrAccount as String: "SymmetricKey",
                    kSecValueData as String: symmetricKey
                ]
                SecItemAdd(keyQuery as CFDictionary, nil)

                let keychainResult = SecItemAdd(keyQuery as CFDictionary, nil)
               print("Keychain add result: \(keychainResult)")

               if keychainResult == errSecSuccess {
                    // Return the Base64 encoded encrypted token to JavaScript
                   resolve(encryptedTokenBase64)
              } else {
             reject("Keychain_Error", "Failed to store symmetric key in keychain", nil)
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

//     private func generateSymmetricKey() -> Data {
//     let key = SymmetricKey(size: .bits256) // Generates a 256-bit key
//     return key.withUnsafeBytes { Data($0) }
// }

private func generateSymmetricKey() -> Data {
    var key = Data(count: kCCKeySizeAES256)
    let result = key.withUnsafeMutableBytes { 
        SecRandomCopyBytes(kSecRandomDefault, kCCKeySizeAES256, $0.baseAddress!) 
    }
    if result == errSecSuccess {
        return key
    } else {
        fatalError("Unable to generate random bytes for key")
    }
}

private func encryptToken(token: String, with key: Data) -> Data? {
    guard let data = token.data(using: .utf8), key.count == kCCKeySizeAES256 else { return nil }
    var numBytesEncrypted = 0
    var encryptedBytes = [UInt8](repeating: 0, count: data.count + kCCBlockSizeAES128)
    
    let status = CCCrypt(CCOperation(kCCEncrypt), CCAlgorithm(kCCAlgorithmAES), CCOptions(kCCOptionPKCS7Padding), key.bytes, key.count, nil, data.bytes, data.count, &encryptedBytes, encryptedBytes.count, &numBytesEncrypted)

    return status == kCCSuccess ? Data(bytes: encryptedBytes, count: numBytesEncrypted) : nil
}


private func decryptToken(encryptedToken: Data, with key: Data) -> String? {
    guard key.count == kCCKeySizeAES256 else { return nil }
    var numBytesDecrypted = 0
    var decryptedBytes = [UInt8](repeating: 0, count: encryptedToken.count + kCCBlockSizeAES128)
    
    let status = CCCrypt(CCOperation(kCCDecrypt), CCAlgorithm(kCCAlgorithmAES), CCOptions(kCCOptionPKCS7Padding), key.bytes, key.count, nil, encryptedToken.bytes, encryptedToken.count, &decryptedBytes, decryptedBytes.count, &numBytesDecrypted)

return status == kCCSuccess ? String(data: Data(decryptedBytes[0..<numBytesDecrypted]), encoding: .utf8) : nil
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
