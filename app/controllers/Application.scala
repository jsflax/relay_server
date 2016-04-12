package controllers

import java.net.URI
import java.sql.{DriverManager, SQLException}

import play.api._
import play.api.mvc._
import scalikejdbc.{AutoSession, ConnectionPool, ConnectionPoolSettings, GlobalSettings, LoggingSQLAndTimeSettings}

/**
  */
object SqlDB {
  // initialize JDBC driver & connection pool
  Class.forName("org.h2.Driver")
  implicit val session = AutoSession

  var isInitialized = false

  def initialize() {
    GlobalSettings.loggingSQLAndTime = LoggingSQLAndTimeSettings(
      enabled = false,
      singleLineMode = false,
      printUnprocessedStackTrace = false,
      stackTraceDepth = 15,
      logLevel = 'error,
      warningEnabled = false,
      warningThresholdMillis = 3000L,
      warningLogLevel = 'warn
    )

    val dbUri = new URI(System.getenv("CLEARDB_DATABASE_URL"))

    val username = dbUri.getUserInfo.split(":")(0)
    val password = dbUri.getUserInfo.split(":")(1)
    val dbUrl = "jdbc:mysql://" + dbUri.getHost + dbUri.getPath

    ConnectionPool.singleton(
      dbUrl,
      username,
      password,
      ConnectionPoolSettings(
        initialSize = 1,
        maxSize = 8
      )
    )

    try {
      val connection = DriverManager.getConnection(
        dbUrl,
        username,
        password
      )
      connection.prepareStatement(
        s"CREATE DATABASE IF NOT EXISTS relay"
      ).executeUpdate()
      connection.prepareStatement(
        s"use relay"
      ).execute()

      isInitialized = true
    } catch {
      case e: SQLException =>
        Logger.error(
          s"Failed to initialize SqlDB: ${e.getMessage}"
        )
        e.printStackTrace()
    }
  }
}

class Application extends Controller {
  SqlDB.initialize()

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }
}
