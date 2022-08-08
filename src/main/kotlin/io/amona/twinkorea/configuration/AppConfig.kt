package io.amona.twinkorea.configuration

import mu.KotlinLogging
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "application.siksinapi")
class AppConfig {
    lateinit var ipWhiteList: String
    lateinit var apikey: String
    lateinit var host: String
    lateinit var defaultCellValue: String
    lateinit var defaultCouponValue: String

    // 결제모듈 상수
    lateinit var npMid: String                      // 나이스페이 상점 ID
    lateinit var npMerchantKey: String              // 나이스페이 상점 키
    lateinit var stMercntIdMyAccount: String        // 세틀뱅크 내통장결제 상점 ID
    lateinit var stAuthKeyMyAccount: String         // 세틀뱅크 내통장결제 암호화키
    lateinit var stMercntIdPg: String               // 세틀뱅크 PG 결제 상점 ID
    lateinit var stHashAuthKeyPg: String            // 세틀뱅크 PG 결제 해쉬값 암호화키
    lateinit var stPrvcAuthKeyPg: String            // 세틀뱅크 PG 결제 개인정보 암호화키

    lateinit var redirectUrl: String                // 결제 성공시 리다이렉트 시킬 url
    lateinit var myAccountApproveUrl: String
    lateinit var myAccountCancelUrl: String

    // 사전청약 진행 날짜 관련
    lateinit var preOrderAvailableTo: String
    lateinit var waitingOrderAvailableTo: String
    lateinit var preOrderPurchaseAvailableFrom: String
    lateinit var preOrderPurchaseAvailableTo: String
    lateinit var waitingOrderPurchaseAvailableFrom: String
    lateinit var waitingOrderPurchaseAvailableTo: String
    lateinit var allPurchaseAvailableFrom: String
    lateinit var cellDiscountTo: String

    // 결제 금액
    lateinit var cityPrice: String
    lateinit var ruralPrice: String
    lateinit var seaPrice: String

    val logger = KotlinLogging.logger{}
}