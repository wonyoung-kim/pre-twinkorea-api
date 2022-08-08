package io.amona.twinkorea.transformer

import io.amona.twinkorea.domain.Admin
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.request.AdminUserRequest
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class AdminUserTransformer {
    fun from(request: AdminUserRequest): Admin {
        val now = LocalDateTime.now()
        return Admin(
            id = 0,
            superAdmin = request.superAdmin ?: false,
            adminRole = request.adminRole,
            user = User(id=0),
            createdAt = now,
            updatedAt = now,
        )
    }
}