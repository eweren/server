/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.events.user.OnUserEmailVerifiedFirst
import io.tolgee.events.user.OnUserUpdated
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.EmailVerification
import io.tolgee.model.UserAccount
import io.tolgee.repository.EmailVerificationRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Lazy
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.random.Random

@Service
class EmailVerificationService(
  private val tolgeeProperties: TolgeeProperties,
  private val emailVerificationRepository: EmailVerificationRepository,
  private val mailSender: MailSender,
  private val applicationEventPublisher: ApplicationEventPublisher,
) {
  @Lazy
  @Autowired
  lateinit var userAccountService: UserAccountService

  @Transactional
  fun createForUser(
    userAccount: UserAccount,
    callbackUrl: String? = null,
    newEmail: String? = null
  ): EmailVerification? {
    if (tolgeeProperties.authentication.needsEmailVerification) {
      val resultCallbackUrl = getCallbackUrl(callbackUrl)
      val code = generateCode()

      val emailVerification = userAccount.emailVerification?.also {
        it.newEmail = newEmail
        it.code = code
      } ?: EmailVerification(userAccount = userAccount, code = code, newEmail = newEmail)

      emailVerificationRepository.save(emailVerification)
      userAccount.emailVerification = emailVerification

      if (newEmail != null) {
        sendMail(userAccount.id, newEmail, resultCallbackUrl, code, false)
      } else {
        sendMail(userAccount.id, userAccount.username, resultCallbackUrl, code)
      }

      return emailVerification
    }
    return null
  }

  fun check(userAccount: UserAccount) {
    if (
      tolgeeProperties.authentication.needsEmailVerification &&
      userAccount.emailVerification != null
    ) {
      throw AuthenticationException(io.tolgee.constants.Message.EMAIL_NOT_VERIFIED)
    }
  }

  fun verify(userId: Long, code: String) {
    val user = userAccountService.get(userId).orElseThrow { NotFoundException() }
    val old = UserAccountDto.fromEntity(user)
    val emailVerification = user?.emailVerification

    if (emailVerification == null || emailVerification.code != code) {
      throw NotFoundException()
    }

    val newEmail = user.emailVerification?.newEmail
    setNewEmailIfChanged(newEmail, user)

    userAccountService.saveAndFlush(user)
    emailVerificationRepository.delete(emailVerification)

    val isFirstEmailVerification = newEmail == null
    val isEmailChange = newEmail != null

    if (isFirstEmailVerification) {
      applicationEventPublisher.publishEvent(OnUserEmailVerifiedFirst(this, user))
    }

    if (isEmailChange) {
      applicationEventPublisher.publishEvent(OnUserUpdated(this, old, UserAccountDto.fromEntity(user)))
    }
  }

  private fun setNewEmailIfChanged(newEmail: String?, user: UserAccount) {
    newEmail?.let {
      user.username = newEmail
    }
  }

  private fun sendMail(
    userId: Long,
    email: String,
    resultCallbackUrl: String?,
    code: String,
    isSignUp: Boolean = true
  ) {
    val message = SimpleMailMessage()
    message.setTo(email)
    message.subject = "Tolgee e-mail verification"
    val url = "$resultCallbackUrl/$userId/$code"
    message.text = """
                    Hello!
                    ${if (isSignUp) "Welcome to Tolgee!" else ""}
                    
                    To verify your e-mail click on this link: 
                    $url
                    
                    Regards,
                    Tolgee
    """.trimIndent()
    message.from = tolgeeProperties.smtp.from
    mailSender.send(message)
  }

  private fun generateCode(): String {
    val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    val code = (1..50).map { charPool[Random.nextInt(0, charPool.size)] }.joinToString("")
    return code
  }

  private fun getCallbackUrl(callbackUrl: String?): String {
    var resultCallbackUrl = tolgeeProperties.frontEndUrl ?: callbackUrl

    if (resultCallbackUrl == null) {
      throw BadRequestException(io.tolgee.constants.Message.MISSING_CALLBACK_URL)
    }

    resultCallbackUrl += "/login/verify_email"
    return resultCallbackUrl
  }
}
