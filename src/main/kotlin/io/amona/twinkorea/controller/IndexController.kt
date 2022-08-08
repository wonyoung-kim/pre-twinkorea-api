package io.amona.twinkorea.controller


import io.amona.twinkorea.service.PreOrderService
import io.amona.twinkorea.transformer.JSONResponse
import io.amona.twinkorea.transformer.ResponseTransformer
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import springfox.documentation.annotations.ApiIgnore

@ApiIgnore
@RequestMapping("/default")
@RestController
class IndexController(val preOrderService: PreOrderService) {
    @GetMapping("/health-check")
    fun getAreaId(): ResponseEntity<JSONResponse> {
        return ResponseTransformer.successResponse("ok")
    }
}