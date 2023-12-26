#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(RnBiometry, NSObject)

RCT_EXTERN_METHOD(multiply:(float)a withB:(float)b
                 withResolver:(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(isSensorAvailable:(NSDictionary *)params
                          resolver:(RCTPromiseResolveBlock)resolve
                          rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(showBiometricPromptForDecryption:(NSDictionary *)params
                                           resolver:(RCTPromiseResolveBlock)resolve
                                           rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(showBiometricPromptForEncryption:(NSDictionary *)params
                                           resolver:(RCTPromiseResolveBlock)resolve
                                           rejecter:(RCTPromiseRejectBlock)reject)

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

@end