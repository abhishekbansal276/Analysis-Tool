package dao

import javax.inject.Inject
import play.api.libs.mailer.{Email, MailerClient}

class EmailService @Inject()(mailerClient: MailerClient) {

  def sendEmail(to: String, subject: String, body: String): Unit = {
    val email = Email(
      subject = subject,
      from = "Analytics.com <analytic.2903@gmail.com>",
      to = Seq(to),
      bodyText = Some(body)
    )
    mailerClient.send(email)
  }
}