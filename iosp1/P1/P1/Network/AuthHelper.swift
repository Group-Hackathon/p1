import Foundation

class AuthHelper {
    static let shared = AuthHelper()
    
    private let devicePassword = "secret_device_password"
    
    private init() {}
    
    func ensureAuthenticated() async -> Bool {
        if SessionManager.shared.getToken() != nil {
            return true
        }
        
        let email = "\(SessionManager.shared.getOrCreateDeviceId())@local.device"
        let request = AuthRequest(email: email, password: devicePassword)
        
        do {
            let response = try await ApiService.shared.register(request: request)
            SessionManager.shared.saveToken(response.token)
            print("LPM_AUTH: Registered new device account")
            return true
        } catch {
            do {
                let response = try await ApiService.shared.login(request: request)
                SessionManager.shared.saveToken(response.token)
                print("LPM_AUTH: Logged in existing device account")
                return true
            } catch {
                print("LPM_AUTH: Authentication failed - \(error)")
                return false
            }
        }
    }
    
    func ensureProfile() async -> String? {
        if let profileId = SessionManager.shared.getProfileId() {
            return profileId
        }
        
        do {
            let profile = try await ApiService.shared.createProfile(
                request: ProfileRequest(first_name: "Patient", last_name: "Local", relation: "Self")
            )
            SessionManager.shared.saveProfileId(profile.id)
            return profile.id
        } catch {
            print("LPM_AUTH: Failed to create profile - \(error)")
            return nil
        }
    }
}
