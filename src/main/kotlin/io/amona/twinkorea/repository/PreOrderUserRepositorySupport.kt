package io.amona.twinkorea.repository

import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import io.amona.twinkorea.domain.PreOrderUser
import io.amona.twinkorea.domain.QPreOrderUser
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.enums.PreOrderStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import org.springframework.stereotype.Repository
import javax.annotation.Resource

@Repository
class PreOrderUserRepositorySupport(
    @Resource(name = "jpaQueryFactory")
    private val query: JPAQueryFactory
): QuerydslRepositorySupport(PreOrderUser::class.java) {

    fun findAllAvailablePreContract(user: User, areaId: Long?, pageable: Pageable, already: MutableList<Long>): Page<PreOrderUser> {
        val qPreOrderUser = QPreOrderUser("po")

        val builder = BooleanBuilder()
            .and(qPreOrderUser.user.eq(user))
            .and(qPreOrderUser.preOrder.status.eq(PreOrderStatus.PREORDER))

        if (already.size > 0) {
            builder.and(qPreOrderUser.id.notIn(already))
        }

        if (areaId != null) {
            builder.and(qPreOrderUser.preOrder.areaId.notIn(areaId))
        }

        val q = query.select(qPreOrderUser).from(qPreOrderUser)
            .where(builder).groupBy(qPreOrderUser.preOrder.id)

        val totalCounts = q.fetch().size.toLong()
        val fetch = q.offset(pageable.offset).limit(pageable.pageSize.toLong()).fetch()
        return PageImpl(fetch, pageable, totalCounts)
    }
}