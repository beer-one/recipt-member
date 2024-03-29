package com.recipt.member.application.member

import com.recipt.core.exception.member.DuplicatedMemberException
import com.recipt.core.exception.member.MemberNotFoundException
import com.recipt.core.exception.member.WrongPasswordException
import com.recipt.member.application.member.dto.ProfileModifyCommand
import com.recipt.member.application.member.dto.SignUpCommand
import com.recipt.member.domain.member.entity.FollowerMapping
import com.recipt.member.domain.member.entity.Member
import com.recipt.member.domain.member.repository.FollowerMappingRepository
import com.recipt.member.domain.member.repository.MemberRepository
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import reactor.test.StepVerifier

@ExtendWith(MockKExtension::class)
internal class MemberCommandServiceTest {
    @MockK
    private lateinit var memberRepository: MemberRepository

    @MockK
    private lateinit var followerMappingRepository: FollowerMappingRepository

    @MockK
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var memberCommandService: MemberCommandService

    private val testPassword = "password"
    private val newTestPassword = "password1"

    @BeforeEach
    fun setUp() {
        memberCommandService = MemberCommandService(
            memberRepository = memberRepository,
            followerMappingRepository = followerMappingRepository,
            passwordEncoder = passwordEncoder
        )

        /** 귀찮으니까 패스워드 인코더 모킹 여기서.. **/
        every { passwordEncoder.matches(testPassword, testPassword) } returns true
        every { passwordEncoder.matches(not(testPassword), testPassword) } returns false
        every { passwordEncoder.encode(testPassword) } returns testPassword
        every { passwordEncoder.encode(newTestPassword) } returns newTestPassword

        /** 나중에 도입 필요성 느끼면 쓰게 놔둠.. **/
//        every { transactionTemplate.execute(any<TransactionCallback<*>>()) } answers {
//            firstArg<TransactionCallback<*>>().doInTransaction(mockk())
//        }
    }

    @Test
    fun `회원가입 테스트`() {
        val command = SignUpCommand(
            email = "email@email.com",
            password = "password",
            name = "홍길동",
            nickname = "nickname",
            mobileNo = "010-1234-5678"
        )
        val created = Member.create(command)

        every { memberRepository.findByEmailOrNickname(command.email, command.nickname) } returns null
        every { memberRepository.save(created) } returns created

        val result = memberCommandService.signUp(command)

        StepVerifier.create(result)
            .expectNext(Unit)
            .expectComplete()
            .verify()

        verify(exactly = 1) {
            memberRepository.findByEmailOrNickname(command.email, command.nickname)
            memberRepository.save(created)
        }
    }

    @Test
    fun `회원가입 테스트(실패, 이메일 중복)`() {
        val command = SignUpCommand(
            email = "email@email.com",
            password = "password",
            name = "홍길동",
            nickname = "nickname",
            mobileNo = "010-1234-5678"
        )

        val existedMember = mockk<Member> {
            every { email } returns command.email
        }

        every { memberRepository.findByEmailOrNickname(command.email, command.nickname) } returns existedMember
        every { memberRepository.save(any<Member>()) } returns mockk()

        val result = memberCommandService.signUp(command)

        StepVerifier.create(result)
            .expectError(DuplicatedMemberException::class.java)
            .verify()


        verify(exactly = 1) {
            memberRepository.findByEmailOrNickname(command.email, command.nickname)
        }
        verify(exactly = 0) {
            memberRepository.save(any<Member>())
        }
    }

    @Test
    fun `회원가입 테스트(실패, 닉네임 중복)`() {
        val command = SignUpCommand(
            email = "email@email.com",
            password = "password",
            name = "홍길동",
            nickname = "nickname",
            mobileNo = "010-1234-5678"
        )

        val existedMember = mockk<Member> {
            every { email } returns "A${command.email}"
            every { nickname } returns command.nickname
        }

        every { memberRepository.findByEmailOrNickname(command.email, command.nickname) } returns existedMember
        every { memberRepository.save(any<Member>()) } returns mockk()

        val result = memberCommandService.signUp(command)

        StepVerifier.create(result)
            .expectError(DuplicatedMemberException::class.java)
            .verify()

        verify(exactly = 1) {
            memberRepository.findByEmailOrNickname(command.email, command.nickname)
        }
        verify(exactly = 0) {
            memberRepository.save(any<Member>())
        }
    }

    @Test
    fun `회원 정보 변경`() {
        val memberNo = 1

        val command = mockk<ProfileModifyCommand> {
            every { password } returns testPassword
            every { newPassword } returns newTestPassword
        }
        val member = mockk<Member> {
            every { modify(any(), any()) } just runs
            every { password } returns testPassword
        }

        every { memberRepository.findByIdOrNull(memberNo) } returns member
        every { memberRepository.findByIdOrNull(not(memberNo)) } returns null
        every { memberRepository.save(any<Member>()) } returns mockk()


        val result = memberCommandService.modify(memberNo, command)

        StepVerifier.create(result)
            .expectNext(Unit)
            .expectComplete()
            .verify()

        verify(exactly = 1) {
            member.modify(any(), any())
            memberRepository.save(any<Member>())
        }
    }

    @Test
    fun `회원 정보 변경 (실패, 회원 없음)`() {
        val memberNo = 1
        val notExistMemberNo = 2

        val command = mockk<ProfileModifyCommand> {
            every { password } returns testPassword
            every { newPassword } returns newTestPassword
        }
        val member = mockk<Member> {
            every { modify(any(), any()) } just runs
            every { password } returns testPassword
        }

        every { memberRepository.findByIdOrNull(memberNo) } returns member
        every { memberRepository.findByIdOrNull(not(memberNo)) } returns null

        val result = memberCommandService.modify(notExistMemberNo, command)

        StepVerifier.create(result)
            .expectError(MemberNotFoundException::class.java)
            .verify()

        verify(exactly = 0) {
            member.modify(any(), any())
        }
    }

    @Test
    fun `회원 정보 변경 (실패,  비번 틀림)`() {
        val memberNo = 1

        val wrongPasswordCommand = mockk<ProfileModifyCommand> {
            every { password } returns "wrong$testPassword"
            every { newPassword } returns newTestPassword
        }

        val member = mockk<Member> {
            every { modify(any(), any()) } just runs
            every { password } returns testPassword
        }

        every { memberRepository.findByIdOrNull(memberNo) } returns member
        every { memberRepository.findByIdOrNull(not(memberNo)) } returns null

        every { memberRepository.save(any<Member>()) } returns mockk()

        val result = memberCommandService.modify(memberNo, wrongPasswordCommand)

        StepVerifier.create(result)
            .expectError(WrongPasswordException::class.java)
            .verify()

        verify(exactly = 0) {
            member.modify(any(), any())
        }
    }

    @Test
    fun `팔로우`() {
        val memberNo = 1
        val followerNo = 2

        every { memberRepository.existsById(followerNo) } returns true
        every { memberRepository.existFollowing(memberNo, followerNo) } returns false
        every { followerMappingRepository.save(any<FollowerMapping>()) } returns mockk()

        val result = memberCommandService.follow(from = memberNo, to = followerNo)

        StepVerifier.create(result)
            .expectNext(Unit)
            .expectComplete()
            .verify()

        verify(exactly = 1) {
            memberRepository.existsById(followerNo)
            memberRepository.existFollowing(memberNo, followerNo)
            followerMappingRepository.save(any<FollowerMapping>())
        }
    }

    @Test
    fun `팔로우 (실패, 팔로우할 회원이 없는 경우)`() {
        val memberNo = 1
        val followerNo = 2

        every { memberRepository.existsById(followerNo) } returns false
        every { memberRepository.existFollowing(memberNo, followerNo) } returns false
        every { followerMappingRepository.save(any<FollowerMapping>()) } returns mockk()

        val result = memberCommandService.follow(from = memberNo, to = followerNo)

        StepVerifier.create(result)
            .expectError(MemberNotFoundException::class.java)
            .verify()

        verify(exactly = 1) {
            memberRepository.existsById(followerNo)
        }
        verify(exactly = 0) {
            memberRepository.existFollowing(memberNo, followerNo)
            followerMappingRepository.save(any<FollowerMapping>())
        }
    }

    @Test
    fun `언팔로우`() {
        val memberNo = 1
        val followerNo = 2

        val followerMapping = FollowerMapping(
            memberNo = memberNo,
            followerNo = followerNo
        )

        every { memberRepository.existsById(followerNo) } returns true
        every {
            followerMappingRepository.findByMemberNoAndFollowerNo(memberNo, followerNo)
        } returns followerMapping
        every { followerMappingRepository.deleteById(any()) } just runs

        val result = memberCommandService.unfollow(from = memberNo, to = followerNo)

        StepVerifier.create(result)
            .expectNext(Unit)
            .verifyComplete()

        verify(exactly = 1) {
            memberRepository.existsById(followerNo)
            followerMappingRepository.findByMemberNoAndFollowerNo(memberNo, followerNo)
            followerMappingRepository.deleteById(any())
        }
    }

    @Test
    fun `언팔로우 (실패, 언팔로우할 회원이 없는 경우)`() {
        val memberNo = 1
        val followerNo = 2

        val followerMapping = FollowerMapping(
            memberNo = memberNo,
            followerNo = followerNo
        )

        every { memberRepository.existsById(followerNo) } returns false
        every {
            followerMappingRepository.findByMemberNoAndFollowerNo(memberNo, followerNo)
        } returns followerMapping
        every { followerMappingRepository.deleteById(any()) } just runs

        val result = memberCommandService.unfollow(from = memberNo, to = followerNo)

        StepVerifier.create(result)
            .expectError(MemberNotFoundException::class.java)
            .verify()

        verify(exactly = 1) {
            memberRepository.existsById(followerNo)
        }
        verify(exactly = 0) {
            followerMappingRepository.findByMemberNoAndFollowerNo(memberNo, followerNo)
            followerMappingRepository.deleteById(any())
        }
    }
}