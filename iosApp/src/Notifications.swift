import SwiftUI
import UserNotifications
import KotlinModules

final class NotificationAlertPresenter: NSObject, AlertPresenter, UNUserNotificationCenterDelegate {
    override init() {
        super.init()
        UNUserNotificationCenter.current().delegate = self
    }

    func present(alertId: String, title: String, body: String, severity: String) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.userInfo = ["alertId": alertId, "deviceId": "", "severity": severity]
        content.sound = severity == "ALARM" ? .defaultCritical : .default
        let request = UNNotificationRequest(
            identifier: alertId,
            content: content,
            trigger: nil,
        )
        UNUserNotificationCenter.current().add(request)
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
    ) async -> UNNotificationPresentationOptions {
        [.banner, .sound, .badge]
    }
}

extension AppGraphHolder {
    static func installNotifications() {
        let presenter = NotificationAlertPresenter()
        shared.graph.alertCenter.presenterOverride = presenter
        let center = UNUserNotificationCenter.current()
        center.requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
            _ = granted
        }
    }
}
