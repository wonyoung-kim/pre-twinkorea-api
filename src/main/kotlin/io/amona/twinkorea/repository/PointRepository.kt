package io.amona.twinkorea.repository

import io.amona.twinkorea.domain.Point
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface PointRepository: JpaRepository<Point, Long>, JpaSpecificationExecutor<Point> {
    fun findFirstByUserIdOrderByIdDesc(UserId: Long): Point?
}