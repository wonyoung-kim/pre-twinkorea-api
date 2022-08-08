package io.amona.twinkorea.service.external

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.amona.twinkorea.configuration.AppConfig
import io.amona.twinkorea.enums.SnsProvider
import io.amona.twinkorea.exception.MsgException
import io.amona.twinkorea.exception.NotFoundException
import io.amona.twinkorea.repository.UserRepository
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val commonService: CommonService,
    private val userRepo: UserRepository,
    private val okHttpClient: OkHttpClient,
    private val objectMapper: ObjectMapper,
    private val appConfig: AppConfig
) {
    /**
     * SNS Access_token 을 가지고, SNS 인증업체에서 식신 어플리케이션에 정보를 넘겨주는 회원의 SNS 고유 식별 ID를 가져온다.
     */
    fun getSnsId(snsProvider: SnsProvider, userSnsKey: String): String {
        val requestUrl: String
        val headerKey: String
        val headerValue: String
        val sns: String
        when (snsProvider) {
            SnsProvider.S -> {
                requestUrl = "https://kapi.kakao.com/v1/user/access_token_info"
                headerKey = "Authorization"
                headerValue = "Bearer $userSnsKey"
                sns = "KAKAO"
            }
            SnsProvider.N -> {
                requestUrl = "https://openapi.naver.com/v1/nid/me"
                headerKey = "Authorization"
                headerValue = "Bearer $userSnsKey"
                sns = "NAVER"
            }
            // TODO
            else -> throw Exception("카카오, 네이버 인증 외에는 아직 구현되지 않았습니다.")
        }
        appConfig.logger.info{"[$sns API] 인증 요청 요청"}
        val httpResponse = okHttpClient.newCall(
            Request.Builder().url(requestUrl).addHeader(name=headerKey, value=headerValue)
                .get().build()
        ).execute()
        try {
            commonService.checkStatusCodeIsOk(httpResponse)
            appConfig.logger.info{"[$sns API] 인증 요청 완료"}
            val responseBody: String = httpResponse.body!!.string()
            val node: JsonNode = objectMapper.readTree(responseBody)
            return node.get("id").asText()
        }
        finally {
            httpResponse.body!!.close()
        }
    }

    /**
     * SNS_ID를 unlink 합니다.
     */
    fun unlinkSns(snsProvider: SnsProvider, userId: Long): Boolean {
        val requestUrl: String
        val headerKey: String
        val headerValue: String
        val sns: String
        val user = userRepo.findByIdAndDeactivateIsFalse(userId) ?: throw NotFoundException("입력된 정보와 일치하는 회원을 찾을 수 없습니다.")
        when (snsProvider) {
            SnsProvider.S -> {
                requestUrl = "https://kapi.kakao.com/v1/user/unlink"
                headerKey = "Authorization"
                headerValue = "KakaoAK 01797c0483191bc2d9acb25cbf25d626"
                sns = "KAKAO"
                val requestBody = FormBody.Builder()
                    .add("target_id_type", "user_id")
                    .add("target_id", user.snsId).build()
                val httpResponse = okHttpClient.newCall(
                    Request.Builder().url(requestUrl).addHeader(name = headerKey, value = headerValue)
                        .post(requestBody).build()).execute()
                try {
                    commonService.checkStatusCodeIsOk(httpResponse)
                    appConfig.logger.info{"[$sns API] 회원#${userId}에 대한 인증정보 연결 끊기 요청 완료"}
                    return true
                }
                finally {
                    httpResponse.body!!.close()
                }
            }
            else -> {
                throw MsgException("카카오만 지원")
            }
        }
    }
}
