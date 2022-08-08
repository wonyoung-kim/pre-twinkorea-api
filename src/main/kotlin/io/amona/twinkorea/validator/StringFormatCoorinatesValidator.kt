package io.amona.twinkorea.validator

import io.amona.twinkorea.annotation.StringFormatCoordinates
import org.slf4j.LoggerFactory
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class StringFormatCoorinatesValidator: ConstraintValidator<StringFormatCoordinates, String> {
    private val logger = LoggerFactory.getLogger(StringFormatCoordinates::class.java)
    private var pattern: String? = null
    private var message: String? = null

    override fun initialize(constraintAnnotation: StringFormatCoordinates?) {
        this.pattern = constraintAnnotation?.pattern
        this.message = constraintAnnotation?.message
    }

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        val pattern = pattern!!.toRegex()
        if (pattern.containsMatchIn(value.toString())) {
            return true
        } else {
            logger.error("Request Failed :: $message \n --> 입력값: $value")
            throw NumberFormatException("Request Failed :: $message \n --> 입력값: $value")
        }
    }
}