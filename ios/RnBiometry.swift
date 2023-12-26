import LocalAuthentication
import Security

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

        let promptMessage = params["promptMessage"] as? String ?? "Authenticate to decrypt the data"
        let combinedString = params["encryptedToken"] as? String ?? ""

        // Split the combined string into IV and encrypted data
        let parts = combinedString.split(separator: ":")
        guard parts.count == 2, let ivData = Data(base64Encoded: String(parts[0])), let encryptedData = Data(base64Encoded: String(parts[1])) else {
            reject("Decryption_Error", "Invalid combined string format", nil)
            return
        }

        context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: promptMessage) { success, evaluateError in
            DispatchQueue.main.async {
                if success {
                    // Implement the decryption logic here
                    if let decryptedToken = self.decryptData(encryptedData: encryptedData, iv: ivData) {
                        resolve(decryptedToken)
                    } else {
                        reject("Decryption_Error", "Decryption failed", nil)
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

        let promptMessage = params["promptMessage"] as? String ?? "Authenticate to encrypt the data"
        let payload = params["payload"] as? String ?? ""

        context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: promptMessage) { success, evaluateError in
            DispatchQueue.main.async {
                if success {
                    // Placeholder for encryption logic
                    // Encrypt the payload here as needed

                    // For now, just return true as you requested
                    resolve(true)
                } else {
                    // Handle the error
                    if let error = evaluateError as NSError? {
                        reject("Authentication_Error", "Authentication failed", error)
                    }
                }
            }
        }
    }

    private func decryptData(encryptedData: Data, iv: Data) -> String? {
        // Implement decryption logic here
        // Example: Decrypt using a symmetric key

        // For demonstration, return a placeholder string
        return "Decrypted data"
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