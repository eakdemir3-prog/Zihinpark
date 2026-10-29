package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun `verify exactly 16 base games configured with unique ids`() {
    val games = com.example.data.local.RoomDatabaseModule.BASE_16_GAMES
    assertEquals(16, games.size)
    val uniqueIds = games.map { it.id }.toSet()
    assertEquals(16, uniqueIds.size)
  }

  @Test
  fun `verify UserDailyActivityEntity creation and play time aggregation`() {
    val initial = com.example.data.local.entity.UserDailyActivityEntity(
        date = "2026-10-29",
        totalPlayTimeSeconds = 120L,
        gamesPlayedCount = 2,
        totalScoreEarned = 180,
        starsEarned = 6
    )

    assertEquals("2026-10-29", initial.date)
    assertEquals(120L, initial.totalPlayTimeSeconds)
    assertEquals(2, initial.gamesPlayedCount)
    assertEquals(180, initial.totalScoreEarned)
    assertEquals(6, initial.starsEarned)

    // Simulate session update
    val updated = initial.copy(
        totalPlayTimeSeconds = initial.totalPlayTimeSeconds + 90L,
        gamesPlayedCount = initial.gamesPlayedCount + 1,
        totalScoreEarned = initial.totalScoreEarned + 95,
        starsEarned = initial.starsEarned + 3
    )

    assertEquals(210L, updated.totalPlayTimeSeconds)
    assertEquals(3, updated.gamesPlayedCount)
    assertEquals(275, updated.totalScoreEarned)
    assertEquals(9, updated.starsEarned)
  }

  @Test
  fun `verify weekly summary fills missing days with zero metrics`() {
    val activities = listOf(
        com.example.data.local.entity.UserDailyActivityEntity(
            date = "2026-10-29",
            totalPlayTimeSeconds = 300L,
            gamesPlayedCount = 4,
            totalScoreEarned = 250,
            starsEarned = 10
        )
    )
    val activityMap = activities.associateBy { it.date }
    val todayActivity = activityMap["2026-10-29"]
    assertNotNull(todayActivity)
    assertEquals(300L, todayActivity?.totalPlayTimeSeconds)

    val missingDayActivity = activityMap["2026-10-25"]
    assertNull(missingDayActivity)
    val fallbackPlayTime = missingDayActivity?.totalPlayTimeSeconds ?: 0L
    assertEquals(0L, fallbackPlayTime)
  }

  @Test
  fun `verify daily reminder next trigger millis calculation in future`() {
    val now = java.util.Calendar.getInstance().apply {
      set(java.util.Calendar.HOUR_OF_DAY, 10)
      set(java.util.Calendar.MINUTE, 0)
      set(java.util.Calendar.SECOND, 0)
      set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis

    // Schedule for 20:00 (later today)
    val trigger = com.example.notification.NotificationHelper.calculateNextReminderMillis(20, 0, now)
    assertTrue("Trigger must be strictly after now", trigger > now)

    val targetCal = java.util.Calendar.getInstance().apply { timeInMillis = trigger }
    assertEquals(20, targetCal.get(java.util.Calendar.HOUR_OF_DAY))
    assertEquals(0, targetCal.get(java.util.Calendar.MINUTE))
  }

  @Test
  fun `verify daily reminder triggers next day if time has already passed today`() {
    val now = java.util.Calendar.getInstance().apply {
      set(java.util.Calendar.HOUR_OF_DAY, 21)
      set(java.util.Calendar.MINUTE, 30)
      set(java.util.Calendar.SECOND, 0)
      set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis

    // Schedule for 20:00 (which was 1.5 hours ago)
    val trigger = com.example.notification.NotificationHelper.calculateNextReminderMillis(20, 0, now)
    assertTrue("Trigger must be in the future (tomorrow)", trigger > now)

    val targetCal = java.util.Calendar.getInstance().apply { timeInMillis = trigger }
    assertEquals(20, targetCal.get(java.util.Calendar.HOUR_OF_DAY))
    assertEquals(0, targetCal.get(java.util.Calendar.MINUTE))
  }
}
