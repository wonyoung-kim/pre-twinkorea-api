package io.amona.twinkorea.repository

import com.querydsl.jpa.impl.JPAQueryFactory
import io.amona.twinkorea.domain.Offer
import io.amona.twinkorea.domain.QLand
import io.amona.twinkorea.domain.QOffer
import io.amona.twinkorea.dtos.LandDto
import io.amona.twinkorea.dtos.QLandDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import org.springframework.stereotype.Repository
import javax.annotation.Resource


@Repository
class LandRepositorySupport(
    @Autowired
    @Resource(name = "jpaQueryFactory")
    val query: JPAQueryFactory,
) : QuerydslRepositorySupport(Offer::class.java) {
    fun findAllByOwner(userId: Long, pageable: Pageable): Page<LandDto> {
        val qLand = QLand("l")
        val qOffer = QOffer("o")

        val q = query.select(QLandDto(
            qLand.id, qLand.district, qLand.cellCount, qLand.priceNearBy, qLand.pricePerCell,
            qLand.leftTop, qLand.rightBottom, qOffer.status))
            .from(qLand)
            .leftJoin(qLand.offer, qOffer).on(qLand.offer.id.eq(qOffer.id)).groupBy(qLand.id)
            .where(qLand.owner.id.eq(userId))
            .orderBy(qLand.createdAt.desc())
        q.offset(pageable.offset)
        val fetch = q.fetch()
        return PageImpl(fetch, pageable, fetch.size.toLong())
    }

}
