package io.amona.twinkorea.repository

import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import io.amona.twinkorea.domain.PaymentLog
import io.amona.twinkorea.domain.QPaymentLog
import io.amona.twinkorea.request.PaymentLogSearchRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import org.springframework.stereotype.Repository
import javax.annotation.Resource

@Repository
class PaymentLogRepositorySupport(
    @Resource(name = "jpaQueryFactory")
    private val query: JPAQueryFactory
): QuerydslRepositorySupport(PaymentLog::class.java) {
    fun findAll(request: PaymentLogSearchRequest, pageable: Pageable): Page<PaymentLog> {
        val qPaymentLog = QPaymentLog("p")
        val builder = BooleanBuilder()

        if(request.trNo != null) {
            builder.and(qPaymentLog.trNo.eq(request.trNo.trim()))
        }
        if(request.userEmail != null) {
            builder.and(qPaymentLog.user.email.eq(request.userEmail.trim()))
        }
        if(request.startDate != null && request.endDate != null) {
            builder.and(qPaymentLog.createdAt.between(request.startDate, request.endDate))
        }
        val q = query.selectFrom(qPaymentLog).where(builder).orderBy(qPaymentLog.createdAt.desc())
        val qCount = query.select(qPaymentLog.count()).from(qPaymentLog).where(builder)
        val counts = qCount.fetch().first()
        val fetch = q.offset(pageable.offset).limit(pageable.pageSize.toLong()).fetch()
        return PageImpl(fetch, pageable, counts)
    }
}