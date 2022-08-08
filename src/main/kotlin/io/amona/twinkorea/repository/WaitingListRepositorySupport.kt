package io.amona.twinkorea.repository

import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import io.amona.twinkorea.domain.QWaitingList
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.domain.WaitingList
import io.amona.twinkorea.enums.PreOrderStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import org.springframework.stereotype.Repository
import javax.annotation.Resource

@Repository
class WaitingListRepositorySupport(
    @Resource(name = "jpaQueryFactory")
    private val query: JPAQueryFactory
): QuerydslRepositorySupport(WaitingList::class.java) {
    fun findAllAvailablePreContract(user: User, areaId: Long?, pageable: Pageable, already: MutableList<Long>): Page<WaitingList> {
        val qWaitingList = QWaitingList("wl")
        val builder = BooleanBuilder()
            .and(qWaitingList.user.eq(user))
            .and(qWaitingList.preOrder.status.eq(PreOrderStatus.PREORDER))

        if (already.size > 0) {
            builder.and(qWaitingList.id.notIn(already))
        }

        if (areaId != null) {
            builder.and(qWaitingList.preOrder.areaId.notIn(areaId))
        }

        val q = query.select(qWaitingList).from(qWaitingList)
            .where(builder).groupBy(qWaitingList.preOrder.id)

        val totalCounts = q.fetch().size.toLong()
        val fetch = q.offset(pageable.offset).limit(pageable.pageSize.toLong()).fetch()
        return PageImpl(fetch, pageable, totalCounts)
    }
}