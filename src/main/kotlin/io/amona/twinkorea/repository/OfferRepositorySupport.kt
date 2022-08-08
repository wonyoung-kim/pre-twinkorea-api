package io.amona.twinkorea.repository

import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import io.amona.twinkorea.domain.Offer
import io.amona.twinkorea.domain.QLand
import io.amona.twinkorea.domain.QOffer
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.dtos.OfferDto
import io.amona.twinkorea.dtos.PopularOfferDto
import io.amona.twinkorea.dtos.QOfferDto
import io.amona.twinkorea.dtos.QPopularOfferDto
import io.amona.twinkorea.enums.OfferStatus
import io.amona.twinkorea.request.OfferSearchRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import org.springframework.stereotype.Repository
import javax.annotation.Resource

@Repository
class OfferRepositorySupport(
    @Autowired
    @Resource(name = "jpaQueryFactory")
    val query: JPAQueryFactory
) : QuerydslRepositorySupport(Offer::class.java) {
    fun findAll(request: OfferSearchRequest, pageable: Pageable): Page<OfferDto> {
        val qOffer = QOffer("o")
        val qLand = QLand("l")
        val builder = BooleanBuilder().and(qOffer.status.eq(OfferStatus.PENDING))

        if(request.addressOne != null) {
            builder.and(qOffer.district.contains(request.addressOne))
        }
        if(request.addressTwo != null) {
            builder.and(qOffer.district.contains(request.addressTwo))
        }
        if(request.addressThree != null) {
            builder.and(qOffer.district.contains(request.addressThree))
        }
        if(request.text != null) {
            builder.and(qOffer.district.contains(request.text).or(qOffer.name.contains(request.text)))
        }
        val q = query.select(QOfferDto(
            qOffer.id, qOffer.name, qOffer.district, qLand.cellCount, qLand.priceNearBy, qLand.pricePerCell,
            qLand.leftTop, qLand.rightBottom))
            .from(qOffer, qLand)
            .join(qLand).where(qOffer.land.id.eq(qLand.id))
            .where(builder).orderBy(qOffer.createdAt.desc())
        val fetch = q.offset(pageable.offset).limit(pageable.pageSize.toLong()).fetch()
        return PageImpl(fetch, pageable, fetch.size.toLong())
    }

    fun findOfferToMe(user: User, pageable: Pageable): Page<OfferDto> {
        val qOffer = QOffer("o")
        val qLand = QLand("l")

        val builder = BooleanBuilder().and(qOffer.status.eq(OfferStatus.OFFERED)).and(qOffer.seller.eq(user))
        val q = query.select(QOfferDto(
            qOffer.id, qOffer.name, qOffer.district, qLand.cellCount, qLand.priceNearBy, qLand.pricePerCell,
            qLand.leftTop, qLand.rightBottom))
            .from(qOffer, qLand)
            .join(qLand).where(qOffer.land.id.eq(qLand.id))
            .where(builder).orderBy(qOffer.createdAt.desc())
        q.offset(pageable.offset)
        val fetch = q.fetch()
        return PageImpl(fetch, pageable, fetch.size.toLong())
    }

    fun findOfferFromMe(user: User, pageable: Pageable): Page<OfferDto> {
        val qOffer = QOffer("o")
        val qLand = QLand("l")

        val builder = BooleanBuilder().and(qOffer.status.eq(OfferStatus.OFFERED)).and(qOffer.buyer.eq(user))
        val q = query.select(QOfferDto(
            qOffer.id, qOffer.name, qOffer.district, qLand.cellCount, qLand.priceNearBy, qLand.pricePerCell,
            qLand.leftTop, qLand.rightBottom))
            .from(qOffer, qLand)
            .join(qLand).where(qOffer.land.id.eq(qLand.id))
            .where(builder).orderBy(qOffer.createdAt.desc())
        q.offset(pageable.offset)
        val fetch = q.fetch()
        return PageImpl(fetch, pageable, fetch.size.toLong())
    }

    fun findPopularOffer(): List<PopularOfferDto> {
        val qOffer = QOffer("q")
        val qLand = QLand("l")

//        val builder = BooleanBuilder().and(qOffer.status.eq(OfferStatus.PENDING))
        val q = query.select(QPopularOfferDto(
            qOffer.id, qOffer.district, qLand.siksinCount, qLand.estEarn, qLand.pricePerCell, qLand.pricePerCell
        )).from(qOffer, qLand)
            .join(qLand).where(qOffer.land.id.eq(qLand.id))
            .where(qOffer.status.eq(OfferStatus.PENDING))
            .orderBy(qLand.siksinCount.desc()).limit(6)
        return q.fetch()
    }
}