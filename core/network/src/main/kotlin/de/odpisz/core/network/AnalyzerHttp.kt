package de.odpisz.core.network

import de.odpisz.core.model.LetterAnalysis
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.url
import io.ktor.serialization.kotlinx.json.json

/**
 * HTTP client reserved for the backend phase. No API key lives in the app.
 * Phase 0 binds [FakeLetterAnalyzer] and never calls this client.
 */
object AnalyzerHttp {
    const val CONNECT_TIMEOUT_MS = 15_000L
    const val READ_TIMEOUT_MS = 60_000L

    fun client(baseUrl: String): HttpClient =
        HttpClient(OkHttp) {
            expectSuccess = true
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIMEOUT_MS
                requestTimeoutMillis = READ_TIMEOUT_MS
                socketTimeoutMillis = READ_TIMEOUT_MS
            }
            install(ContentNegotiation) {
                json(AnalysisJson.format)
            }
            if (baseUrl.isNotBlank()) {
                defaultRequest {
                    url(baseUrl)
                }
            }
        }
}

class UnconfiguredLetterAnalyzer(
    private val http: HttpClient,
) : LetterAnalyzer {
    override suspend fun analyze(maskedText: String): LetterAnalysis {
        http.close()
        error(
            "Backend nie jest skonfigurowany. Faza 0 używa FakeLetterAnalyzer i nie wysyła tekstu.",
        )
    }
}
