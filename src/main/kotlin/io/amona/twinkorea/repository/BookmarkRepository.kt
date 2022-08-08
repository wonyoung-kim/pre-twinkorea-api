package io.amona.twinkorea.repository

import io.amona.twinkorea.domain.Bookmark
import io.amona.twinkorea.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface BookmarkRepository: JpaRepository<Bookmark, Long>, JpaSpecificationExecutor<Bookmark> {
    fun findById(id: Long?): Bookmark?

    fun findByIdAndUser(id: Long, user: User): Bookmark?

    fun findFirstByUserAndDefaultIsTrue(user: User): Bookmark?

    // TODO 정렬 -> 식당 갯수
    fun findAllByUser(user: User): MutableList<Bookmark>
}