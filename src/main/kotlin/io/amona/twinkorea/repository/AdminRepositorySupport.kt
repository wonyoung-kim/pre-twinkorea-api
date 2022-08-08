package io.amona.twinkorea.repository

import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import io.amona.twinkorea.domain.Admin
import io.amona.twinkorea.domain.QAdmin
import io.amona.twinkorea.request.UserSearchRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import org.springframework.stereotype.Repository
import javax.annotation.Resource

@Repository
class AdminRepositorySupport(
    @Resource(name = "jpaQueryFactory")
    private val query: JPAQueryFactory
) : QuerydslRepositorySupport(Admin::class.java) {
    fun findAll(request: UserSearchRequest, pageable: Pageable): Page<Admin> {
        val qAdmin = QAdmin("a")
        val builder = BooleanBuilder().and(qAdmin.user.deactivate.isFalse)

        if(request.email != null) {
            builder.and(qAdmin.user.email.contains(request.email))
        }
        if(request.nickname != null) {
            builder.and(qAdmin.user.nickname.contains(request.nickname))
        }
        val q = query.selectFrom(qAdmin).where(builder).orderBy(qAdmin.createdAt.desc())
        val totalCounts = q.fetch().size.toLong()
        val fetch = q.offset(pageable.offset).limit(pageable.pageSize.toLong()).fetch()
        return PageImpl(fetch, pageable, totalCounts)
    }
}