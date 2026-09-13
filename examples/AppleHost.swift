// Add this pattern to the Swift host's AppDelegate after linking the PushPort SPM product.
import UIKit
import PushPort

@MainActor final class AppDelegate: NSObject, UIApplicationDelegate {
    // Force-link the Objective-C bridge before Kotlin creates its adapter.
    private let pushPortBridgeLink = PushPortKMPBridge()

    func application(_ application: UIApplication, didFinishLaunchingWithOptions options: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        do { try PushPort.shared.initialize(appId: "YOUR_PUSHPORT_APP_ID", environment: .sandbox) }
        catch { assertionFailure(error.localizedDescription) }
        // Start your KMP app here; initialize its PushPort client with the identical configuration.
        return true
    }
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken token: Data) {
        PushPort.shared.didRegisterForRemoteNotifications(deviceToken: token)
    }
    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        PushPort.shared.didFailToRegisterForRemoteNotifications(error: error)
    }
}
