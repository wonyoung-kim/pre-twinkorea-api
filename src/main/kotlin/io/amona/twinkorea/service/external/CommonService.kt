package io.amona.twinkorea.service.external

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.amona.twinkorea.configuration.AppConfig
import io.amona.twinkorea.exception.AuthenticationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.springframework.stereotype.Service

@Service
class CommonService(
    private val appConfig: AppConfig,
    private val okHttpClient: OkHttpClient,
    ) {

    val apikey = appConfig.apikey
    val host = appConfig.host

    /**
     * http 응답 코드 해석후 200 아닐경우 에러 throw
     */
    internal fun checkStatusCodeIsOk(httpResponse: Response) {
        if (httpResponse.code != 200) {
            val objectMapper = ObjectMapper()
            val responseBody: String = httpResponse.body!!.string()
            val responseBodyObject: JsonNode = objectMapper.readTree(responseBody)
            println(responseBodyObject)
            val message = responseBodyObject.get("message")?.toString()
            throw AuthenticationException(message ?: "인증서버로 부터 정상적인 결과를 받아오지 못했습니다. \n $responseBodyObject")
        }
    }

    internal fun connSiksinApiPost(requestBody: String, url: String, subject: String): String {

        appConfig.logger.info{"[식신 API] $subject 요청 Body : $requestBody "}
        val httpResponse = okHttpClient.newCall(
            Request.Builder().url("${host}/${url}")
                .addHeader(name = "siksinOauth", value = apikey)
                .post(body=requestBody.toRequestBody("application/json; charset=utf-8".toMediaType())).build()
        ).execute()

        try {
            checkStatusCodeIsOk(httpResponse)
            val responseBody = httpResponse.body!!.string()
            return if (responseBody.isNotEmpty()) {
                appConfig.logger.info{"[식신 API] $subject 응답 Body : $responseBody"}
                responseBody
            } else {
                appConfig.logger.info{"[식신 API] $subject 응답 Code : ${httpResponse.code}"}
                httpResponse.code.toString()
            }
        }
        finally {
            httpResponse.body!!.close()
        }
    }

    internal fun connSiksinApiGet(params: String, url: String, subject: String): String {
        val requestUrl = if (params == "") {
            "${host}${url}"
        } else {
            "${host}${url}?${params}"
        }
        appConfig.logger.info{"[식신 API] $subject 요청 url : $requestUrl"}
        val httpResponse = okHttpClient.newCall(
            Request.Builder().url(requestUrl)
                .addHeader(name = "siksinOauth", value = apikey)
                .build()
        ).execute()
        try {
            checkStatusCodeIsOk(httpResponse)
            val response = httpResponse.body!!.string()
            appConfig.logger.info{"[식신 API] $subject 응답 Body : $response"}
            return response
        } finally {
            httpResponse.body!!.close()
        }
    }
}
