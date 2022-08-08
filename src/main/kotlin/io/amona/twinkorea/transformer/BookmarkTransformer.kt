package io.amona.twinkorea.transformer

import io.amona.twinkorea.domain.Bookmark
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.repository.BookmarkRepository
import io.amona.twinkorea.request.BookmarkRequest
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class BookmarkTransformer(val repository: BookmarkRepository) {
    fun from(request: BookmarkRequest, user: User): Bookmark {
        val now = LocalDateTime.now()
        return Bookmark(
            id = 0,
            groupName = request.groupName,
            groupColor = request.groupColor,
            iconUrl = request.iconUrl,
            createdAt = now,
            updatedAt = now,
            user = user,
            counts = 0,
        )
    }
}