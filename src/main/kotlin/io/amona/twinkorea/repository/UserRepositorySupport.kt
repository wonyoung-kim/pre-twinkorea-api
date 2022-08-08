package io.amona.twinkorea.repository

import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import io.amona.twinkorea.domain.Admin
import io.amona.twinkorea.domain.QUser
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.request.UserSearchRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import org.springframework.stereotype.Repository
import javax.annotation.Resource

@Repository
class UserRepositorySupport(
    @Resource(name = "jpaQueryFactory")
    private val query: JPAQueryFactory
) : QuerydslRepositorySupport(Admin::class.java) {
    fun findAll(request: UserSearchRequest, pageable: Pageable): Page<User> {
        val qUser = QUser("u")
        val builder = BooleanBuilder().and(qUser.deactivate.isFalse).and(qUser.admin.isFalse)

        if(request.email != null) {
            builder.and(qUser.email.contains(request.email))
        }
        if(request.nickname != null) {
            builder.and(qUser.nickname.contains(request.nickname))
        }
        val q = query.selectFrom(qUser).where(builder).orderBy(qUser.createdAt.desc())
        val qCount = query.select(qUser.count()).from(qUser).where(builder)
        val counts = qCount.fetch().first()
        val fetch = q.offset(pageable.offset).limit(pageable.pageSize.toLong()).fetch()
        return PageImpl(fetch, pageable, counts)
    }
}