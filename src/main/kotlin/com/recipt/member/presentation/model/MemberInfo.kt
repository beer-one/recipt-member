package com.recipt.member.presentation.model

import com.recipt.member.domain.member.entity.Member

data class MemberInfo (
    val email: String,
    val no: Int,
    val nickname: String
) {
    constructor(member: Member): this(member.email, member.no, member.nickname)
}