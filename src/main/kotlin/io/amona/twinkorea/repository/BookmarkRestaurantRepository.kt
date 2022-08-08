package io.amona.twinkorea.repository

import io.amona.twinkorea.domain.BookmarkRestaurant
import io.amona.twinkorea.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface BookmarkRestaurantRepository: JpaRepository<BookmarkRestaurant, Long>, JpaSpecificationExecutor<BookmarkRestaurant> {
    fun findByBookmarkIdAndPid(bookmarkId: Long, pid: Long): BookmarkRestaurant?

    fun findAllByBookmarkIdAndBookmark_User(bookmarkId: Long, bookmarkUser: User): MutableList<BookmarkRestaurant>

    fun findByPidAndBookmark_DefaultAndBookmark_User(pid: Long, bookmarkDefault: Boolean, bookmarkUser: User): BookmarkRestaurant?

    fun findAllByBookmark_UserAndPid(bookmarkUser: User, pid: Long): MutableList<BookmarkRestaurant>

    fun findAllByPidAndBookmark_Default(pid: Long, bookmarkDefault: Boolean): MutableList<BookmarkRestaurant>

    fun countAllByPidAndBookmark_Default(pid: Long, bookmarkDefault: Boolean): Long

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM BookmarkRestaurant WHERE pid = :pid AND bookmark.id in :groupId")
    fun deleteAllByPid(pid: Long, groupId: MutableList<Long>)
}