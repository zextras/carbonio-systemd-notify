// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.systemd;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sends sd_notify readiness and status messages to systemd via libsystemd FFM binding.
 *
 * <p>When running under systemd with {@code Type=notify}, the main process must send {@code
 * READY=1} to signal that the service is fully started. This class calls {@code sd_notify(3)}
 * directly from the JVM process, which allows the tightest {@code NotifyAccess=main} setting.
 *
 * <p>When {@code NOTIFY_SOCKET} is not set (e.g. running outside systemd, in dev/test), all calls
 * are guaranteed no-ops — no native library is loaded, no FFM code executes.
 *
 * <p>Requires Java 22+ (FFM is stable from JEP 454). The JVM must be started with
 * {@code --enable-native-access=ALL-UNNAMED} to permit the FFM downcall.
 */
public class SystemdNotify {

  private static final Logger LOG = Logger.getLogger(SystemdNotify.class.getName());
  private static final MethodHandle SD_NOTIFY;

  static {
    MethodHandle handle = null;
    String notifySocket = System.getenv("NOTIFY_SOCKET");
    if (notifySocket != null && !notifySocket.isEmpty()) {
      try {
        SymbolLookup lib = SymbolLookup.libraryLookup("libsystemd.so.0", Arena.global());
        handle =
            Linker.nativeLinker()
                .downcallHandle(
                    lib.find("sd_notify").orElseThrow(),
                    FunctionDescriptor.of(
                        ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
      } catch (Throwable e) {
        LOG.log(Level.FINE, "libsystemd not available, sd_notify disabled: {0}", e.getMessage());
      }
    }
    SD_NOTIFY = handle;
  }

  private SystemdNotify() {}

  /**
   * Notify systemd that the service is ready, with an optional human-readable status message.
   *
   * <p>The status string is displayed by {@code systemctl status <unit>}.
   *
   * @param status status text, or {@code null} to send only READY=1
   */
  public static void ready(String status) {
    String msg = "READY=1";
    if (status != null && !status.isEmpty()) {
      msg += "\nSTATUS=" + status;
    }
    notify(msg);
  }

  private static void notify(String state) {
    if (SD_NOTIFY == null) {
      return;
    }
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment msg = arena.allocateFrom(state);
      int rc = (int) SD_NOTIFY.invoke(0, msg);
      if (rc < 0) {
        LOG.log(Level.WARNING, "sd_notify failed with rc={0}", rc);
      }
    } catch (Throwable e) {
      LOG.log(Level.WARNING, "sd_notify call failed: {0}", e.getMessage());
    }
  }
}
