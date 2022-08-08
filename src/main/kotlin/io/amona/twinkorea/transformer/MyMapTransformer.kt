package io.amona.twinkorea.transformer

import io.amona.twinkorea.domain.MyMap
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.repository.MyMapRepository
import io.amona.twinkorea.request.MyMapRequest
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class MyMapTransformer(val repository: MyMapRepository) {
    fun from(request: MyMapRequest, user: User): MyMap {
        val now = LocalDateTime.now()
        return MyMap(
            id = 0,
            mapName = request.mapName!!,
            leftTop = request.leftTop!!,
            rightBottom = request.rightBottom!!,
            iconUrl = request.iconUrl,
            user = user,
            createdAt = now,
            updatedAt = now,
        )
    }
}