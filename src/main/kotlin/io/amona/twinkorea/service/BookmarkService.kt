package io.amona.twinkorea.service

import io.amona.twinkorea.configuration.AppConfig
import io.amona.twinkorea.domain.Bookmark
import io.amona.twinkorea.domain.BookmarkRestaurant
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.dtos.BookmarkListDto
import io.amona.twinkorea.exception.DuplicatedException
import io.amona.twinkorea.exception.NotFoundException
import io.amona.twinkorea.exception.WrongStatusException
import io.amona.twinkorea.repository.BookmarkRepository
import io.amona.twinkorea.repository.BookmarkRestaurantRepository
import io.amona.twinkorea.repository.UserRepository
import io.amona.twinkorea.request.BookmarkRequest
import io.amona.twinkorea.transformer.BookmarkTransformer
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class BookmarkService(private val repo: BookmarkRepository,
                      private val transformer: BookmarkTransformer,
                      private val bookmarkRestaurantRepo: BookmarkRestaurantRepository,
                      private val userRepo: UserRepository,
                      private val appConfig: AppConfig,
                      ) {

    fun addBookmarkGroup(request: BookmarkRequest, user: User): Bookmark {
        val transformed = transformer.from(request, user)
        val result = repo.save(transformed)
        appConfig.logger.info{
            "[TWINKOREA API] 유저#${result.user.id}가 새로운 북마크 그룹 \"${result.groupName}\"을 만들었습니다."
        }
        return result
    }

    @Transactional
    fun editBookmarkGroup(groupId: Long, user: User, request: BookmarkRequest): Bookmark {
        val now = LocalDateTime.now()
        val target = repo.findByIdAndUser(id = groupId, user = user)
            ?: throw NotFoundException("수정하려는 대상을 찾을 수 없습니다. 입력된 파라미터를 확인해주세요.")
        if (target.default) throw WrongStatusException("기본 그룹은 수정할 수 없습니다.")
        return repo.save(target.copy(
            groupName = request.groupName, groupColor = request.groupColor, iconUrl = request.iconUrl, updatedAt = now)
        )
    }

    fun getBookmarkGroupList(user: User): MutableList<BookmarkListDto> {
        val bookmarkList = repo.findAllByUser(user = user)
        val bookmarkDtoList: MutableList<BookmarkListDto> = mutableListOf()
        bookmarkList.forEach {
            val bookmarkDetail = BookmarkListDto(
                id = it.id,
                groupName = it.groupName,
                groupColor = it.groupColor,
                restaurantCounts = it.counts
            )
            bookmarkDtoList.add(bookmarkDetail)
        }
        return bookmarkDtoList
    }

    @Transactional
    fun addRestaurantToDefaultBookmark(pid: Long, user: User): BookmarkRestaurant {
        val userDefaultBookmark = repo.findFirstByUserAndDefaultIsTrue(user = user)
            ?: throw NotFoundException("유저의 기본 북마크 그룹을 불러올 수 없습니다. 관리자에게 문의하세요.")

        // 중복 체크
        val checkDuplicated = bookmarkRestaurantRepo.findByBookmarkIdAndPid(bookmarkId = userDefaultBookmark.id, pid = pid)
        if (checkDuplicated != null) throw DuplicatedException("이미 저장된 식당입니다.")

        val now = LocalDateTime.now()
        val result =  bookmarkRestaurantRepo.save(
            BookmarkRestaurant(
                pid = pid,
                bookmark = userDefaultBookmark,
                createdAt = now,
                updatedAt = now,
            )
        )
        // 새로 등록 후 북마크 그룹의 카운트 증가시킴
        repo.save(userDefaultBookmark.copy(counts = userDefaultBookmark.counts + 1))
        userRepo.save(user.copy(bookmarkCount = user.bookmarkCount + 1))
        appConfig.logger.info{
            "[TWINKOREA API] 유저#${user.id}가 식당 ID : #${pid}를 마이맛집에 저장하였습니다."
        }
        return result
    }

    @Transactional
    fun addRestaurantToBookmarkGroup(groupId: Long, pid: Long, user: User): BookmarkRestaurant {
        val targetBookmark = repo.findById(id = groupId)
        if (targetBookmark == null || targetBookmark.user != user) {
            throw NotFoundException("북마크 그룹을 불러올 수 없습니다. 입력값을 확인해주세요.")
        }
        bookmarkRestaurantRepo.findByPidAndBookmark_DefaultAndBookmark_User(pid = pid, bookmarkDefault = true, bookmarkUser = user)
            ?: throw NotFoundException("등록하고자 하는 식당을 기본 그룹 리스트에서 찾을 수 없습니다.")
        val checkDuplicated = bookmarkRestaurantRepo.findByBookmarkIdAndPid(bookmarkId = groupId, pid = pid)
        if (checkDuplicated != null) throw DuplicatedException("이미 저장된 식당입니다.")

        val now = LocalDateTime.now()
        val result = bookmarkRestaurantRepo.save(
            BookmarkRestaurant(
                pid = pid,
                bookmark = targetBookmark,
                createdAt = now,
                updatedAt = now
            )
        )
        // 새로 등록 후 북마크 그룹의 카운트 증가시킴
        repo.save(targetBookmark.copy(counts = targetBookmark.counts + 1))
        appConfig.logger.info{
            "[TWINKOREA API] 유저#${user.id}가 식당 ID : #${pid}를 북마크 그룹 ID : #${groupId}에 저장하였습니다."
        }
        return result
    }

    @Transactional
    fun deleteRestaurantFromBookmarkGroup(groupId: Long, pid: Long, user: User) {
        val target = bookmarkRestaurantRepo.findByBookmarkIdAndPid(bookmarkId = groupId, pid = pid)
            ?: throw NotFoundException("삭제하려는 대상을 찾을 수 없습니다. 입력된 파라미터를 확인해주세요")
        // 기본 븍마크 그룹에서 삭제할 경우, 다른 그룹에서도 모두 삭제되어야 함
        return if (target.bookmark.default) {
            // 입력된 pid와 user를 갖는 북마크 그룹을 가져온 후 카운트를 1씩 빼고, 식당을 그 북마크에서 삭제함
            val bookmarks = getBookmarksByPid(pid = pid, user = user)
            val bookmarkIds: MutableList<Long> = mutableListOf()
            // 카운트 빼고, 북마크 아이디 만들기
            bookmarks.forEach {
                repo.save(it.copy(counts = it.counts - 1))
                bookmarkIds.add(it.id)
            }
            // 북마크 아이디와 pid가 겹치는 애들 삭제
            bookmarkRestaurantRepo.deleteAllByPid(target.pid, bookmarkIds)
            appConfig.logger.info {
                "[TWINKOREA API] 유저#${user.id}가 기본 북마크 그룹 ID : #${groupId}에 포함된 식당 ID : #${pid}를 삭제하였습니다. " +
                        "하위 그룹에 등록된 정보도 모두 삭제합니다."
            }
        } else {
            // 하위 북마크 그룹에서 삭제할 경우, 다른 그룹에는 영향을 주지 않음
            bookmarkRestaurantRepo.delete(target)
            // 북마크의 카운트 갯수 빼기
            repo.save(target.bookmark.copy(counts = target.bookmark.counts - 1))
            appConfig.logger.info {
                "[TWINKOREA API] 유저#${user.id}가 북마크 그룹 ID : #${groupId}에 포함된 식당 ID : #${pid}를 삭제하였습니다."
            }
        }
    }

    fun getPidsByBookmark(groupId: Long, user: User): MutableList<Long> {
        val bookmarkRestaurantList = bookmarkRestaurantRepo.findAllByBookmarkIdAndBookmark_User(
            bookmarkId = groupId, bookmarkUser = user
        )
        if (bookmarkRestaurantList.size == 0) throw NotFoundException("회원 정보와 일치하는 북마크 그룹 데이터를 찾을 수 없습니다.")
        val userBookmarkList: MutableList<Long> = mutableListOf()
        bookmarkRestaurantList.forEach {
            userBookmarkList.add(it.pid)
        }
        return userBookmarkList
    }

    fun getBookmarksByPid(pid: Long, user: User): MutableList<Bookmark> {
        val bookmarkRestaurantList = bookmarkRestaurantRepo.findAllByBookmark_UserAndPid(bookmarkUser = user, pid = pid)
        val bookmarks: MutableList<Bookmark> = mutableListOf()
        bookmarkRestaurantList.forEach {
            bookmarks.add(it.bookmark)
        }
        return bookmarks
    }
}