# QPollLab v2.12.1-reset

基于 `2.12.0-reader-test1`（versionCode 26）的复位版：`versionCode 27`、`versionName 2.12.1-reset`。

## 新增

- **刷图前自动断场复位**：`pausePolling`（自动探测合法超时）→ 关场 2 s → 下发
  `0x13 / 01 2A 02 0A 7A` → `setReaderMode(0x81)` → `resumePolling` 开场。
  场恢复后的第一轮寻卡直接归 App，不再需要把手机从壳子上拿开再贴回。
- 日志新增 `QP|RESET|…`，记录 pause 是否被接受、关场时长、7A 配置状态和开场结果。

## 修复

- **`INfcAdapter.pausePolling` 的参数是 `long` 而不是 `int`**。之前按 `int` 反射会抛
  `NoSuchMethodException`，五个候选超时全部被拒后静默跳过断场，场一直开着，
  所以仍然只能靠手动拿开壳子复位。
- `qtag(1)` 返回非 0 不再中止刷图：ST54L 上 `0x70` 返回 `05` 属于正常，只记
  `QTAG_REJECTED` 并继续按 `7A` 轮询。
- `cleanup()` 补 `resumePolling()`，异常/中止退出不会把 NFC 留在暂停态。

## 验证

小米 15 Pro（haotian / ST54L）+ 华为 552×768 四色墨水屏壳子：

```
pausePolling 被接受 → 关场 2 s → setReaderMode flags=129 → resumePolling: done
→ Tag 自动出现 → D1 → D8 → D3 传图 → D4 刷新 → DE 空闲确认
```

全程没有拿开手机；刷新阶段若出现 `DE ... SW=68CA`，属于供电/压力问题，与寻卡无关，
压紧壳子或把「上电等待」调到 3000–5000 ms 后重试。

## 安装

```sh
adb install -r QPollLab-2.12.1-reset.apk
```

- 从 `2.12.0-reader-test1` 覆盖升级（同签名）；
- 如果签名不同，先 `adb uninstall org.nfcresearch.qpolllab` 再安装；
- 需要 Shizuku（Shell / Root）授权与 NFC 开启。

## 构建

```powershell
$env:ANDROID_HOME = "<sdk>"
$env:JAVA_HOME   = "<jdk17>"
python build.py
# 或使用仓库内 build_qpoll.ps1（Windows / PowerShell）
```
