package io.amona.twinkorea.service

import io.amona.twinkorea.configuration.AppConfig
import io.amona.twinkorea.domain.Point
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.enums.MsgRevenueType
import io.amona.twinkorea.exception.BalanceException
import io.amona.twinkorea.repository.PointRepository
import io.amona.twinkorea.request.PointRequest
import io.amona.twinkorea.transformer.PointTransformer
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PointService(
    private val repo: PointRepository,
    private val transformer: PointTransformer,
    private val appConfig: AppConfig
) {
    fun createPoint(request: PointRequest): Point {
        val userBalance = readUserBalance(request.user)
        val transformed = transformer.from(request)
        val now = LocalDateTime.now()
        val balance: Long
        val usage: String
        if (transformed.revenueType == MsgRevenueType.BUY) {
            balance = userBalance - transformed.msg!!
            if (balance < 0) throw BalanceException("구매자의 잔액이 부족합니다.")
            usage = "차감"
        } else {
            balance =  readUserBalance(request.user) + transformed.msg!!
            usage = "획득"
        }
        val result = repo.save(transformed.copy(balance = balance, createdAt = now, updatedAt = now))
        appConfig.logger.info {
            "[TWINKOREA API] 유저#${result.user!!.id}가 ${result.revenueType.displayName}로 MSG 포인트 ${result.msg} 만큼을 $usage 하였습니다."
        }
        return result
    }

    private fun readUserBalance(user: User): Long {
        val userPoint = repo.findFirstByUserIdOrderByIdDesc(user.id)
        return userPoint?.balance ?: 0
    }
}