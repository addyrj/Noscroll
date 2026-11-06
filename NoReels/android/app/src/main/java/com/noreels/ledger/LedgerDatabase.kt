package com.noreels.ledger

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Local SQLite ledger. Written only by the Kotlin engine; React Native reads it through the
 * bridge. Plain SQLiteOpenHelper keeps the build free of annotation processors.
 */
class LedgerDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

  override fun onCreate(db: SQLiteDatabase) {
    db.execSQL("CREATE TABLE kv (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
    db.execSQL(
        "CREATE TABLE daily (date TEXT PRIMARY KEY, used_sec INTEGER NOT NULL DEFAULT 0, " +
            "sessions INTEGER NOT NULL DEFAULT 0, blocked INTEGER NOT NULL DEFAULT 0, " +
            "limit_sec INTEGER NOT NULL)")
    db.execSQL(
        "CREATE TABLE sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT NOT NULL, " +
            "app TEXT NOT NULL, start_ts INTEGER NOT NULL, end_ts INTEGER NOT NULL, " +
            "seconds_used INTEGER NOT NULL)")
    db.execSQL(
        "CREATE TABLE event_log (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER NOT NULL, " +
            "kind TEXT NOT NULL, app TEXT NOT NULL, detail TEXT NOT NULL)")
  }

  override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

  fun getKv(key: String): String? =
      readableDatabase.rawQuery("SELECT value FROM kv WHERE key = ?", arrayOf(key)).use {
        if (it.moveToFirst()) it.getString(0) else null
      }

  fun putKvs(values: Map<String, String>) {
    val db = writableDatabase
    db.beginTransaction()
    try {
      for ((k, v) in values) {
        db.insertWithOnConflict(
            "kv",
            null,
            ContentValues().apply {
              put("key", k)
              put("value", v)
            },
            SQLiteDatabase.CONFLICT_REPLACE)
      }
      db.setTransactionSuccessful()
    } finally {
      db.endTransaction()
    }
  }

  fun upsertDaily(row: DailyRow) {
    writableDatabase.insertWithOnConflict(
        "daily",
        null,
        ContentValues().apply {
          put("date", row.date)
          put("used_sec", row.usedSec)
          put("sessions", row.sessions)
          put("blocked", row.blocked)
          put("limit_sec", row.limitSec)
        },
        SQLiteDatabase.CONFLICT_REPLACE)
  }

  fun getDaily(date: String): DailyRow? = getDailyRange(date, date).firstOrNull()

  fun getDailyRange(from: String, to: String): List<DailyRow> =
      readableDatabase
          .rawQuery(
              "SELECT date, used_sec, sessions, blocked, limit_sec FROM daily " +
                  "WHERE date >= ? AND date <= ? ORDER BY date",
              arrayOf(from, to))
          .use { c ->
            buildList {
              while (c.moveToNext()) {
                add(DailyRow(c.getString(0), c.getInt(1), c.getInt(2), c.getInt(3), c.getInt(4)))
              }
            }
          }

  fun firstDailyDate(): String? =
      readableDatabase.rawQuery("SELECT MIN(date) FROM daily", null).use {
        if (it.moveToFirst() && !it.isNull(0)) it.getString(0) else null
      }

  fun insertSession(s: SessionEntity) {
    writableDatabase.insert(
        "sessions",
        null,
        ContentValues().apply {
          put("date", s.date)
          put("app", s.app)
          put("start_ts", s.startTs)
          put("end_ts", s.endTs)
          put("seconds_used", s.secondsUsed)
        })
  }

  fun insertLog(entry: LogEntry) {
    val db = writableDatabase
    db.insert(
        "event_log",
        null,
        ContentValues().apply {
          put("ts", entry.ts)
          put("kind", entry.kind)
          put("app", entry.app)
          put("detail", entry.detail)
        })
    db.execSQL(
        "DELETE FROM event_log WHERE id NOT IN (SELECT id FROM event_log ORDER BY id DESC LIMIT $MAX_LOG_ROWS)")
  }

  fun recentLogs(limit: Int): List<LogEntry> =
      readableDatabase
          .rawQuery(
              "SELECT ts, kind, app, detail FROM event_log ORDER BY id DESC LIMIT ?",
              arrayOf(limit.toString()))
          .use { c ->
            buildList {
              while (c.moveToNext()) {
                add(LogEntry(c.getLong(0), c.getString(1), c.getString(2), c.getString(3)))
              }
            }
          }

  companion object {
    private const val DB_NAME = "ledger.db"
    private const val DB_VERSION = 1
    private const val MAX_LOG_ROWS = 500

    @Volatile private var instance: LedgerDatabase? = null

    fun get(context: Context): LedgerDatabase =
        instance ?: synchronized(this) { instance ?: LedgerDatabase(context).also { instance = it } }
  }
}
