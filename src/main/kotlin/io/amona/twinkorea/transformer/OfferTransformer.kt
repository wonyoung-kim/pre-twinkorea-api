package io.amona.twinkorea.transformer

import io.amona.twinkorea.domain.Offer
import io.amona.twinkorea.exception.NotFoundException
import io.amona.twinkorea.repository.CellRepository
import io.amona.twinkorea.repository.LandRepository
import io.amona.twinkorea.repository.OfferRepository
import io.amona.twinkorea.request.OfferRequest
import org.springframework.stereotype.Component

@Component
class OfferTransformer(val cellRepository: CellRepository,
                       val landRepository: LandRepository,
                       val repo: OfferRepository,
                       ) {
    fun toSellFrom(request: OfferRequest): Offer {
        val land = landRepository.findById(id = request.landId!!)
            ?: throw NotFoundException("판매 요청에 판매하고자 하는 지역묶음이 포함되어있지 않습니다.")
        return Offer(
                buyer = null,
                seller = null,
                name = request.name ?: "",
                district = land.district,
                land = land
        )
    }

    fun toBuyFrom(offerId: Long): Offer {
        return repo.findById(id=offerId)
            ?: throw NoSuchElementException("Request Failed :: No Offer: Offer#${offerId}")
    }
}


