// ABI contract implemented by the PushPort Swift package. Link that product in the iOS host.
#import <Foundation/Foundation.h>
NS_ASSUME_NONNULL_BEGIN
@interface PPPushPortBridge : NSObject
- (instancetype)init;
- (void)initializeWithAppId:(NSString *)appId serverURL:(NSString *)serverURL environment:(NSString *)environment completion:(void (^)(NSString * _Nullable))completion;
- (NSString *)statusJSON;
- (void)sync;
- (NSString * _Nullable)userJSON;
- (void)updateUserJSON:(NSString *)value completion:(void (^)(NSString * _Nullable))completion;
- (void)setConsentRequired:(BOOL)value completion:(void (^)(NSString * _Nullable))completion;
- (void)setConsentGiven:(BOOL)value completion:(void (^)(NSString * _Nullable))completion;
- (void)setSubscribed:(BOOL)value completion:(void (^)(NSString * _Nullable))completion;
- (void)setLocale:(NSString * _Nullable)value completion:(void (^)(NSString * _Nullable))completion;
- (void)requestPermissionWithCompletion:(void (^)(NSString * _Nullable))completion;
@end
NS_ASSUME_NONNULL_END
