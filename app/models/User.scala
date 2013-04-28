/**
 * Created by Anton Fagerberg.
 * anton@antonfagerberg.com | www.antonfagerberg.com
 */
package models

import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKeyFactory
import java.security.SecureRandom
import play.api.Play.current
import play.api.db._
import anorm._
import anorm.SqlParser._

/** User account.
  *
  * @param email  E-mail address of User.
  */
case class User(email: String)

object User {
  val userParser =
    get[String]("user.email") map {
      case email => User(email)
    }

  /** Create a new User account (save to database).
    *
    * The Password will be hashed by the hash-function in the User object before being inserted.
    * A new random salt will be created.
    *
    * @param email    E-mail address of user (no validation is performed, max 255 chars).
    * @param password Password clear-text.
    * @return         Was the information saved to the database?
    */
  def create(email: String, password: Array[Char]): Boolean = {
    DB.withConnection { implicit connection =>
      val salt = randomSalt

      SQL(
        """
          |INSERT INTO
          | user
          |SET
          | user.email = {email},
          | user.password = {password},
          | user.salt = {salt}
        """.stripMargin
      ).on(
        'email -> email,
        'password -> hash(password, salt),
        'salt -> salt
      ).executeUpdate() == 1
    }
  }

  /** Authenticate e-mail address for a user.
    *
    * This method is used in authenticated request to determine if the e-mail (from session) is valid.
    *
    * @param email  E-mail address.
    * @return       User if e-mail was valid.
    */
  def authenticateEmail(email: String): Option[User] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          |SELECT
          | user.email
          |FROM
          | user
          |WHERE
          | user.email = {email}
          |LIMIT 1
        """.stripMargin
      ).on(
        'email -> email
      ).as(
        userParser singleOpt
      )
    }
  }

  /** Authenticate a user by validating email and password (clear text).
    *
    * @param email    E-mail address.
    * @param password Password clear text (the corresponding salt from database will be used).
    * @return
    */
  def authenticate(email: String, password: Array[Char]): Boolean = {
    DB.withConnection { implicit connection =>
      // Used to get an Array[Byte] from database with Anorm.
      // For reference, see: http://stackoverflow.com/questions/12109278/how-to-extract-binary-information-from-a-database-using-anorm-with-scala
      implicit def rowToByteArray: Column[Array[Byte]] = Column.nonNull {
        (value, meta) =>
          val MetaDataItem(qualified, nullable, clazz) = meta
          value match {
            case o: Array[Byte] => Right(o)
            case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to Array[Byte] for column " + qualified))
          }
      }

      /** User from database to match against.
        *
        * @param email    E-mail address.
        * @param password Password (hashed).
        * @param salt     Salt used when hashing password.
        */
      case class ValidationUser(email: String, password: Array[Byte], salt: Array[Byte])

      val validationUserParser =
        get[String]("user.email") ~
          get[Array[Byte]]("user.password")(implicitly[Column[Array[Byte]]]) ~
          get[Array[Byte]]("user.salt")(implicitly[Column[Array[Byte]]]) map {
          case email ~ password ~ salt => ValidationUser(email, password, salt)
        }


      val validationUser =
          SQL(
          """
            |SELECT
            | user.email,
            | user.password,
            | user.salt
            |FROM
            | user
            |WHERE
            | user.email = {email}
            |LIMIT 1
          """.stripMargin
        ).on(
          'email -> email
        ).as(
          validationUserParser singleOpt
        )

      validationUser.isDefined && validationUser.get.password.sameElements(hash(password, validationUser.get.salt))
    }
  }

  /** Returns byte array hash created with PBKDF2WithHmacSHA1.
    *
    * @param clearText  Text to be hashed.
    * @param salt       Salt used when hashing the clearText.
    * @return           Hashed clearText.
    */
  private def hash(clearText: Array[Char], salt: Array[Byte]): Array[Byte] = {
    // Read about these values for PBKDF2 before changing iterationCount or keyLength.
    // Beware of changing these values if there are existing users in the database (you might break them).
    val iterationCount = 20480
    val keyLength = 160

    val spec = new PBEKeySpec(clearText, salt, iterationCount, keyLength)
    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded
  }

  /** Generate a pseudo-random salt used when hashing passwords created with SHA1PRNG.
    *
    * The salt is considered a non-secret and can be stored with the passwords in the database.
    * When a password is changed, the salt can, but does not have to be modified.
    *
    * @return Pseudo-random generated salt.
    */
  private def randomSalt: Array[Byte] = {
    val numberOfBytes = 20
    val random = SecureRandom.getInstance("SHA1PRNG")
    val salt = new Array[Byte](numberOfBytes)
    random.nextBytes(salt)
    salt
  }
}