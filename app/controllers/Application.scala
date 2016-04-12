package controllers

import java.sql.{DriverManager, SQLException}

import play.api._
import play.api.mvc._
import scalikejdbc.{AutoSession, ConnectionPool, GlobalSettings, LoggingSQLAndTimeSettings}

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
    ConnectionPool.singleton(
      s"jdbc:mysql://localhost:3306/relay",
      "root",
      "root"
    )

    try {
      val connection = DriverManager.getConnection(
        s"jdbc:mysql://localhost:3306/relay",
        "root",
        "root"
      )
      connection.prepareStatement(
        s"CREATE DATABASE IF NOT EXISTS relay"
      ).executeUpdate()
      connection.prepareStatement(
        s"use relay"
      ).execute()

      isInitialized = true
    } catch {
      case e: SQLException => Logger.error(
        s"Failed to initialize SqlDB: ${e.getMessage}"
      )
    }
  }
}

class Application extends Controller {
  SqlDB.initialize()

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }
}
