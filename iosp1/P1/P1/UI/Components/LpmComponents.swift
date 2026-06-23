import SwiftUI

struct LpmSectionTitle: View {
    let title: String
    
    var body: some View {
        Text(title)
            .font(.title2)
            .fontWeight(.bold)
            .foregroundColor(.black)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct LpmBodyText: View {
    let text: String
    
    var body: some View {
        Text(text)
            .font(.body)
            .foregroundColor(Color(UIColor.systemGray))
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct LpmCard<Content: View>: View {
    let content: Content
    
    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }
    
    var body: some View {
        content
            .background(Color.white)
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Color(UIColor.systemGray5), lineWidth: 1)
            )
    }
}

struct LpmTopBar: View {
    let title: String
    let onBack: () -> Void
    
    var body: some View {
        HStack {
            Button(action: onBack) {
                Image(systemName: "chevron.left")
                    .font(.title3.weight(.semibold))
                    .foregroundColor(.black)
            }
            Spacer()
            Text(title)
                .font(.headline)
                .fontWeight(.bold)
            Spacer()
            // Placeholder for symmetry
            Image(systemName: "chevron.left").opacity(0)
        }
        .padding()
        .background(Color.white)
    }
}

struct LpmStepIndicator: View {
    let currentStep: Int
    let totalSteps: Int
    
    var body: some View {
        HStack(spacing: 8) {
            ForEach(1...totalSteps, id: \.self) { step in
                Rectangle()
                    .fill(step <= currentStep ? Color.black : Color(UIColor.systemGray5))
                    .frame(height: 4)
                    .cornerRadius(2)
            }
        }
    }
}
