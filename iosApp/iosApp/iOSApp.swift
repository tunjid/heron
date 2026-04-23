import SwiftUI
import Firebase
import FirebaseMessaging
import UserNotifications
import ComposeApp

class AppDelegate: NSObject, UIApplicationDelegate,
                   MessagingDelegate, UNUserNotificationCenterDelegate {

    let appState = EntryPoint_iosKt.createAppState()

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        FirebaseApp.configure()
        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().delegate = self
        application.registerForRemoteNotifications()
        return true
    }

    // Re-sync notification permission state and FCM token on every foreground
    func applicationDidBecomeActive(_ application: UIApplication) {
      Messaging.messaging().token { [self] token, _ in
            guard let token = token else { return }
            onNewToken(fcmToken: token)
        }
    }

    // Forward APNs device token to Firebase
    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        Messaging.messaging().apnsToken = deviceToken
    }

    // Firebase FCM token → forward to Kotlin
    func messaging(
        _ messaging: Messaging,
        didReceiveRegistrationToken fcmToken: String?
    ) {
        guard let token = fcmToken else { return }
        onNewToken(fcmToken: token)
    }

    // Data-only / silent push notifications (background delivery)
    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        var payload: [String: String] = [:]
        for (key, value) in userInfo {
            if let k = key as? String, let v = value as? String {
                payload[k] = v
            }
        }
        EntryPoint_iosKt.onPushNotificationReceived(appState: appState, payload: payload) {
            completionHandler(.newData)
        }
    }

    func onNewToken(
        fcmToken: String,
    ) {
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            let hasPermissions =
                settings.authorizationStatus == .authorized
                    || settings.authorizationStatus == .provisional

            DispatchQueue.main.async {
                EntryPoint_iosKt.onNotificationPermissionsUpdated(
                    appState: self.appState,
                    hasPermissions: hasPermissions
                )

                EntryPoint_iosKt.onNewFcmToken(appState: self.appState, token: fcmToken)
            }
        }
    }

    // Show notification banner when app is in foreground
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound, .badge])
    }

    // Handle notification tap → deep link
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        if let scheme = userInfo[IosNotifier.companion.DEEP_LINK_SCHEME_KEY] as? String,
           let path = userInfo[IosNotifier.companion.DEEP_LINK_PATH_KEY] as? String {
            EntryPoint_iosKt.onNotificationTapped(appState: appState, scheme: scheme, path: path)
        }
        completionHandler()
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView(appState: delegate.appState)
        }
    }
}
