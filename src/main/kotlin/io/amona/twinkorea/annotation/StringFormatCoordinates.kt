package io.amona.twinkorea.annotation

import io.amona.twinkorea.validator.StringFormatCoorinatesValidator
import javax.validation.Constraint
import javax.validation.Payload
import kotlin.reflect.KClass

@Constraint(validatedBy=[StringFormatCoorinatesValidator::class])
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER,
)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class StringFormatCoordinates (
    val pattern: String = "^[0-9.]+(,[0-9.]+)\$",
    val message: String = "꼭지점 좌표 속성은 숫자로 구성된 x와 y 좌표를 \",\"(쉼표)로 구분한 문자열 값으로 입력해야합니다.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
    )