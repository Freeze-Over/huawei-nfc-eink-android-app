# QPollLab v2.12.4-reset

基于 `2.12.0-reader-test1` 的复位版：`versionCode 30`、`versionName 2.12.4-reset`。

## 新增

- **刷图前自动断场复位**：`pausePolling`（自动探测合法超时）→ 关场 2 s → 下发
  `0x13 / 01 2A 02 0A 7A` → `setReaderMode(0x81)` → `resumePolling` 开场。
  场恢复后的第一轮寻卡直接归 App，不再需要把手机从壳子上拿开再贴回。
- 日志新增 `QP|RESET|…`，记录 pause 是否被接受、关场时长、7A 配置状态和开场结果。

## 修复

- **`pausePolling` 签名兼容**：`INfcAdapter.pausePolling` 在不同系统上不同——
  Android 16 / ST54L 是 `long`，Android 15 / NXP 是 `int`。现在两种都会尝试；
  之前只按一种反射会在另一类系统上抛 `NoSuchMethodException`，断场被静默跳过。
- `qtag(1)` 返回非 0 不再中止刷图：ST54L 上 `0x70` 返回 `05` 属于正常，只记
  `QTAG_REJECTED` 并继续按 `7A` 轮询。
- `cleanup()` 补 `resumePolling()`，异常/中止退出不会把 NFC 留在暂停态。

## v2.12.4 改进

- **Shell 无权限时自动用 root 关场**：部分 ROM（红米 Note 12 Turbo / NXP SN100U）的
  `pausePolling` 要求 `WRITE_SECURE_SETTINGS`，Shell（UID 2000）会被拒绝。新增
  `RootPause` 助手：worker 通过 `su -c app_process ... RootPause pause|resume` 以 UID 0
  完成关场/开场，且同时兼容 `int` / `long` 签名；`su` 不可用时只记一条 `跳过断场`。
- `SecurityException` 不再对 5 个候选各刷一条日志。
- `0x13/7A` 只对 ST54L 设备（存在 `/odm/etc/libnfc-hal-st.conf` 或
  `/vendor/etc/libnfc-hal-st.conf`）下发；NXP 机型自动跳过，不影响 QTag 寻卡。

## 验证

- 小米 15 Pro（haotian / ST54L）：Shell 直接 `pausePolling(long)` → 关场 2 s →
  `setReaderMode flags=129` → `resumePolling: done` → Tag 自动出现 →
  D1 → D8 → D3 → D4 → DE 完成，全程没有拿开手机。
- 红米 Note 12 Turbo（NXP SN100U）：Shell 调 `pausePolling(int)` 被权限拒绝 →
  自动 root 关场 → 开场后寻卡；`0x13` 跳过；刷新流程不变。

## 安装

```sh
adb install -r QPollLab-2.12.4-reset.apk
```

- 从旧版覆盖升级（同签名）；
- 签名不同请先 `adb uninstall org.nfcresearch.qpolllab`；
- 需要 Shizuku（Shell / Root）授权、NFC 开启；root 回退需要 `su` 可用。
