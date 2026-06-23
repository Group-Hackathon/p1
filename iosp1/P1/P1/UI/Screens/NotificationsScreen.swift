import SwiftUI
import UserNotifications

private struct ReminderUi: Identifiable {
    let id: String
    let title: String
    let description: String
    let time: String
}

private let defaultReminders = [
    ReminderUi(id: "photo", title: "Daily photo", description: "Guided photo of the tracked area", time: "9:00 AM"),
    ReminderUi(id: "checkin", title: "Evening check-in", description: "Pain level and infection questions", time: "7:00 PM"),
    ReminderUi(id: "missed", title: "Missed routine alert", description: "If nothing is recorded by this time", time: "8:30 PM")
]

struct NotificationsScreen: View {
    let onBack: () -> Void
    
    @State private var enabledStates: [String: Bool] = [
        "photo": true,
        "checkin": true,
        "missed": true
    ]
    @State private var testSent = false
    
    var body: some View {
        VStack(spacing: 0) {
            LpmTopBar(title: "Notifications", onBack: onBack)
            
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("Reminders keep your follow-up consistent. Each entry strengthens the report for your doctor.")
                        .font(.body)
                        .foregroundColor(Color(UIColor.darkGray))
                        .padding(.top, 8)
                    
                    Text("DAILY REMINDERS")
                        .font(.caption)
                        .fontWeight(.semibold)
                        .kerning(1.5)
                        .foregroundColor(.gray)
                        .padding(.top, 12)
                    
                    ForEach(defaultReminders) { reminder in
                        ReminderRow(
                            reminder: reminder,
                            isEnabled: Binding(
                                get: { enabledStates[reminder.id] ?? false },
                                set: { enabledStates[reminder.id] = $0 }
                            )
                        )
                    }
                    
                    Spacer().frame(height: 16)
                    
                    LpmSecondaryButton(
                        text: testSent ? "Test notification sent" : "Send a test notification"
                    ) {
                        sendTestNotification()
                    }
                    .disabled(testSent)
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 32)
            }
        }
        .background(Color.white)
    }
    
    private func sendTestNotification() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
            if granted {
                let content = UNMutableNotificationContent()
                content.title = "P1 — Daily routine"
                content.body = "Time for your photo and check-in."
                content.sound = .default
                
                let request = UNNotificationRequest(
                    identifier: UUID().uuidString,
                    content: content,
                    trigger: UNTimeIntervalNotificationTrigger(timeInterval: 3, repeats: false) // 3 seconds delay
                )
                
                UNUserNotificationCenter.current().add(request)
                
                DispatchQueue.main.async {
                    testSent = true
                }
            } else {
                print("Notification permission denied")
            }
        }
    }
}

private struct ReminderRow: View {
    let reminder: ReminderUi
    @Binding var isEnabled: Bool
    
    var body: some View {
        LpmCard {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Text(reminder.title)
                            .font(.headline)
                            .foregroundColor(isEnabled ? .black : .gray)
                        
                        Text(reminder.time)
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .foregroundColor(isEnabled ? Color(UIColor.darkGray) : .gray)
                    }
                    
                    Text(reminder.description)
                        .font(.caption)
                        .foregroundColor(.gray)
                }
                
                Spacer()
                
                Toggle("", isOn: $isEnabled)
                    .labelsHidden()
                    .tint(.black)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
        }
    }
}
