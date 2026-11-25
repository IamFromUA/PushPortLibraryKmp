// Add this pattern to the Swift host's AppDelegate after linking the PushPort SPM product.
import UIKit
import PushPort

@MainActor final class AppDelegate: NSObject, UIApplicationDelegate {
    // Force-link the Objective-C bridge before Kotlin creates its adapter.
    private let pushPortBridgeLink = PushPortKMPBridge()

    func application(_ application: UIApplication, didFinishLaunchingWithOptions options: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        // Start the shared Kotlin app here. Its createApplePushPort() client owns
        // initialization and applies the configured consent policy before registration.
        return true
    }
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken token: Data) {
        PushPort.shared.didRegisterForRemoteNotifications(deviceToken: token)
    }
    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        PushPort.shared.didFailToRegisterForRemoteNotifications(error: error)
    }
}
