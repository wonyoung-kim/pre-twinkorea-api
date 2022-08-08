package io.amona.twinkorea.service.external

import com.fasterxml.jackson.databind.ObjectMapper
import io.amona.twinkorea.configuration.AppConfig
import io.amona.twinkorea.domain.PaymentLog
import io.amona.twinkorea.exception.NotFoundException
import io.amona.twinkorea.repository.PaymentLogRepository
import io.amona.twinkorea.request.DataEncRequestForMyAccount
import io.amona.twinkorea.request.MyAccountPaymentRequest
import io.amona.twinkorea.response.MyAccountDataEncResponse
import io.amona.twinkorea.response.SettleBankResponse
import io.amona.twinkorea.service.UserService
import io.amona.twinkorea.utils.AES256EncDec
import io.amona.twinkorea.utils.SHA256Encrypt
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.simple.JSONObject
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class SettlebankService (
    private val repo: PaymentLogRepository,
    private val userService: UserService,
    private val appConfig: AppConfig,
    private val okHttpClient: OkHttpClient,
    private val objectMapper: ObjectMapper,
) {
    private val myAccountApproveUrl = appConfig.myAccountApproveUrl
    private val myAccountCancelUrl = appConfig.myAccountCancelUrl
    private val mercntIdMyAccount = appConfig.stMercntIdMyAccount
    private val authKeyMyAccount = appConfig.stAuthKeyMyAccount
    private val mercntIdPg = appConfig.stMercntIdPg
    private val hashAuthKeyPg = appConfig.stHashAuthKeyPg
    private val prvcAuthKeyPg = appConfig.stPrvcAuthKeyPg

    fun readPaymentLog(trNo: String): PaymentLog {
        return repo.findByTrNo(trNo) ?: throw NotFoundException("요청하신 PamyentId 로 결과값을 찾아낼 수 없습니다.")
    }

    /**
     * 세틀뱅크에 결제 승인 요청
     */
    fun paymentRequest(myAccountPaymentRequest: MyAccountPaymentRequest, userId: Long): SettleBankResponse {
        val targetUrl = myAccountApproveUrl

        // API 호출 용
        val now = LocalDateTime.now().plusHours(9)
        val hdInfo = "IA_APPROV_1.0_1.0"
        val apiVer = "2.0"
        val authNo = myAccountPaymentRequest.authNo
        val reqDay = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val reqTime = now.format(DateTimeFormatter.ofPattern("HHmmss"))
        val signature = SHA256Encrypt.encryptSHA256HexEncoding(mercntIdMyAccount + authNo + reqDay + reqTime + authKeyMyAccount)

        appConfig.logger.info {
            "[SETTLEBANK API] 결제 승인 요청 : \n" +
                    "authNo: ${authNo}\n" +
                    "reqDay: ${reqDay}\n" +
                    "reqTime: ${reqTime}\n" +
                    "=====================\n" +
                    "signature: $signature"
        }

        // DB 기록용
        val ordNo = myAccountPaymentRequest.ordNo

        // http 요청용 JSON 데이터
        val jsonObject = JSONObject()
        jsonObject["hdInfo"] = hdInfo
        jsonObject["apiVer"] = apiVer
        jsonObject["mercntId"] = mercntIdMyAccount
        jsonObject["authNo"] = authNo
        jsonObject["reqDay"] = reqDay
        jsonObject["reqTime"] = reqTime
        jsonObject["signature"] = signature
        val body = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        // http 요청
        val httpResponse = okHttpClient.newCall(
            Request.Builder().url(targetUrl)
                .addHeader("Content-Type", "application/json")
                .post(body).build()
        ).execute()
        val responseBody = httpResponse.body!!.string()
        val node = objectMapper.readTree(responseBody)
        appConfig.logger.info { "[TWINKOREA] 유저#${userId} 결제 승인 요청 결과 \n $node" }
        val resultCd = node.get("resultCd").toString().replace("\"","")
        val errCd = node.get("errCd").toString().replace("\"","")
        val resultMsg = node.get("resultMsg").toString().replace("\"","")
        val trNo = try {
            node.get("trNo").toString().replace("\"","")
        } catch (e: NullPointerException) {
            null
        }
        val trPrice = try {
            node.get("trPrice").toString().replace("\"","")
        } catch (e: java.lang.NullPointerException) {
            null
        }

        return when (resultCd) {
            "0" -> {
                SettleBankResponse(
                    resultIsSuccess = true,
                    trNo = trNo,
                    ordNo = ordNo,
                    trPrice = trPrice,
                    resultCd = resultCd,
                    errCd = errCd,
                    resultMsg = resultMsg,
                )
            }
            else -> {
                SettleBankResponse(
                    resultIsSuccess = false,
                    resultCd = resultCd,
                    errCd = errCd,
                    resultMsg = resultMsg,
                )
            }
        }
    }

    /**
     * 세틀뱅크에 결제 취소 요청
     */
    fun cancelRequest(trNo: String): SettleBankResponse {
        val targetPaymentLog = readPaymentLog(trNo)
        val targetUrl = myAccountCancelUrl

        val now = LocalDateTime.now().plusHours(9)
        val hdInfo = "IA_APPROV_1.0_1.0"
        val apiVer = "2.0"
        val cancelOrdNo = "C"+targetPaymentLog.ordNo
        val cancelPrice = AES256EncDec.aesECBEncodeHex(authKeyMyAccount, targetPaymentLog.trPrice)
        val reqDay = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val reqTime = now.format(DateTimeFormatter.ofPattern("HHmmss"))
        val signature = SHA256Encrypt.encryptSHA256HexEncoding(
            mercntIdMyAccount + trNo + cancelOrdNo + targetPaymentLog.trPrice + reqDay + reqTime + authKeyMyAccount
        )

        // http 요청용 JSON 데이터
        val jsonObject = JSONObject()
        jsonObject["hdInfo"] = hdInfo
        jsonObject["apiVer"] = apiVer
        jsonObject["mercntId"] = mercntIdMyAccount
        jsonObject["oldTrNo"] = trNo
        jsonObject["ordNo"] = cancelOrdNo
        jsonObject["cancelPrice"] = cancelPrice
        jsonObject["reqDay"] = reqDay
        jsonObject["reqTime"] = reqTime
        jsonObject["signature"] = signature
        val body = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        // http 요청
        val httpResponse = okHttpClient.newCall(
            Request.Builder().url(targetUrl)
                .addHeader("Content-Type", "application/json")
                .post(body).build()
        ).execute()
        val responseBody = httpResponse.body!!.string()
        val node = objectMapper.readTree(responseBody)
        appConfig.logger.info { "[TWINKOREA] 결제번호#${trNo} 결제 취소 요청 결과 \n $node" }

        val resultCd = node.get("resultCd").toString().replace("\"","")
        val errCd = node.get("errCd").toString().replace("\"","")
        val resultMsg = node.get("resultMsg").toString().replace("\"","")
        val oldTrNo = try {
            node.get("oldTrNo").toString().replace("\"","")
        } catch (e: NullPointerException) {
            null
        }
        val cancelTrNo = try {
            node.get("trNo").toString().replace("\"","")
        } catch (e: java.lang.NullPointerException) {
            null
        }

        return when (resultCd) {
            "0" -> {
                SettleBankResponse(
                    resultIsSuccess = true,
                    trNo = oldTrNo,
                    resultCd = resultCd,
                    errCd = errCd,
                    resultMsg = resultMsg,
                    cancelOrdNo = cancelOrdNo,
                    cancelPrice = cancelPrice,
                    cancelTrNo = cancelTrNo,
                )
            }
            else -> {
                SettleBankResponse(
                    resultIsSuccess = false,
                    resultCd = resultCd,
                    errCd = errCd,
                    resultMsg = resultMsg,
                )
            }
        }

    }

    /**
     * 클라이언트에서 전송된 내 통장 결제 요청 데이터 암호화 (위변조 방지 알고리즘 SHA256)
     */
    fun getSignatureForMyAccount(request: DataEncRequestForMyAccount): String {
        return SHA256Encrypt.encryptSHA256HexEncoding(mercntIdMyAccount + request.ordNo + request.trDay + request.trTime + request.trPrice + authKeyMyAccount)
    }

    /**
     * 클라이언트에서 전송된 trPrice 암호화 (위변조 방지 알고리즘 AES256)
     */
    fun getEncodedPriceForMyAccount(trPrice: String): String {
        return AES256EncDec.aesECBEncodeBase64(authKeyMyAccount, trPrice)
    }

    /**
     * 서명 데이터 확인 (내통장결제용)
     */
    fun getSignDataMyAccount(request: DataEncRequestForMyAccount): MyAccountDataEncResponse {
        val signature = getSignatureForMyAccount(request)
        val encodedPrice = getEncodedPriceForMyAccount(request.trPrice)
        return MyAccountDataEncResponse(signature, encodedPrice)
    }

    /**
     * mercntParam2(=쿠폰 할인 갯수) 를 Long으로 변환
     */
    fun convertMercntParam2ToLong(mercntParam2: String): Long {
        return if (mercntParam2 == "") {
            0
        } else {
            mercntParam2.toLong()
        }
    }
}