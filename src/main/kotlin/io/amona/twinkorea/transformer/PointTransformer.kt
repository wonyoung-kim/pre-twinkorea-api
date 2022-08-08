package io.amona.twinkorea.transformer

import io.amona.twinkorea.domain.Point
import io.amona.twinkorea.repository.PointRepository
import io.amona.twinkorea.request.PointRequest
import org.springframework.stereotype.Component

@Component
class PointTransformer (val repository: PointRepository) {
    fun from(request: PointRequest): Point {
        return Point(
            id = request.pointId ?: 0,
            balance = 0, // 잔액은 나중에 채워줌
            krw = request.krw,
            msg = request.msg,
            revenueType = request.revenueType,
            user = request.user
        )
    }
}