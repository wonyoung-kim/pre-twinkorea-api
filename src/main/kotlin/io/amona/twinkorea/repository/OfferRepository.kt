package io.amona.twinkorea.repository

import io.amona.twinkorea.domain.Offer
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface OfferRepository: JpaRepository<Offer, Long>, JpaSpecificationExecutor<Offer> {
    fun findById(id: Long?): Offer?
}
