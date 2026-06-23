import SwiftUI

struct JourneyScreen: View {
    let followUp: FollowUpUi
    let onOpenDrawer: () -> Void
    let onOpenReport: () -> Void
    let onStartRoutine: () -> Void

    @State private var events: [TimelineEventResponse] = []
    @State private var chatText = ""
    @State private var isSending = false
    @State private var currentFollowUp: FollowUpUi
    
    // Menu & dialogs
    @State private var showDatePicker = false
    @State private var showDeleteConfirm = false
    @State private var pickedDate: Date = Date()
    
    // Delete event
    @State private var eventToDelete: TimelineEventResponse? = nil
    @State private var showDeleteEventConfirm = false
    
    // Missed measurement
    @State private var showMissedForm = false
    @State private var missedEffectiveDate: Date? = nil

    init(followUp: FollowUpUi, onOpenDrawer: @escaping () -> Void, onOpenReport: @escaping () -> Void, onStartRoutine: @escaping () -> Void) {
        self.followUp = followUp
        self.onOpenDrawer = onOpenDrawer
        self.onOpenReport = onOpenReport
        self.onStartRoutine = onStartRoutine
        _currentFollowUp = State(initialValue: followUp)
    }

    // Compute % complete from real events
    private var completionPercent: Int {
        let expected = max(1, currentFollowUp.totalDays)
        let actual = events.filter { $0.type == "user" && !$0.date_label.contains("Question") }.count
        return min(Int((Float(actual) / Float(expected)) * 100), 100)
    }
    
    private var appointmentDateString: String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let d = formatter.date(from: currentFollowUp.expiresAt) {
            let df = DateFormatter()
            df.dateFormat = "d MMM"
            return df.string(from: d)
        }
        return "—"
    }
    
    private var startDate: Date {
        let fmt = ISO8601DateFormatter()
        fmt.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return fmt.date(from: currentFollowUp.startsAt) ?? Date()
    }
    
    // Build mixed timeline: past events + future/past day placeholders
    private enum TimelineItem: Identifiable {
        case event(TimelineEventResponse)
        case futureDay(dayNumber: Int, date: Date, isPast: Bool)
        var id: String {
            switch self {
            case .event(let e): return "event-\(e.id)"
            case .futureDay(let d, _, _): return "day-\(d)"
            }
        }
    }
    
    private var timelineItems: [TimelineItem] {
        var items: [TimelineItem] = []
        // Past events (user + ai pairs)
        var i = 0
        let sorted = events.sorted { ($0.effective_at ?? $0.created_at) < ($1.effective_at ?? $1.created_at) }
        while i < sorted.count {
            items.append(.event(sorted[i]))
            i += 1
        }
        // Future & past days without data
        let daysDone = currentFollowUp.totalDays - currentFollowUp.daysRemaining
        let startFuture = max(daysDone + 1, 1)
        let cal = Calendar.current
        for d in startFuture...max(startFuture, currentFollowUp.totalDays) {
            let dayDate = cal.date(byAdding: .day, value: d - 1, to: startDate) ?? Date()
            let isPast = dayDate < Date()
            items.append(.futureDay(dayNumber: d, date: dayDate, isPast: isPast))
        }
        return items
    }

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
                    Spacer()
                    Text(currentFollowUp.title)
                        .font(.headline)
                        .fontWeight(.bold)
                    Spacer()
                    // Report + overflow menu
                    HStack(spacing: 4) {
                        Button(action: onOpenReport) {
                            Image(systemName: "doc.text")
                                .font(.title3)
                                .foregroundColor(.black)
                        }
                        Menu {
                            Button("Change appointment date") {
                                // Init picker to current expiry
                                let fmt = ISO8601DateFormatter()
                                fmt.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
                                pickedDate = fmt.date(from: currentFollowUp.expiresAt) ?? Date()
                                showDatePicker = true
                            }
                            Button(role: .destructive) {
                                showDeleteConfirm = true
                            } label: {
                                Label("Delete tracking", systemImage: "trash")
                            }
                        } label: {
                            Image(systemName: "ellipsis")
                                .font(.title3)
                                .foregroundColor(.black)
                                .padding(.leading, 4)
                        }
                    }
                }
                .padding()
                .background(Color.white)

                ScrollView {
                    VStack(spacing: 0) {
                        // Summary Card (dynamic)
                        JourneySummaryCard(
                            followUp: currentFollowUp,
                            completionPercent: completionPercent,
                            appointmentDateStr: appointmentDateString
                        )
                        .padding(.vertical, 16)

                        // Timeline: events + future/past day rows
                        if timelineItems.isEmpty {
                            EmptyStateTimeline()
                        } else {
                            VStack(spacing: 0) {
                                ForEach(timelineItems) { item in
                                    switch item {
                                    case .event(let event):
                                        TimelineEventRow(
                                            event: event,
                                            isLast: false,
                                            onLongPress: {
                                                if event.type == "user" {
                                                    eventToDelete = event
                                                    showDeleteEventConfirm = true
                                                }
                                            }
                                        )
                                    case .futureDay(let day, let date, let isPast):
                                        FutureDayRow(
                                            dayNumber: day,
                                            date: date,
                                            isPast: isPast,
                                            onAddMissed: isPast ? {
                                                missedEffectiveDate = date
                                                withAnimation { showMissedForm = true }
                                            } : nil
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                BottomInput(
                    text: $chatText,
                    isSending: isSending,
                    onStartRoutine: onStartRoutine,
                    onSendQuestion: {
                        guard !chatText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
                        Task { await sendQuestion() }
                    }
                )
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            
            // Missed Measurement bottom sheet
            if showMissedForm {
                Color.black.opacity(0.4)
                    .edgesIgnoringSafeArea(.all)
                    .onTapGesture { showMissedForm = false }
                
                VStack {
                    Spacer()
                    MissedMeasurementForm(
                        effectiveDate: missedEffectiveDate ?? Date(),
                        followUpId: currentFollowUp.id,
                        onClose: {
                            showMissedForm = false
                            Task { await reloadTimeline() }
                        }
                    )
                }
                .edgesIgnoringSafeArea(.bottom)
                .transition(.move(edge: .bottom))
                .animation(.spring(response: 0.35), value: showMissedForm)
            }
        }
        .task {
            await reloadTimeline()
        }
        // Auto-refresh every 3s
        .task {
            do {
                while !Task.isCancelled {
                    try await Task.sleep(nanoseconds: 3_000_000_000)
                    if Task.isCancelled { break }
                    await reloadTimeline()
                }
            } catch {
                // Task cancelled
            }
        }
        // Date picker sheet
        .sheet(isPresented: $showDatePicker) {
            NavigationView {
                VStack {
                    DatePicker(
                        "New appointment date",
                        selection: $pickedDate,
                        displayedComponents: [.date]
                    )
                    .datePickerStyle(.graphical)
                    .tint(.black)
                    .padding()
                    Spacer()
                }
                .navigationTitle("Change appointment date")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") { showDatePicker = false }
                    }
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Confirm") {
                            showDatePicker = false
                            Task { await changeAppointmentDate(to: pickedDate) }
                        }
                        .foregroundColor(.black)
                        .fontWeight(.bold)
                    }
                }
            }
        }
        // Delete tracking confirm
        .alert("Delete Tracking", isPresented: $showDeleteConfirm) {
            Button("Delete", role: .destructive) {
                Task { await deleteTracking() }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This will permanently delete this tracking and all its data. This cannot be undone.")
        }
        // Delete event confirm
        .alert("Delete Record", isPresented: $showDeleteEventConfirm) {
            Button("Delete", role: .destructive) {
                if let ev = eventToDelete {
                    Task { await deleteEvent(ev.id) }
                }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Are you sure you want to delete this record?")
        }
    }

    private func reloadTimeline() async {
        do {
            let fetched = try await ApiService.shared.getTimeline(subscriptionId: currentFollowUp.id)
            await MainActor.run {
                events = fetched.sorted { ($0.effective_at ?? $0.created_at) < ($1.effective_at ?? $1.created_at) }
            }
        } catch {
            print("Timeline fetch failed: \(error)")
        }
    }

    private func sendQuestion() async {
        isSending = true
        do {
            let request = TimelineEventRequest(content: chatText, date_label: "Question", effective_date: nil)
            let newEvent = try await ApiService.shared.postTimelineEvent(subscriptionId: currentFollowUp.id, request: request)
            await MainActor.run {
                events.append(newEvent)
                chatText = ""
            }
        } catch {
            print("Failed to send question: \(error)")
        }
        isSending = false
    }
    
    private func deleteEvent(_ id: String) async {
        do {
            try await ApiService.shared.deleteTimelineEvent(subscriptionId: currentFollowUp.id, eventId: id)
            await reloadTimeline()
        } catch {
            print("Delete event failed: \(error)")
        }
    }
    
    private func changeAppointmentDate(to date: Date) async {
        let fmt = DateFormatter()
        fmt.dateFormat = "yyyy-MM-dd"
        let dateStr = fmt.string(from: date)
        do {
            _ = try await ApiService.shared.patchSubscription(
                id: currentFollowUp.id,
                request: UpdateSubscriptionRequest(expires_at: dateStr)
            )
            // Re-fetch to update
            let subs = try await ApiService.shared.getSubscriptions()
            let agents = try await ApiService.shared.getAgents()
            if let sub = subs.first(where: { $0.id == currentFollowUp.id }) {
                let agent = agents.first(where: { $0.id == sub.agent_id })
                let title = agent?.name ?? currentFollowUp.title
                let isoFmt = ISO8601DateFormatter()
                isoFmt.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
                let start = isoFmt.date(from: sub.starts_at) ?? Date()
                let end = isoFmt.date(from: sub.expires_at) ?? date
                let totalSec = end.timeIntervalSince(start)
                let elapsed = Date().timeIntervalSince(start)
                let progress = max(0, min(1, Float(elapsed / totalSec)))
                let daysLeft = max(0, Int(end.timeIntervalSince(Date()) / 86400))
                await MainActor.run {
                    currentFollowUp = FollowUpUi(
                        id: sub.id, title: title,
                        daysRemaining: daysLeft, totalDays: Int(totalSec / 86400),
                        progress: progress, isActive: daysLeft > 0,
                        startsAt: sub.starts_at, expiresAt: sub.expires_at,
                        rules: sub.parameters?.rules
                    )
                }
            }
        } catch {
            print("Change appointment failed: \(error)")
        }
    }
    
    private func deleteTracking() async {
        do {
            try await ApiService.shared.deleteSubscription(id: currentFollowUp.id)
            await MainActor.run { onOpenDrawer() }
        } catch {
            print("Delete tracking failed: \(error)")
        }
    }
}

// MARK: - Future / Past Day Row

private struct FutureDayRow: View {
    let dayNumber: Int
    let date: Date
    let isPast: Bool
    let onAddMissed: (() -> Void)?

    private var dateLabel: String {
        let df = DateFormatter()
        df.dateFormat = "EEEE, MMM d"
        return df.string(from: date)
    }

    var body: some View {
        HStack(spacing: 12) {
            // Central dot on line
            VStack(spacing: 0) {
                Rectangle()
                    .fill(Color(UIColor.systemGray4))
                    .frame(width: 2, height: 20)
                Circle()
                    .strokeBorder(Color(UIColor.systemGray4), lineWidth: 2)
                    .frame(width: 10, height: 10)
                Rectangle()
                    .fill(Color(UIColor.systemGray4))
                    .frame(width: 2, height: 20)
            }
            .padding(.leading, 35)

            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Day \(dayNumber) — \(dateLabel)")
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundColor(isPast ? Color(UIColor.systemGray) : Color(UIColor.systemGray3))
                    Text("Scheduled tracking")
                        .font(.caption2)
                        .foregroundColor(Color(UIColor.systemGray4))
                }
                Spacer()
                if isPast, let onAdd = onAddMissed {
                    Button(action: onAdd) {
                        Text("+ Add")
                            .font(.caption)
                            .fontWeight(.bold)
                            .foregroundColor(.black)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 5)
                            .background(Color(UIColor.systemGray6))
                            .cornerRadius(8)
                    }
                }
            }
            .padding(.vertical, 8)
            .padding(.trailing, 20)
            .opacity(isPast ? 0.7 : 0.4)
        }
    }
}

// MARK: - Summary Card (dynamic)


private struct JourneySummaryCard: View {
    let followUp: FollowUpUi
    let completionPercent: Int
    let appointmentDateStr: String

    var body: some View {
        LpmCard {
            VStack(spacing: 16) {
                HStack {
                    VStack {
                        Text("Appt Date")
                            .font(.caption)
                            .foregroundColor(.gray)
                        Text(appointmentDateStr)
                            .font(.title3)
                            .fontWeight(.bold)
                    }
                    Spacer()
                    VStack {
                        Text("Days left")
                            .font(.caption)
                            .foregroundColor(.gray)
                        Text("\(followUp.daysRemaining)")
                            .font(.title3)
                            .fontWeight(.bold)
                    }
                    Spacer()
                    VStack {
                        Text("Complete")
                            .font(.caption)
                            .foregroundColor(.gray)
                        Text("\(completionPercent)%")
                            .font(.title3)
                            .fontWeight(.bold)
                    }
                }

                GeometryReader { geometry in
                    ZStack(alignment: .leading) {
                        Capsule().frame(height: 8).foregroundColor(Color(UIColor.systemGray5))
                        Capsule()
                            .frame(width: geometry.size.width * CGFloat(followUp.progress), height: 8)
                            .foregroundColor(.black)
                    }
                }
                .frame(height: 8)
            }
            .padding()
        }
        .padding(.horizontal, 20)
    }
}

private struct EmptyStateTimeline: View {
    var body: some View {
        Text("Your personalized protocol has started. Let's begin your first measurement.")
            .font(.body)
            .foregroundColor(.gray)
            .multilineTextAlignment(.center)
            .padding()
            .background(Color.white)
            .cornerRadius(12)
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(UIColor.systemGray5), lineWidth: 1))
            .padding(.horizontal, 20)
            .padding(.top, 24)
    }
}

private struct TimelineEventRow: View {
    let event: TimelineEventResponse
    let isLast: Bool
    let onLongPress: () -> Void

    private var timeLabel: String {
        let fmt = ISO8601DateFormatter()
        fmt.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let d = fmt.date(from: event.effective_at ?? event.created_at) {
            let df = DateFormatter()
            df.dateFormat = "HH:mm"
            return df.string(from: d)
        }
        return ""
    }

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(spacing: 0) {
                ZStack {
                    Circle()
                        .fill(event.type == "ai" ? Color.blue : Color.black)
                        .frame(width: 32, height: 32)
                    Image(systemName: event.type == "ai" ? "sparkles" : "person.fill")
                        .foregroundColor(.white)
                        .font(.system(size: 14, weight: .bold))
                }
                if !isLast {
                    Rectangle()
                        .fill(Color(UIColor.systemGray4))
                        .frame(width: 2)
                        .padding(.vertical, 4)
                }
            }

            VStack(alignment: .leading, spacing: 6) {
                let label = event.date_label.uppercased()
                Text(timeLabel.isEmpty ? label : "\(label) • \(timeLabel)")
                    .font(.caption2)
                    .fontWeight(.bold)
                    .foregroundColor(.gray)

                Text(event.content)
                    .font(.subheadline)
                    .padding(14)
                    .background(event.type == "ai" ? Color.blue.opacity(0.1) : Color(UIColor.systemGray6))
                    .foregroundColor(.black)
                    .clipShape(
                        UnevenRoundedRectangle(
                            topLeadingRadius: event.type == "ai" ? 4 : 16,
                            bottomLeadingRadius: 16,
                            bottomTrailingRadius: 16,
                            topTrailingRadius: event.type == "ai" ? 16 : 4
                        )
                    )
                    .onLongPressGesture { onLongPress() }
            }
            .padding(.bottom, isLast ? 24 : 16)

            Spacer()
        }
        .padding(.horizontal, 20)
    }
}

private struct BottomInput: View {
    @Binding var text: String
    let isSending: Bool
    let onStartRoutine: () -> Void
    let onSendQuestion: () -> Void

    var body: some View {
        VStack {
            Button(action: onStartRoutine) {
                Text("📋 Fill measurements")
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.black)
                    .cornerRadius(12)
            }
            .padding(.horizontal)
            .padding(.bottom, 8)

            HStack {
                TextField("Ask a question...", text: $text)
                    .padding()
                    .background(Color(UIColor.systemGray6))
                    .cornerRadius(24)

                Button(action: onSendQuestion) {
                    if isSending {
                        ProgressView()
                    } else {
                        Image(systemName: "paperplane.fill")
                            .foregroundColor(.black)
                    }
                }
                .padding(.horizontal, 8)
            }
            .padding(.horizontal)
            .padding(.bottom, 16)
        }
        .background(Color.white.shadow(radius: 10))
    }
}

// MARK: - Missed Measurement Form

private struct MissedMeasurementForm: View {
    let effectiveDate: Date
    let followUpId: String
    let onClose: () -> Void
    
    @State private var painLevel: Double = 5
    @State private var tempValue = ""
    @State private var isSaving = false
    
    private var dateLabel: String {
        let df = DateFormatter()
        df.dateFormat = "EEEE, MMM d"
        return "Retroactive - \(df.string(from: effectiveDate))"
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            // Handle
            RoundedRectangle(cornerRadius: 3)
                .fill(Color(UIColor.systemGray4))
                .frame(width: 40, height: 5)
                .frame(maxWidth: .infinity)
                .padding(.top, 12)
            
            Text("Add missed measurement")
                .font(.title3)
                .fontWeight(.bold)
                .padding(.horizontal, 24)
            
            Text(dateLabel)
                .font(.subheadline)
                .foregroundColor(.gray)
                .padding(.horizontal, 24)
            
            VStack(alignment: .leading, spacing: 16) {
                Text("Pain level: \(Int(painLevel))/10")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                Slider(value: $painLevel, in: 0...10, step: 1).tint(.black)
                
                TextField("Temperature (°C) - optional", text: $tempValue)
                    .keyboardType(.decimalPad)
                    .padding()
                    .background(Color(UIColor.systemGray6))
                    .cornerRadius(8)
            }
            .padding(.horizontal, 24)
            
            if isSaving {
                ProgressView().frame(maxWidth: .infinity).padding()
            } else {
                Button(action: saveMissed) {
                    Text("Save")
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.black)
                        .cornerRadius(12)
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 32)
            }
        }
        .background(Color.white)
        .cornerRadius(24, corners: [.topLeft, .topRight])
    }
    
    private func saveMissed() {
        isSaving = true
        var lines = ["Routine Check-in (\(dateLabel)):"]
        lines.append("• Pain Level: \(Int(painLevel))/10")
        if !tempValue.isEmpty { lines.append("• Temperature: \(tempValue) °C") }
        let content = lines.joined(separator: "\n")
        
        let isoFmt = ISO8601DateFormatter()
        isoFmt.formatOptions = [.withInternetDateTime]
        let effectiveDateStr = isoFmt.string(from: effectiveDate)
        
        Task {
            do {
                _ = try await ApiService.shared.postTimelineEvent(
                    subscriptionId: followUpId,
                    request: TimelineEventRequest(
                        content: content,
                        date_label: dateLabel,
                        effective_date: effectiveDateStr
                    )
                )
                await MainActor.run { onClose() }
            } catch {
                print("Missed measurement save failed: \(error)")
                await MainActor.run { isSaving = false }
            }
        }
    }
}

// Helper extension for corner radius on specific corners
extension View {
    func cornerRadius(_ radius: CGFloat, corners: UIRectCorner) -> some View {
        clipShape(RoundedCorner(radius: radius, corners: corners))
    }
}

struct RoundedCorner: Shape {
    var radius: CGFloat = .infinity
    var corners: UIRectCorner = .allCorners
    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: corners,
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
    }
}
