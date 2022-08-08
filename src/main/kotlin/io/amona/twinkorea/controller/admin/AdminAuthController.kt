package io.amona.twinkorea.controller.admin

import io.amona.twinkorea.annotation.AdminUser
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.request.*
import io.amona.twinkorea.service.AdminIpAuthService
import io.amona.twinkorea.service.AdminService
import io.amona.twinkorea.transformer.JSONResponse
import io.amona.twinkorea.transformer.ResponseTransformer
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import springfox.documentation.annotations.ApiIgnore

@Api(description = "어드민 인증/보안 관련 API")
@RequestMapping("/admin")
@RestController
class AdminAuthController(
    private val service: AdminService,
    private val authService: AdminIpAuthService,
) {
    @ApiOperation(value = "어드민 계정 추가", response = JSONResponse::class, notes = AdminNotes.createNewAdminNote)
    @PostMapping()
    fun createNewAdmin(
        @ApiIgnore @AdminUser user: User,
        @RequestBody request: AdminUserRequest,
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = service.createAdminUser(request = request)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "어드민 계정 비활성화", response = JSONResponse::class, notes = AdminNotes.deactivateAdminNote)
    @DeleteMapping("/{adminId}")
    fun deactivateAdmin(
        @ApiIgnore @AdminUser user: User,
        @PathVariable(name = "adminId") adminId: Long
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = service.deActiveAdminUser(adminId = adminId, user = user)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "어드민 계정 수정", response = JSONResponse::class, notes = AdminNotes.editAdminNote)
    @PatchMapping("/{adminId}")
    fun editAdmin(
        @RequestBody request: AdminUserRequest,
        @ApiIgnore @AdminUser user: User,
        @PathVariable(name = "adminId") adminId: Long
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = service.editAdminUser(request = request, adminId = adminId, user = user)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    /**
     * 어드민 로그인
     */
    @ApiOperation(value = "어드민 로그인", response = JSONResponse::class, notes = AdminNotes.adminLoginNote)
    @PostMapping("/signin")
    fun adminLogin(@RequestBody request: UserRequest): ResponseEntity<JSONResponse> {
        return try {
            val result = service.login(request)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    /**
     * 어드민 회원의 otp secretKey 재발급
     */
    @ApiOperation(value = "otp secret key 재발급", response = JSONResponse::class, notes = AdminNotes.renewOtpSecretKeyNote)
    @PostMapping("/{adminId}/mfa/otp/renew")
    fun renewOtpSecretKey(
        @ApiIgnore @AdminUser user: User,
        @PathVariable(name = "adminId") adminId: Long,
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = service.renewSecretKey(user, adminId)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "어드민 정보 조회", response = JSONResponse::class, notes = AdminNotes.getAdminInfoNote)
    @GetMapping("/{adminId}")
    fun getAdminInfo(
        @ApiIgnore @AdminUser user: User,
        @PathVariable(name = "adminId") adminId: Long,
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = service.getAdminInfo(adminId = adminId)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    @ApiOperation(value = "전체 어드민 회원 리스트 조회", response = JSONResponse::class, notes = AdminNotes.getAdminListNote)
    @GetMapping("/list")
    fun getAdminList(
        @ApiIgnore @AdminUser user: User,
        @ApiParam(value = "이름(닉네임)", example = "김동현") @RequestParam(name = "nickname", required = false) nickname: String?,
        @ApiParam(value = "아이디(이메일)", example = "gisa@gmail.com") @RequestParam(name = "email", required = false) email: String?,
        @RequestParam(name = "page", required = true) page: Int,
        @RequestParam(name = "size", required = true) size: Int,
    ): ResponseEntity<JSONResponse> {
        return try {
            val adminSearchRequest = UserSearchRequest(nickname, email)
            val result = service.getAdminList(adminSearchRequest, PageRequest(page, size).of())
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    /**
     * IP 화이트 리스트의 목록을 불러옵니다.
     */
    @ApiOperation(value = "IP 화이트 리스트 조회", response = JSONResponse::class)
    @GetMapping("/ip-auth/white-list")
    fun getIpWhiteList(
        @ApiIgnore @AdminUser user: User,
        @RequestParam(name = "page", required = true) page: Int,
        @RequestParam(name = "size", required = true) size: Int,
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = authService.getListOfIpWhiteList(PageRequest(page, size).of())
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    /**
     * IP 화이트 리스트에 새로운 IP를 추가합니다.
     */
    @ApiOperation(value = "IP 화이트 리스트 추가", response = JSONResponse::class)
    @PostMapping("/ip-auth/white-list")
    fun addIpWhiteList(
        @ApiIgnore @AdminUser user: User,
        @RequestBody request: IpWhiteListRequest,
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = authService.addIpToWhiteList(request)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    /**
     * IP 화이트 리스트의 설명을 수정합니다. (IP 주소의 수정은 지원하지 않으며, IP 주소를 수정하려면 삭제후 추가 해야함)
     */
    @ApiOperation(value = "IP 화이트 리스트 수정", response = JSONResponse::class)
    @PostMapping("/ip-auth/white-list/edit")
    fun editIpWhiteList(
        @ApiIgnore @AdminUser user: User,
        @RequestBody request: IpWhiteListRequest
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = authService.editIpWhiteListDescription(request)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }

    /**
     * 등록된 IP를 삭제합니다.
     */
    @ApiOperation(value = "IP 삭제", response = JSONResponse::class)
    @DeleteMapping("/ip-auth/white-list")
    fun deleteWhiteList(
        @ApiIgnore @AdminUser user: User,
        @RequestBody request: IpWhiteListRequest
    ): ResponseEntity<JSONResponse> {
        return try {
            val result = authService.deleteIpFromWhiteList(request.ip)
            ResponseTransformer.successResponse(result)
        } catch (e: Exception) {
            ResponseTransformer.errorWrapper(e)
        }
    }
}