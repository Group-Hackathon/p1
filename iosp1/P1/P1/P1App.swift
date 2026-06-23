//
//  P1App.swift
//  P1
//
//  Created by Gary on 21/06/2026.
//

import SwiftUI

@main
struct P1App: App {
    @Environment(\.scenePhase) private var scenePhase
    
    init() {
        // Register background tasks
        BackgroundManager.shared.registerBackgroundTasks()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
        .onChange(of: scenePhase) { newPhase in
            if newPhase == .background {
                BackgroundManager.shared.scheduleAppRefresh()
            }
        }
    }
}
