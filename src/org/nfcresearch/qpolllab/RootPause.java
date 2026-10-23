package org.nfcresearch.qpolllab;

/**
 * Root helper: pause / resume NFC polling with uid 0.
 *
 * 部分 ROM（如红米 Note 12 Turbo / NXP SN100U）的 NfcService 在
 * pausePolling 上强制检查 WRITE_SECURE_SETTINGS，Shell（UID 2000）会被拒绝。
 * 由 worker 通过 su 启动本类，以 root 身份完成关场 / 开场。
 *
 * 用法：
 *   CLASSPATH=<apk> app_process /system/bin org.nfcresearch.qpolllab.RootPause pause 10000
 *   CLASSPATH=<apk> app_process /system/bin org.nfcresearch.qpolllab.RootPause resume
 */
public final class RootPause {
    public static void main(String[] args) throws Exception {
        String action = args.length > 0 ? args[0] : "pause";
        Object binder = Class.forName("android.os.ServiceManager")
                .getMethod("getService", String.class).invoke(null, "nfc");
        if (binder == null) throw new IllegalStateException("no nfc service");
        Class<?> iface = Class.forName("android.nfc.INfcAdapter");
        Object service = Class.forName("android.nfc.INfcAdapter$Stub")
                .getMethod("asInterface", Class.forName("android.os.IBinder"))
                .invoke(null, binder);
        if ("pause".equals(action)) {
            long ms = args.length > 1 ? Long.parseLong(args[1]) : 10000L;
            Object status;
            try {
                status = iface.getMethod("pausePolling", long.class).invoke(service, ms);
            } catch (NoSuchMethodException e) {
                status = iface.getMethod("pausePolling", int.class).invoke(service, (int) ms);
            }
            System.out.println("ROOTPAUSE|" + status);
        } else {
            Object status = iface.getMethod("resumePolling").invoke(service);
            System.out.println("ROOTRESUME|" + status);
        }
    }
}
