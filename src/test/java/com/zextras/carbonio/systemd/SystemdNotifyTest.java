// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.systemd;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class SystemdNotifyTest {

  @Test
  void extendTimeoutMessage_withStatus() {
    assertEquals(
        "EXTEND_TIMEOUT_USEC=120000000\nSTATUS=Running database migrations",
        SystemdNotify.extendTimeoutMessage(
            Duration.ofSeconds(120), "Running database migrations"));
  }

  @Test
  void extendTimeoutMessage_withoutStatus() {
    assertEquals(
        "EXTEND_TIMEOUT_USEC=1500000",
        SystemdNotify.extendTimeoutMessage(Duration.ofMillis(1500), null));
    assertEquals(
        "EXTEND_TIMEOUT_USEC=1500000",
        SystemdNotify.extendTimeoutMessage(Duration.ofMillis(1500), ""));
  }

  @Test
  void extendTimeoutMessage_microsecondResolution() {
    assertEquals(
        "EXTEND_TIMEOUT_USEC=1",
        SystemdNotify.extendTimeoutMessage(Duration.ofNanos(1_999), null));
  }

  @Test
  void extendTimeoutMessage_rejectsInvalidDurations() {
    assertThrows(
        IllegalArgumentException.class, () -> SystemdNotify.extendTimeoutMessage(null, "x"));
    assertThrows(
        IllegalArgumentException.class,
        () -> SystemdNotify.extendTimeoutMessage(Duration.ZERO, "x"));
    assertThrows(
        IllegalArgumentException.class,
        () -> SystemdNotify.extendTimeoutMessage(Duration.ofSeconds(-1), "x"));
    assertThrows(
        IllegalArgumentException.class,
        () -> SystemdNotify.extendTimeoutMessage(Duration.ofNanos(999), "x"));
    assertThrows(
        IllegalArgumentException.class,
        () -> SystemdNotify.extendTimeoutMessage(Duration.ofSeconds(Long.MAX_VALUE), "x"));
  }

  @Test
  void statusMessage_buildsLineOrNull() {
    assertEquals("STATUS=Migration 3/10", SystemdNotify.statusMessage("Migration 3/10"));
    assertNull(SystemdNotify.statusMessage(null));
    assertNull(SystemdNotify.statusMessage(""));
  }

  @Test
  void statusMessage_stripsNewlines() {
    assertEquals("STATUS=a b  c", SystemdNotify.statusMessage("a\nb\r\nc"));
  }

  @Test
  void publicMethods_areNoOpWithoutNotifySocket() {
    assertDoesNotThrow(() -> SystemdNotify.status("x"));
    assertDoesNotThrow(() -> SystemdNotify.status(null));
    assertDoesNotThrow(() -> SystemdNotify.extendTimeout(Duration.ofSeconds(1), "x"));
    assertThrows(
        IllegalArgumentException.class, () -> SystemdNotify.extendTimeout(Duration.ZERO, "x"));
  }
}
