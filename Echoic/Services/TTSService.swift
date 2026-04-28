import Foundation

// MARK: - TTS Service Protocol

protocol TTSServiceProtocol {
    func synthesize(text: String, model: TTSModel, voice: Voice, format: AudioFormat) async throws -> Data
}

// MARK: - TTS Service

class TTSService: TTSServiceProtocol {
    private let session: URLSession

    init(session: URLSession = .shared) {
        self.session = session
    }

    func testConnection(for provider: TTSProvider) async throws {
        switch provider {
        case .openai, .fishAudio:
            try await testOpenAICompatible(provider: provider)
        case .mimo:
            try await testMiMo()
        case .elevenlabs:
            try await testElevenLabs()
        case .google:
            try await testGoogle()
        case .azure:
            try await testAzure()
        case .edgeTTS:
            try await testEdgeTTS()
        }
    }

    func synthesize(text: String, model: TTSModel, voice: Voice, format: AudioFormat) async throws -> Data {
        guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            throw TTSError.emptyText
        }
        switch model.provider {
        case .openai, .fishAudio:
            return try await synthesizeOpenAICompatible(text: text, model: model, voice: voice, format: format, provider: model.provider)
        case .mimo:
            return try await synthesizeMiMo(text: text, voice: voice)
        case .elevenlabs:
            return try await synthesizeElevenLabs(text: text, model: model, voice: voice)
        case .google:
            return try await synthesizeGoogle(text: text, model: model, voice: voice)
        case .azure:
            return try await synthesizeAzure(text: text, voice: voice)
        case .edgeTTS:
            return try await synthesizeEdgeTTS(text: text, voice: voice)
        }
    }

    // MARK: - OpenAI Compatible (OpenAI / Fish Audio)

    private func testOpenAICompatible(provider: TTSProvider) async throws {
        let apiKey = try retrieveAPIKey(for: provider)
        let request = try buildOpenAIRequest(
            text: "Hi",
            model: provider.availableModels.first!,
            voice: provider.availableVoices.first!,
            format: .mp3,
            apiKey: apiKey,
            provider: provider
        )
        try await executeVoid(request)
    }

    private func synthesizeOpenAICompatible(text: String, model: TTSModel, voice: Voice, format: AudioFormat, provider: TTSProvider) async throws -> Data {
        let apiKey = try retrieveAPIKey(for: provider)
        let request = try buildOpenAIRequest(text: text, model: model, voice: voice, format: format, apiKey: apiKey, provider: provider)
        return try await executeData(request)
    }

    private func buildOpenAIRequest(text: String, model: TTSModel, voice: Voice, format: AudioFormat, apiKey: String, provider: TTSProvider) throws -> URLRequest {
        let storedBase = UserDefaults.standard.string(forKey: provider.baseURLStorageKey)
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
        let baseURL = (storedBase?.isEmpty ?? true) ? provider.defaultBaseURL : storedBase!

        let urlString = "\(baseURL)/v1/audio/speech"
        guard let url = URL(string: urlString) else {
            throw TTSError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let body: [String: String] = [
            "model": model.rawValue,
            "input": text,
            "voice": voice.id,
            "response_format": format.rawValue,
        ]
        request.httpBody = try JSONEncoder().encode(body)
        request.timeoutInterval = 60
        return request
    }

    // MARK: - ElevenLabs

    private func testElevenLabs() async throws {
        let apiKey = try retrieveAPIKey(for: .elevenlabs)
        let voice = TTSProvider.elevenlabs.availableVoices.first!
        let model = TTSProvider.elevenlabs.availableModels.first!
        let request = try buildElevenLabsRequest(text: "Hi", model: model, voice: voice, apiKey: apiKey)
        try await executeVoid(request)
    }

    private func synthesizeElevenLabs(text: String, model: TTSModel, voice: Voice) async throws -> Data {
        let apiKey = try retrieveAPIKey(for: .elevenlabs)
        let request = try buildElevenLabsRequest(text: text, model: model, voice: voice, apiKey: apiKey)
        return try await executeData(request)
    }

    private func buildElevenLabsRequest(text: String, model: TTSModel, voice: Voice, apiKey: String) throws -> URLRequest {
        let storedBase = UserDefaults.standard.string(forKey: TTSProvider.elevenlabs.baseURLStorageKey)
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
        let baseURL = (storedBase?.isEmpty ?? true) ? TTSProvider.elevenlabs.defaultBaseURL : storedBase!

        let urlString = "\(baseURL)/v1/text-to-speech/\(voice.id)"
        guard let url = URL(string: urlString) else {
            throw TTSError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue(apiKey, forHTTPHeaderField: "xi-api-key")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let body: [String: Any] = [
            "text": text,
            "model_id": model.rawValue,
        ]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        request.timeoutInterval = 60
        return request
    }

    // MARK: - Google Cloud TTS

    private func testGoogle() async throws {
        let apiKey = try retrieveAPIKey(for: .google)
        let request = try buildGoogleRequest(text: "Hi", model: .google_neural2, voice: TTSProvider.google.availableVoices.first!, apiKey: apiKey)
        try await executeVoid(request)
    }

    private func synthesizeGoogle(text: String, model: TTSModel, voice: Voice) async throws -> Data {
        let apiKey = try retrieveAPIKey(for: .google)
        let request = try buildGoogleRequest(text: text, model: model, voice: voice, apiKey: apiKey)
        let data = try await executeData(request)

        // Google returns base64-encoded audio in JSON
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let audioContent = json["audioContent"] as? String,
              let audioData = Data(base64Encoded: audioContent) else {
            throw TTSError.invalidResponse
        }
        return audioData
    }

    private func buildGoogleRequest(text: String, model: TTSModel, voice: Voice, apiKey: String) throws -> URLRequest {
        let storedBase = UserDefaults.standard.string(forKey: TTSProvider.google.baseURLStorageKey)
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
        let baseURL = (storedBase?.isEmpty ?? true) ? TTSProvider.google.defaultBaseURL : storedBase!

        let urlString = "\(baseURL)/v1/text:synthesize?key=\(apiKey)"
        guard let url = URL(string: urlString) else {
            throw TTSError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let body: [String: Any] = [
            "input": ["text": text],
            "voice": ["languageCode": "en-US", "name": voice.id],
            "audioConfig": ["audioEncoding": "MP3"],
        ]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        request.timeoutInterval = 60
        return request
    }

    // MARK: - Azure TTS

    private func testAzure() async throws {
        let apiKey = try retrieveAPIKey(for: .azure)
        let request = try buildAzureRequest(text: "Hi", voice: TTSProvider.azure.availableVoices.first!, apiKey: apiKey)
        try await executeVoid(request)
    }

    private func synthesizeAzure(text: String, voice: Voice) async throws -> Data {
        let apiKey = try retrieveAPIKey(for: .azure)
        let request = try buildAzureRequest(text: text, voice: voice, apiKey: apiKey)
        return try await executeData(request)
    }

    private func buildAzureRequest(text: String, voice: Voice, apiKey: String) throws -> URLRequest {
        let storedBase = UserDefaults.standard.string(forKey: TTSProvider.azure.baseURLStorageKey)
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
        let baseURL = (storedBase?.isEmpty ?? true) ? TTSProvider.azure.defaultBaseURL : storedBase!

        let urlString = "\(baseURL)/cognitiveservices/v1"
        guard let url = URL(string: urlString) else {
            throw TTSError.invalidURL
        }

        let ssml = """
        <speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'>
            <voice name='\(voice.id)'>\(escapeXML(text))</voice>
        </speak>
        """

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue(apiKey, forHTTPHeaderField: "Ocp-Apim-Subscription-Key")
        request.setValue("application/ssml+xml", forHTTPHeaderField: "Content-Type")
        request.setValue("audio-16khz-128kbitrate-mono-mp3", forHTTPHeaderField: "X-Microsoft-OutputFormat")
        request.setValue("Echoic", forHTTPHeaderField: "User-Agent")
        request.httpBody = ssml.data(using: .utf8)
        request.timeoutInterval = 60
        return request
    }

    // MARK: - MiMo TTS (Xiaomi)

    private func testMiMo() async throws {
        let apiKey = try retrieveAPIKey(for: .mimo)
        let request = try buildMiMoRequest(text: "Hi", voice: TTSProvider.mimo.availableVoices.first!, apiKey: apiKey)
        let data = try await executeData(request)
        // Verify response contains audio data
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let choices = json["choices"] as? [[String: Any]],
              !choices.isEmpty else {
            throw TTSError.invalidResponse
        }
    }

    private func synthesizeMiMo(text: String, voice: Voice) async throws -> Data {
        let apiKey = try retrieveAPIKey(for: .mimo)
        let request = try buildMiMoRequest(text: text, voice: voice, apiKey: apiKey)
        let data = try await executeData(request)

        // MiMo returns base64 audio inside chat completion response
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let choices = json["choices"] as? [[String: Any]],
              let message = choices.first?["message"] as? [String: Any],
              let audio = message["audio"] as? [String: Any],
              let audioBase64 = audio["data"] as? String,
              let audioData = Data(base64Encoded: audioBase64) else {
            throw TTSError.invalidResponse
        }
        return audioData
    }

    private func buildMiMoRequest(text: String, voice: Voice, apiKey: String) throws -> URLRequest {
        let storedBase = UserDefaults.standard.string(forKey: TTSProvider.mimo.baseURLStorageKey)
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
        let baseURL = (storedBase?.isEmpty ?? true) ? TTSProvider.mimo.defaultBaseURL : storedBase!

        let urlString = "\(baseURL)/v1/chat/completions"
        guard let url = URL(string: urlString) else {
            throw TTSError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue(apiKey, forHTTPHeaderField: "api-key")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let body: [String: Any] = [
            "model": "mimo-v2-tts",
            "messages": [
                ["role": "user", "content": "Please read the following text aloud."],
                ["role": "assistant", "content": text]
            ],
            "audio": [
                "format": "wav",
                "voice": voice.id
            ]
        ]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        request.timeoutInterval = 60
        return request
    }

    // MARK: - Edge TTS

    private func testEdgeTTS() async throws {
        // Edge TTS needs no key — just verify we can reach the token endpoint
        let request = URLRequest(url: URL(string: "https://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1")!)
        let (_, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse, http.statusCode < 500 else {
            throw TTSError.serverError((response as? HTTPURLResponse)?.statusCode ?? 0)
        }
    }

    private func synthesizeEdgeTTS(text: String, voice: Voice) async throws -> Data {
        // Edge TTS uses WebSocket-like protocol via HTTP multipart streaming
        // We use the public web endpoint with SSML
        let ssml = """
        <speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'>
            <voice name='\(voice.id)'>\(escapeXML(text))</voice>
        </speak>
        """

        // First get a token
        let tokenURL = URL(string: "https://edge.api.speech.microsoft.com/cognitiveservices/v1?language=en-US")!
        var tokenRequest = URLRequest(url: tokenURL)
        tokenRequest.httpMethod = "POST"
        tokenRequest.setValue("application/ssml+xml", forHTTPHeaderField: "Content-Type")
        tokenRequest.setValue("audio-16khz-128kbitrate-mono-mp3", forHTTPHeaderField: "X-Microsoft-OutputFormat")
        tokenRequest.httpBody = ssml.data(using: .utf8)
        tokenRequest.timeoutInterval = 60

        return try await executeData(tokenRequest)
    }

    // MARK: - Helpers

    private func retrieveAPIKey(for provider: TTSProvider) throws -> String {
        guard let value = UserDefaults.standard.string(forKey: provider.apiKeyStorageKey),
              !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            throw TTSError.missingAPIKey
        }
        return value.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private func executeVoid(_ request: URLRequest) async throws {
        let (data, response) = try await session.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw TTSError.invalidResponse
        }
        switch httpResponse.statusCode {
        case 200...299: return
        case 401: throw TTSError.unauthorized
        case 429: throw TTSError.rateLimited
        default:
            if let body = String(data: data, encoding: .utf8) {
                throw TTSError.clientError("HTTP \(httpResponse.statusCode): \(body)")
            }
            throw TTSError.serverError(httpResponse.statusCode)
        }
    }

    private func executeData(_ request: URLRequest) async throws -> Data {
        let (data, response) = try await session.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw TTSError.invalidResponse
        }
        switch httpResponse.statusCode {
        case 200...299: return data
        case 401: throw TTSError.unauthorized
        case 429: throw TTSError.rateLimited
        case 400...499:
            if let body = String(data: data, encoding: .utf8) {
                throw TTSError.clientError("Request failed (\(httpResponse.statusCode)): \(body)")
            }
            throw TTSError.clientError("Request failed with status \(httpResponse.statusCode)")
        default:
            throw TTSError.serverError(httpResponse.statusCode)
        }
    }

    private func escapeXML(_ string: String) -> String {
        string
            .replacingOccurrences(of: "&", with: "&amp;")
            .replacingOccurrences(of: "<", with: "&lt;")
            .replacingOccurrences(of: ">", with: "&gt;")
            .replacingOccurrences(of: "'", with: "&apos;")
            .replacingOccurrences(of: "\"", with: "&quot;")
    }
}

// MARK: - TTS Errors

enum TTSError: LocalizedError {
    case emptyText
    case missingAPIKey
    case invalidURL
    case invalidResponse
    case unauthorized
    case rateLimited
    case clientError(String)
    case serverError(Int)
    case playbackFailed(String)

    var errorDescription: String? {
        switch self {
        case .emptyText:
            "Please enter some text to synthesize."
        case .missingAPIKey:
            "API key is not configured. Please set it in Settings."
        case .invalidURL:
            "Invalid API URL."
        case .invalidResponse:
            "Received an invalid response from the server."
        case .unauthorized:
            "Invalid API key. Please check your API key in Settings."
        case .rateLimited:
            "Too many requests. Please wait a moment and try again."
        case .clientError(let message):
            message
        case .serverError(let code):
            "Server error (\(code)). Please try again later."
        case .playbackFailed(let message):
            "Audio playback failed: \(message)"
        }
    }
}
