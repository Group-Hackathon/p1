import SwiftUI

struct FollowUpUi: Identifiable, Equatable {
    let id: String
    let title: String
    let daysRemaining: Int
    let totalDays: Int
    let progress: Float
    let isActive: Bool
    let startsAt: String
    let expiresAt: String
    let rules: FollowUpRules?
    let schedule: [String: [String]]?
}

struct DashboardScreen: View {
    let onNewFollowUp: () -> Void
    let onOpenJourney: (FollowUpUi) -> Void
    let onOpenNotifications: () -> Void
    let onOpenDrawer: () -> Void
    
    @State private var followUps: [FollowUpUi] = []
    @State private var isLoading = true
    @State private var errorMessage: String? = nil
    
    var body: some View {
        ZStack {
            Color.white.edgesIgnoringSafeArea(.all)
            
            VStack(spacing: 0) {
                // Top Bar
                HStack {
                    Button(action: onOpenDrawer) {
                        Image(systemName: "line.3.horizontal")
                            .font(.title2)
                            .foregroundColor(.black)
                    }
                    
                    Text("P1")
                        .font(.title2)
                        .fontWeight(.bold)
                        .padding(.leading, 8)
                    
                    Spacer()
                    
                    Button(action: onOpenNotifications) {
                        Image(systemName: "bell")
                            .font(.title3)
                            .foregroundColor(.black)
                    }
                }
                .padding()
                .background(Color.white)
                
                ZStack {
                    if isLoading {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: .black))
                    .scaleEffect(1.5)
            } else if let error = errorMessage {
                VStack(spacing: 16) {
                    Text(error)
                        .foregroundColor(.red)
                        .multilineTextAlignment(.center)
                    LpmPrimaryButton(text: "Retry") {
                        Task { await loadData() }
                    }
                }
                .padding()
            } else if followUps.filter({ $0.isActive }).isEmpty {
                VStack(spacing: 16) {
                    Text(String(localized: "welcome_title"))
                        .font(.title2)
                        .fontWeight(.black)
                        .multilineTextAlignment(.center)
                    
                    Text(String(localized: "welcome_desc"))
                        .font(.body)
                        .foregroundColor(Color(UIColor.systemGray))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)
                    
                    Spacer().frame(height: 32)
                    
                    LpmPrimaryButton(text: String(localized: "start_new_tracking"), action: onNewFollowUp)
                }
                .padding()
            } else {
                ScrollView {
                    VStack(spacing: 0) {
                        ForEach(followUps.filter({ $0.isActive })) { followUp in
                            FollowUpRow(followUp: followUp)
                                .onTapGesture {
                                    onOpenJourney(followUp)
                                }
                            
                            Divider()
                                .background(Color(UIColor.systemGray5))
                                .padding(.horizontal, 24)
                        }
                    }
                        }
                        .padding(.vertical, 16)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        }
        .task {
            await loadData()
        }
    }
    
    private func loadData() async {
        isLoading = true
        errorMessage = nil
        
        do {
            if SessionManager.shared.getToken() == nil {
                errorMessage = "Unable to connect. Check your network."
                followUps = []
                isLoading = false
                return
            }
            
            // Try fetching from real API
            let subscriptions = try await ApiService.shared.getSubscriptions()
            let agents = try await ApiService.shared.getAgents()
            
            // Map them...
            self.followUps = subscriptions.map { sub in
                let agent = agents.first(where: { $0.id == sub.agent_id })
                let title = agent?.name ?? "Tracking from \(sub.starts_at.prefix(10))"
                
                // Simple parsing for dates
                let formatter = ISO8601DateFormatter()
                formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
                let start = formatter.date(from: sub.starts_at) ?? Date()
                let end = formatter.date(from: sub.expires_at) ?? Calendar.current.date(byAdding: .day, value: 14, to: start)!
                
                let totalSeconds = end.timeIntervalSince(start)
                let remainingSeconds = end.timeIntervalSince(Date())
                let elapsedSeconds = Date().timeIntervalSince(start)
                
                let progress = max(0, min(1, Float(elapsedSeconds / totalSeconds)))
                let daysRemaining = max(0, Int(remainingSeconds / 86400))
                
                return FollowUpUi(
                    id: sub.id,
                    title: title,
                    daysRemaining: daysRemaining,
                    totalDays: Int(totalSeconds / 86400),
                    progress: progress,
                    isActive: Date() < end,
                    startsAt: sub.starts_at,
                    expiresAt: sub.expires_at,
                    rules: sub.parameters?.rules,
                    schedule: sub.parameters?.schedule
                )
            }
            
        } catch {
            errorMessage = "Unable to load your follow-ups."
        }
        
        isLoading = false
    }
}

private struct FollowUpRow: View {
    let followUp: FollowUpUi
    
    var body: some View {
        HStack(spacing: 16) {
            Circle()
                .fill(Color(UIColor.systemGray5))
                .frame(width: 40, height: 40)
                .overlay(
                    Text(String(followUp.title.prefix(1)).uppercased())
                        .fontWeight(.bold)
                        .foregroundColor(Color(UIColor.systemGray))
                )
            
            VStack(alignment: .leading, spacing: 4) {
                Text(followUp.title)
                    .font(.headline)
                    .fontWeight(.semibold)
                    .foregroundColor(.black)
                
                Text("Active tracking protocol")
                    .font(.caption)
                    .foregroundColor(Color(UIColor.systemGray2))
            }
            
            Spacer()
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 16)
        .background(Color.white)
    }
}
