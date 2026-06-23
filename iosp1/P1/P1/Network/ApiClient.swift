import Foundation

enum ApiError: Error {
    case invalidURL
    case serverError(statusCode: Int)
    case decodingError(Error)
    case underlying(Error)
}

class ApiClient {
    static let shared = ApiClient()
    
    private let baseURL = URL(string: "https://living-patient-memory-api-772480669824.us-central1.run.app/")!
    private let session: URLSession
    
    private init() {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 60
        config.timeoutIntervalForResource = 60
        self.session = URLSession(configuration: config)
    }
    
    private func createRequest(path: String, method: String = "GET", body: Data? = nil) -> URLRequest {
        let url = baseURL.appendingPathComponent(path)
        var request = URLRequest(url: url)
        request.httpMethod = method
        
        if let token = SessionManager.shared.getToken() {
            request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        if let body = body {
            request.httpBody = body
            request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        }
        
        return request
    }
    
    func performRequest<T: Decodable>(path: String, method: String = "GET", body: Encodable? = nil) async throws -> T {
        var bodyData: Data? = nil
        if let body = body {
            bodyData = try JSONEncoder().encode(body)
        }
        
        let request = createRequest(path: path, method: method, body: bodyData)
        
        let (data, response) = try await session.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw ApiError.invalidURL
        }
        
        guard (200...299).contains(httpResponse.statusCode) else {
            throw ApiError.serverError(statusCode: httpResponse.statusCode)
        }
        
        do {
            let decoder = JSONDecoder()
            // Configure decoder if needed (e.g. date formats)
            return try decoder.decode(T.self, from: data)
        } catch {
            throw ApiError.decodingError(error)
        }
    }
    
    func performRequest(path: String, method: String = "GET", body: Encodable? = nil) async throws {
        var bodyData: Data? = nil
        if let body = body {
            bodyData = try JSONEncoder().encode(body)
        }
        
        let request = createRequest(path: path, method: method, body: bodyData)
        
        let (_, response) = try await session.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw ApiError.invalidURL
        }
        
        guard (200...299).contains(httpResponse.statusCode) else {
            throw ApiError.serverError(statusCode: httpResponse.statusCode)
        }
    }
}
