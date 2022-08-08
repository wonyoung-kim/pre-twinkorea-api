package io.amona.twinkorea.service

import io.amona.twinkorea.auth.JwtTokenProvider
import io.amona.twinkorea.configuration.AppConfig
import io.amona.twinkorea.domain.Bookmark
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.dtos.MyInfoDto
import io.amona.twinkorea.dtos.UserReferralDto
import io.amona.twinkorea.enums.SnsProvider
import io.amona.twinkorea.exception.AuthenticationException
import io.amona.twinkorea.exception.DuplicatedException
import io.amona.twinkorea.exception.NotFoundException
import io.amona.twinkorea.exception.NotNullException
import io.amona.twinkorea.repository.BookmarkRepository
import io.amona.twinkorea.repository.UserRepository
import io.amona.twinkorea.request.UserRequest
import io.amona.twinkorea.response.DeactivateResponse
import io.amona.twinkorea.response.LoginResponse
import io.amona.twinkorea.service.external.AuthService
import io.amona.twinkorea.transformer.UserTransformer
import io.amona.twinkorea.utils.TOPT.TOTPTokenGenerator
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class UserService (private val repo: UserRepository,
                   private val bookmarkRepo: BookmarkRepository,
                   private val transformer: UserTransformer,
                   private val authService: AuthService,
                   private val jwtTokenProvider: JwtTokenProvider,
                   private val appConfig: AppConfig
                   ): UserDetailsService {
    private val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()

    fun readUser(userId: Long): User {
        return repo.findByIdAndDeactivateIsFalse(id = userId)
            ?: throw NotFoundException("입력한 ID로 유저를 찾아낼 수 없습니다.")
    }

    fun getUserBySnsId(snsId: String): User {
        return repo.findBySnsIdAndDeactivateIsFalse(snsId = snsId)
            ?: throw NotFoundException("입력한 ID로 유저를 찾아낼 수 없습니다.")
    }

    /**
     * SNS 회원가입
     */
    @Transactional
    fun createUserWithSns(userRequest: UserRequest): User {
        // 입력값 검증
        if (userRequest.email == null ||
            userRequest.snsKey == null ||
            userRequest.snsProvider == null ||
            userRequest.nickname == null ||
            userRequest.phoneNumber == null) {throw NotNullException("snsKey, snsProvide, nickname, phoneNumber 은 필수 입력값입니다.")}
        // 중복 체크
        val snsId = authService.getSnsId(snsProvider = userRequest.snsProvider, userSnsKey = userRequest.snsKey)
        val user = repo.findBySnsIdAndDeactivateIsFalse(snsId = snsId)
        checkDuplicatedUser(user)

        val transformed = transformer.from(userRequest)
        val now = LocalDateTime.now()

        // refresh_token 생성
        val refreshToken = jwtTokenProvider.createRefreshToken(snsId)

        // 유저 생성
        val newUser = repo.save(transformed.copy(
            snsId = snsId,
            couponCount = transformed.couponCount,
            refreshToken = refreshToken,
        ))

        // 기본 북마크 그룹 생성
        bookmarkRepo.save(Bookmark(
            user = newUser,
            default = true,
            createdAt = now,
            updatedAt = now,
            counts = 0,
        ))

        // 레퍼럴 코드 체크
        var invitedBy: User? = null
        userRequest.referralCode?.let { referralCode ->
            invitedBy = repo.findByReferralCodeAndDeactivateIsFalse(referralCode)
            invitedBy?.let {
                repo.save(it.copy(couponCount = it.couponCount + 1, invitingCount = it.invitingCount + 1))
                repo.save(newUser.copy(couponCount = newUser.couponCount + 1))
            }
        }
        appConfig.logger.info {"[TWINKOREA API] 유저#${newUser.id} 회원가입 완료 / 추천인 ${invitedBy?.id} / 추천코드 ${invitedBy?.referralCode}"}
        return newUser
    }

    /**
     * 이메일 회원가입
     */
    @Transactional
    fun createUserWithEmail(userRequest: UserRequest): User {
        // 입력값 검증
        if (userRequest.email == null ||
            userRequest.pw == null ||
            userRequest.nickname == null ||
            userRequest.phoneNumber == null
        ) {throw NotNullException("email, pw, nickname, phoneNumber는 필수 입력값입니다.")}
        // 중복 체크
        val user = repo.findByEmailAndDeactivateIsFalse(userRequest.email)
        checkDuplicatedUser(user)

        val transformed = transformer.from(userRequest)
        val now = LocalDateTime.now()

        // 유저 생성
        val newUser = repo.save(transformed.copy(
            couponCount = transformed.couponCount,
        ))

        // 유저 생성후 snsID 부여 및 refreshToken 생성
        val localUserSnsId = "LOCAL${newUser.id}"
        val refreshToken = jwtTokenProvider.createRefreshToken(localUserSnsId)
        repo.save(newUser.copy(snsId = localUserSnsId, refreshToken = refreshToken))

        // 기본 북마크 그룹 생성
        bookmarkRepo.save(Bookmark(
            user = newUser,
            default = true,
            createdAt = now,
            updatedAt = now,
        ))

        var invitedBy: User? = null
        userRequest.referralCode?.let { referralCode ->
            invitedBy = repo.findByReferralCodeAndDeactivateIsFalse(referralCode)
            invitedBy?.let {
                repo.save(it.copy(couponCount = it.couponCount + 1, invitingCount = it.invitingCount + 1))
                repo.save(newUser.copy(couponCount = newUser.couponCount + 1))
            }
        }
        appConfig.logger.info {"[TWINKOREA API] 유저#${newUser.id} 회원가입 완료 / 추천인 ${invitedBy?.id}"}
        return newUser
    }

    /**
     * SNS 로그인
     **/
    fun loginWithSns(userRequest: UserRequest): LoginResponse {
        // 입력값 검증
        if (userRequest.snsKey == null || userRequest.snsProvider == null) {throw NotNullException("snsKey, snsProvider 는 필수 입력값입니다.")}
        val snsId = authService.getSnsId(snsProvider = userRequest.snsProvider, userSnsKey = userRequest.snsKey)
        val user = repo.findBySnsIdAndDeactivateIsFalse(snsId)
            ?: throw NotFoundException("요청한 입력값과 매칭되는 회원을 찾을 수 없습니다. 입력값을 확인해주세요.")
        return LoginResponse(
            jwt=jwtTokenProvider.createToken(snsId = snsId),
            user=User(
                id = user.id, snsProvider = user.snsProvider,
                createdAt = user.createdAt, updatedAt = user.updatedAt,
                email = user.email, refreshToken = user.refreshToken, phoneNumber = user.phoneNumber,
                referralCode = user.referralCode
            )
        )
    }

    /**
     * 이메일 로그인
     **/
    fun loginWithPhone(userRequest: UserRequest): LoginResponse {
        // 입력값 검증
        if (userRequest.email == null || userRequest.pw == null) {throw NotNullException("phoneNumber, pw 는 필수 입력값입니다.")}
        val user = repo.findByEmailAndDeactivateIsFalse(userRequest.email)
            ?: throw NotFoundException("요청한 입력값과 매칭되는 회원을 찾을 수 없습니다. 입력값을 확인해주세요.")
        if(passwordEncoder.matches(userRequest.pw, user.pw)) {
            return LoginResponse(
                jwt=jwtTokenProvider.createToken(snsId = user.snsId),
                user=User(id = user.id, email = user.email,
                    createdAt = user.createdAt, updatedAt = user.updatedAt, phoneNumber = user.phoneNumber,
                    refreshToken = user.refreshToken)
            )
        } else {
            throw AuthenticationException("비밀번호가 회원 정보와 일치하지 않습니다.")
        }
    }

    /**
     * 회원 탈퇴
     */
    fun deactivateUser(user: User): DeactivateResponse {
        val snsProvider = SnsProvider.valueOf(user.snsProvider!!)
        val unlinkUser = authService.unlinkSns(snsProvider = snsProvider, userId = user.id)
        return if (unlinkUser) {
            repo.save(user.copy(
                deactivate = true,
                snsId = "deactivated${user.snsId}",
                refreshToken = "",
            ))
            DeactivateResponse(success = true, user = user)
        } else {
            DeactivateResponse(success = false, user = user)
        }
    }

    /**
     * 추천인 코드 불러오기
     */
    fun getReferralCode(user: User): UserReferralDto {
        return UserReferralDto(
            couponCount = user.couponCount,
            referralCode = user.referralCode
        )
    }

    /**
     * 내 정보 불러오기
     */
    fun getMyInfo(user: User): MyInfoDto {
        return MyInfoDto(
            email = user.email,
            phoneNumber = user.phoneNumber,
            nickname = user.nickname,
            popUp = user.preOrderPopup
        )
    }

    /**
     * 경인지역 사전 분양시 할인 가능여부를 판단
     */
    fun checkUserHasPreContractCoupon(user: User?): Boolean {
        // 회원인경우 할인 가능여부 확인하고 할인 가능할 경우 discount = true / 회원 아니거나 할인 불가능한 경우 false
        return user?.let {
            it.preContractCouponCount > 0L
        } ?: false
    }

    /**
     * 중복 회원 체크
     */
    private fun checkDuplicatedUser(user: User?) {
        if (user != null) throw DuplicatedException("이미 가입한 회원입니다.")
    }

    /**
     * for user details
     */
    override fun loadUserByUsername(username: String): UserDetails {
        val result: User? = repo.findBySnsIdAndDeactivateIsFalse(snsId = username)
        if(result != null) {
            return result
        } else {
            throw UsernameNotFoundException("")
        }
    }
}