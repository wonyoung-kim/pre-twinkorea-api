package io.amona.twinkorea.repository

import io.amona.twinkorea.domain.MapRestaurant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface MapRestaurantRepository: JpaRepository<MapRestaurant, Long>, JpaSpecificationExecutor<MapRestaurant> {
}