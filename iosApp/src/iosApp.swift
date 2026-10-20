import SwiftUI
import KotlinModules

struct ComposeHost: UIViewControllerRepresentable {
    let factory: () -> UIViewController

    func makeUIViewController(context: Context) -> UIViewController {
        factory()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @State private var signedIn: Bool = false

    var body: some View {
        Group {
            if signedIn {
                TabView {
                    ComposeHost(factory: { ViewControllerKt.FleetViewController() })
                        .ignoresSafeArea()
                        .tabItem {
                            Label("Fleet", systemImage: "antenna.radiowaves.left.and.right")
                        }
                    ComposeHost(factory: { ViewControllerKt.AlertsViewController() })
                        .ignoresSafeArea()
                        .tabItem {
                            Label("Alerts", systemImage: "bell")
                        }
                    ComposeHost(factory: { ViewControllerKt.SettingsViewController() })
                        .ignoresSafeArea()
                        .tabItem {
                            Label("Settings", systemImage: "gearshape")
                        }
                }
            } else {
                ComposeHost(factory: { ViewControllerKt.LoginViewController() })
                    .ignoresSafeArea()
            }
        }
        .onAppear {
            ViewControllerKt.watchSessionSignedIn { value in
                signedIn = value.boolValue
            }
        }
    }
}

@main
struct iosApp: App {
    init() {
        AppGraphHolder.installNotifications()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
