package io.amona.twinkorea.repository

import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import io.amona.twinkorea.domain.PreOrder
import io.amona.twinkorea.domain.QCell
import io.amona.twinkorea.domain.QPreOrder
import io.amona.twinkorea.request.PolygonCellDataSearchRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import org.springframework.stereotype.Repository
import javax.annotation.Resource

@Repository
class PreOrderRepositorySupport(
    @Resource(name = "jpaQueryFactory")
    private val query: JPAQueryFactory
): QuerydslRepositorySupport(PreOrder::class.java) {
    fun findAll(request: PolygonCellDataSearchRequest, pageable: Pageable): Page<PreOrder> {
        val qPreOrder = QPreOrder("p")
        val qCell = QCell("c")
        val builder = BooleanBuilder().and(qCell.areaId.isNotNull)

        if (request.name != null) {
            builder.and(qPreOrder.name.contains(request.name))
        }
        if (request.district != null) {
            builder.and(qCell.centerCity.contains(request.district))
        }

        val q = query.select(qPreOrder)
            .from(qPreOrder)
            .join(qCell).on(qPreOrder.areaId.eq(qCell.areaId))
            .where(builder).groupBy(qPreOrder.id)

        val sortedQ = if (request.sort != null) {
            val sortingTarget = request.sort.split(",")[0]
            val sortingBy = request.sort.split(",")[1]
            when (sortingTarget) {
                // .orderBy(qOffer.createdAt.desc())
                "id" -> {
                    when (sortingBy) {
                        "asc" -> q.orderBy(qPreOrder.id.asc())
                        else  -> q.orderBy(qPreOrder.id.desc())
                    }
                }
                "areaId" -> {
                    when (sortingBy) {
                        "asc" -> q.orderBy(qPreOrder.areaId.asc())
                        else  -> q.orderBy(qPreOrder.areaId.desc())
                    }
                }
                "name" -> {
                    when (sortingBy) {
                        "asc" -> q.orderBy(qPreOrder.name.asc())
                        else  -> q.orderBy(qPreOrder.name.desc())
                    }
                }
                "totalCellCount" -> {
                    when (sortingBy) {
                        "asc" -> q.orderBy(qPreOrder.cellCount.asc())
                        else  -> q.orderBy(qPreOrder.cellCount.desc())
                    }
                }
                "reservedCellCount" -> {
                    when (sortingBy) {
                        "asc" -> q.orderBy(qPreOrder.reservedCellCount.asc())
                        else  -> q.orderBy(qPreOrder.reservedCellCount.desc())
                    }
                }
                "purchasableCellCount" -> {
                    when (sortingBy) {
                        "asc" -> q.orderBy((qPreOrder.cellCount.subtract(qPreOrder.reservedCellCount)).asc())
                        else  -> q.orderBy((qPreOrder.cellCount.subtract(qPreOrder.reservedCellCount)).desc())
                    }
                }
                "purchasedCellCount" -> {
                    when (sortingBy) {
                        "asc" -> q.orderBy((qPreOrder.purchaseCount).asc())
                        else  -> q.orderBy((qPreOrder.purchaseCount).desc())
                    }
                }
                "soldOut" -> {
                    when (sortingBy) {
                        "asc" -> q.orderBy(
                            (qPreOrder.cellCount.subtract(qPreOrder.reservedCellCount).subtract(qPreOrder.purchaseCount))
                                .asc()
                        )
                        else -> q.orderBy(
                            (qPreOrder.cellCount.subtract(qPreOrder.reservedCellCount).subtract(qPreOrder.purchaseCount))
                                .desc()
                        )
                    }
                }
                else -> q
            }
        } else q

        val totalCounts = sortedQ.fetch().size.toLong()

        val fetch = sortedQ.offset(pageable.offset).limit(pageable.pageSize.toLong()).fetch()
        return PageImpl(fetch, pageable, totalCounts)
    }

    fun findPurchasableInSeoul(areaId: Long?, pageable: Pageable): Page<PreOrder> {
        val qPreOrder = QPreOrder("p")
        val builder = BooleanBuilder()

        if (areaId != null) {
            builder.and(qPreOrder.areaId.ne(areaId))
        }

        val q = query.select(qPreOrder)
            .from(qPreOrder)
            .where(builder).orderBy(
                qPreOrder.cellCount.subtract(
                    qPreOrder.reservedCellCount).subtract(
                    qPreOrder.purchaseCount)
                    .asc()
            )

        val totalCounts = q.fetch().size.toLong()
        val fetch = q.offset(pageable.offset).limit(pageable.pageSize.toLong()).fetch()
        return PageImpl(fetch, pageable, totalCounts)
    }
}