import SwiftUI

@main
struct EchoicApp: App {
    @AppStorage("has_completed_onboarding") private var hasCompletedOnboarding = false
    @AppStorage("app_appearance") private var appearance: String = "system"
    @State private var showSettingsPanel = false

    private var colorScheme: ColorScheme? {
        switch appearance {
        case "light": .light
        case "dark": .dark
        default: nil
        }
    }

    var body: some Scene {
        WindowGroup {
            Group {
                if hasCompletedOnboarding {
                    ContentView(showSettings: $showSettingsPanel)
                } else {
                    OnboardingView()
                }
            }
            .preferredColorScheme(colorScheme)
        }
        .windowStyle(.titleBar)
        .windowToolbarStyle(.unified(showsTitle: true))
        .defaultSize(width: 900, height: 640)
        .commands {
            CommandGroup(replacing: .appSettings) {
                Button("Settings") {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) {
                        showSettingsPanel.toggle()
                    }
                }
                .keyboardShortcut(",", modifiers: .command)
            }
        }
    }
}
