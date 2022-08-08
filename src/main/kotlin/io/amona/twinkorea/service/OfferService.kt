package io.amona.twinkorea.service

import io.amona.twinkorea.configuration.AppConfig
import io.amona.twinkorea.domain.Offer
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.dtos.OfferDto
import io.amona.twinkorea.dtos.PopularOfferDto
import io.amona.twinkorea.enums.MsgRevenueType
import io.amona.twinkorea.enums.OfferStatus
import io.amona.twinkorea.exception.*
import io.amona.twinkorea.repository.LandRepository
import io.amona.twinkorea.repository.OfferRepository
import io.amona.twinkorea.repository.OfferRepositorySupport
import io.amona.twinkorea.repository.PointRepository
import io.amona.twinkorea.request.OfferRequest
import io.amona.twinkorea.request.OfferSearchRequest
import io.amona.twinkorea.request.PointRequest
import io.amona.twinkorea.transformer.OfferTransformer
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class OfferService(private val repo: OfferRepository,
                   private val offerDslRepo: OfferRepositorySupport,
                   private val pointRepo: PointRepository,
                   private val pointService: PointService,
                   private val landRepo: LandRepository,
                   private val transformer: OfferTransformer,
                   private val appConfig: AppConfig) {

    private fun isSellable(user: User, offer: Offer) {
        if (offer.land!!.owner?.id != user.id)
            throw NotFoundException("판매하고자 하는 땅 묶음을 찾을 수 없거나, 땅 묶음의 소유자를 찾을 수 없습니다. 땅의 소유 권한을 확인하세요.")
        else if (offer.land.isSelling)
            throw DuplicatedException("이미 판매중인 땅 묶음은 판매등록 할 수 없습니다.")
    }

    private fun isBuyable(user: User, offer: Offer) {
        if (offer.status != OfferStatus.PENDING)
            throw WrongStatusException("주문건 #${offer.id}의 상태는 ${offer.status}로, 현재 주문 가능한 상태가 아닙니다.")
        val userPoint = pointRepo.findFirstByUserIdOrderByIdDesc(user.id) ?: throw BalanceException("구매자의 잔액이 부족합니다.")
        if (userPoint.balance < (offer.land!!.pricePerCell * offer.land.cellCount))
            throw BalanceException("구매자의 잔액이 부족합니다.")
    }

    @Transactional
    fun createSellOffer(user: User, offerRequest: OfferRequest): Offer {
        if (offerRequest.landId == null || offerRequest.price == null || offerRequest.name == null) {
            throw NotNullException("셀 판매 등록 API 의 필수 입력값은 cellId, price, name 입니다.")
        }
        val transformed = transformer.toSellFrom(offerRequest)
        val now = LocalDateTime.now()
        // 셀 판매 등록 가능여부 체크
        isSellable(user, transformed)
        val result = repo.save(transformed.copy(seller = user, createdAt = now, updatedAt = now))
        landRepo.save(result.land!!.copy(offer = result, isSelling = true, pricePerCell = offerRequest.price))
        val length = result.land.cellCount
        val price = result.land.pricePerCell
        appConfig.logger.info {"[TWINKOREA API] 유저#${user.id}가 셀#${length}개를 총 ${price * length}MSG 포인트에 판매하는 거래를 등록하였습니다."}
        return result
    }

    fun createBuyOffer(user: User, offerId: Long): Offer {
        val transformed = transformer.toBuyFrom(offerId)
        val length = transformed.land!!.cellCount
        val price = transformed.land.pricePerCell
        val now = LocalDateTime.now()
        // 셀 구매 가능여부 체크
        isBuyable(user, transformed)
        val result = repo.save(transformed.copy(buyer = user, updatedAt = now, status = OfferStatus.OFFERED))
        appConfig.logger.info { "[TWINKOREA API] 유저#${user.id}가 셀#${length}개를 총 ${price * length}MSG 포인트에 구매하는 거래를 신청하였습니다." }
        return result
    }

    @Transactional
    fun changeOfferStatus(user: User, offerId: Long, offerStatus: String): Offer {
        val targetOffer = repo.findById(id = offerId)
        // 주문이 없거나, 판매자의 아이디가 로그인된 사용자가 아닐경우 에러
        if (targetOffer?.seller == null || targetOffer.seller.id != user.id || targetOffer.status != OfferStatus.OFFERED) {
            throw NotFoundException("주문이 없거나, 주문이 제안됨 상태가 아니거나, 주문의 판매자와 요청한 회원 정보가 다릅니다.")
        }
        // 구매자가 없는 경우 에러
        val buyer = targetOffer.buyer ?: throw NotFoundException("구매자가 없는 주문건입니다.")
        val totalPrice = targetOffer.land!!.pricePerCell * targetOffer.land.cellCount
        val now = LocalDateTime.now()
        when (offerStatus) {
            "ACCEPT" -> {
                // 오퍼의 상태를 완료로 변경하고
                val result = repo.save(targetOffer.copy(updatedAt = now, status = OfferStatus.DONE))
                // 구매자의 포인트를 깍는다.
                pointService.createPoint(PointRequest(
                    msg = totalPrice, revenueType = MsgRevenueType.BUY, user = buyer
                ))
                // 판매자의 포인트를 늘린다.
                pointService.createPoint(
                    PointRequest(
                    msg = totalPrice, revenueType = MsgRevenueType.SELL, user = user
                ))
                // 지역의 판매 상태를 판매중에서 판매중이 아님으로 변경하고, 소유자를 구매자로 변경한다.
                landRepo.save(result.land!!.copy(isSelling = false, owner = buyer))
                appConfig.logger.info { "[TWINKOREA API] 판매자#${user.id}가 구매자#${buyer.id}이 신청한 구매 주문건#${targetOffer.id}를 승낙하였습니다." }
                return result
            }

            "DENY" -> {
                // 오퍼의 상태를 다시 대기중으로 변경하고 buyer 를 없앤다
                val result = repo.save(targetOffer.copy(updatedAt = now, status = OfferStatus.PENDING, buyer = null))
                appConfig.logger.info { "[TWINKOREA API] 유저#${user.id}가 유저#${buyer.id}이 신청한 구매 주문건#${targetOffer.id}를 거부하였습니다." }
                return result
            }
            else -> throw NoSuchElementException("\"ACCEPT\"와 \"DENY\"의 인자만 허용합니다.")
            }
    }

    @Transactional
    fun cancelSellOffer(user: User, offerId: Long): Offer {
        val targetOffer = repo.findById(id = offerId)
        val now = LocalDateTime.now()
        // 주문이 없거나, 판매자의 아이디가 로그인된 사용자가 아닐경우 에러
        if (targetOffer?.seller == null || targetOffer.seller.id != user.id || targetOffer.status != OfferStatus.PENDING) {
            throw NotFoundException("주문이 없거나, 주문의 판매자와 요청한 회원 정보가 다릅니다. (주문 대기중 상태가 아닌 주문건은 취소할 수 없습니다.)")
        }
        val result = repo.save(targetOffer.copy(updatedAt = now, status = OfferStatus.CANCEL, buyer = null))
        // 판매 등록된 셀들과 주문의 연결을 끊고, 판매 상태를 취소시킴
        landRepo.save(result.land!!.copy(offer = null, isSelling = false))
        appConfig.logger.info("[TWINKOREA API] 유저#${user.id}가 구매 주문건#${targetOffer.id}를 취소하였습니다.")
        return result
    }

    @Transactional
    fun cancelBuyOffer(user: User, offerId: Long): Offer {
        val targetOffer = repo.findById(id = offerId)
        val now = LocalDateTime.now()
        // 주문이 없거나, 구매자가 신청한 구매주문건이 아닐경우 에러
        if (targetOffer == null || targetOffer.buyer?.id != user.id || targetOffer.status != OfferStatus.OFFERED) {
            throw NotFoundException("주문이 없거나, 주문의 구매 신청자와 요청한 회원 정보가 다릅니다. (제안 상태가 아닌 주문건은 취소할 수 없습니다.)")
        }
        val result = repo.save(targetOffer.copy(updatedAt = now, status = OfferStatus.PENDING, buyer = null))
        appConfig.logger.info("[TWINKOREA API] 유저#${user.id}가 구매 주문건#${targetOffer.id}에 대한 구매 신청을 취소하였습니다.")
        return result
    }

    fun getAllOffersByAddress(request: OfferSearchRequest, pageRequest: Pageable): Page<OfferDto> {
        return offerDslRepo.findAll(request, pageRequest)
    }

    fun getPopularOffers(): List<PopularOfferDto> {
        return offerDslRepo.findPopularOffer()
    }

    fun getOfferToMe(user: User, pageRequest: Pageable): Page<OfferDto> {
        return offerDslRepo.findOfferToMe(user, pageRequest)
    }

    fun getOfferFromMe(user: User, pageRequest: Pageable): Page<OfferDto> {
        return offerDslRepo.findOfferFromMe(user, pageRequest)
    }
}
