PVTodo —— 带软件锁的待办 + 每日光伏预测论文推送
================================================

【这是什么】
一个 Android App 完整源码工程。两个功能:
1. 待办清单(本地存储,带软件锁,别人看不到内容)
2. 每日推送光伏功率预测的科研论文(近期,优先标注 SCI Q1 期刊)

【功能说明】
- 软件锁:首次打开设置 4 位 PIN,之后每次打开都要输入才能进。
- 待办:底部「待办」标签,右下角 + 添加(标题/备注/优先级),勾选完成。
- 论文:底部「论文」标签,自动加载,可手动「刷新」;
  每天早上 9:00 自动推送通知(需允许系统通知权限)。
- AI 论文讲解:底部「设置」标签里填入你自己的 LLM 接口(Base URL / API Key / 模型名,
  OpenAI 兼容格式,DeepSeek/通义/Kimi/GLM/各类中转站都行),配置只存本机。
  之后在「论文」标签点任意论文的「AI 讲解」按钮,即生成科研导向导读:
  一句话总结 / 创新点 / 方法模型 / 数据实验 / 局限 / 可借鉴 idea。

================================================
怎么得到 apk —— 方案 B:GitHub 云端编译(推荐,本地零安装)
================================================

你本地【什么都不用装】,编译在 GitHub 的服务器上完成,编完下载 apk 即可。
前提:有一个 GitHub 账号(免费注册 https://github.com)。

步骤:

1. 解压这个 zip,你会看到 PVTodo 文件夹(里面已有 .github/workflows/build.yml 云端编译配置)。

2. 把代码传到 GitHub(二选一):
   【网页上传(最简单,不会 git 也行)】
   - 登录 GitHub → 右上角 + → New repository → 建一个仓库(名字如 PVTodo,选 Public)
   - 进入仓库页面 → 点 "uploading an existing file" / Add file → Upload files
   - 把 PVTodo 文件夹里的【所有文件和子文件夹】拖进去上传
     (注意:要上传的是 PVTodo 文件夹【里面】的内容,含 app/、.github/、build.gradle.kts 等)

   【用 git(会命令行的话)】
   cd PVTodo
   git init
   git add .
   git commit -m "init"
   git branch -M main
   git remote add origin https://github.com/你的用户名/PVTodo.git
   git push -u origin main

3. 触发云端编译:
   - 代码推上去后,GitHub 会自动开始编译(因为它检测到 .github/workflows/build.yml)。
   - 到仓库页面 → 顶部 "Actions" 标签 → 能看到一次 "Build APK" 正在运行(黄色圆圈=进行中)。
   - 等几分钟,变绿勾 ✓ 表示成功。

4. 下载 apk:
   - 点进那次成功的 "Build APK" 运行 → 拉到页面底部 "Artifacts" 区 →
     下载 "PVTodo-debug-apk"。
   - 解压得到 app-debug.apk,传到手机安装(允许未知来源)即可。

【如果 Actions 没自动跑】
到仓库 → Actions 标签 → 左侧选 "Build APK" → 右侧 "Run workflow" 按钮手动触发。

================================================
备选:方案 A 本地编译(如果你有装了 Android Studio 的电脑)
================================================
1. 装 Android Studio:https://developer.android.com/studio(免费,自带 Java+SDK)
2. Open 打开 PVTodo 文件夹,等 Gradle 同步完(第一次联网下依赖)
3. Build → Build Bundle(s)/APK(s) → Build APK(s)
4. apk 在 app/build/outputs/apk/debug/app-debug.apk

================================================
"SCI 一区"说明(务必看)
================================================
- 数据来自 Crossref API(正式期刊论文)+ arXiv API(最新预印本),关键词 photovoltaic power forecasting。
- 权威分区(JCR/中科院分区)是付费数据,免费接口拿不到。本 App 用【内置 Q1 期刊白名单】
  近似判定,命中标 Q1 徽章(Applied Energy / IEEE TSG / Renewable Energy 等已内置)。
- 白名单在 app/src/main/java/com/yg/pvtodo/papers/PaperFetcher.kt 的 Q1_JOURNALS,可自行增删。
- 需要真正权威分区需接入付费/机构数据服务。

【可自行修改】
- 每日推送时间:app/src/main/java/com/yg/pvtodo/App.kt 的 schedule(this, 9, 0, ...),改 9 为你要的小时。
- 论文关键词:PaperFetcher.kt 里的查询字符串。

【技术栈】Kotlin + Jetpack Compose + Room + WorkManager,minSdk 26(Android 8.0+)。
