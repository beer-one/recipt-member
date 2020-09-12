package com.recipt.member.presentation.exception.request

import com.recipt.member.presentation.exception.ReciptException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
class InvalidParameterException(val parameterName: String):
    ReciptException(RequestErrorCode.INVALID_PARAMETER, parameterName)