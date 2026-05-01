# Echoic

跨平台 AI 语音合成（TTS）桌面应用，基于 Kotlin Multiplatform + Compose Desktop 构建。

## 功能特性

- **云端 TTS** — 集成多家云端语音合成 API，一键生成高质量语音
- **本地模型** — 支持下载和运行本地 TTS 模型，离线可用
- **实时进度** — 模型下载支持实时进度、速度和大小显示
- **多格式输出** — 支持 MP3、WAV、OPUS、FLAC、AAC 格式
- **中英双语** — UI 支持中文和英文切换
- **深色模式** — 支持亮色/暗色主题

## 支持的云端提供商

| 提供商 | 说明 |
|--------|------|
| OpenAI | TTS-1 / TTS-1 HD |
| Google Cloud | WaveNet / Neural2 |
| Azure | Microsoft Neural Voices |
| ElevenLabs | 超逼真 AI 语音 |
| Baidu | 百度语音合成 |
| Tencent | 腾讯云语音合成 |
| Aliyun | 阿里云 NLS 语音合成 |
| Fish Audio | 高质量语音合成 |
| MiniMax | Speech-01 语音合成 |
| Zhipu AI (GLM) | 智谱 GLM TTS（OpenAI 兼容） |
| Volcano Engine | 火山引擎（字节跳动）TTS |

## 支持的本地模型

| 模型 | 大小 | 说明 |
|------|------|------|
| Piper | 60 MB | 轻量级 ONNX 神经网络 TTS |
| Sherpa-ONNX | 115 MB | 跨平台语音工具包 |
| eSpeak NG | 10 MB | 多语言共振峰合成 |
| VoxCPM | 4700 MB | OpenBMB 零样本声音克隆 |
| CosyVoice | 3200 MB | 阿里巴巴多语言 TTS |
| ChatTTS | 1200 MB | 对话式语音生成 |
| GPT-SoVITS | 2000 MB | 少样本声音克隆 |

## 技术栈

- **Kotlin 2.1** + Kotlin Multiplatform
- **Compose Desktop** (Material 3)
- **Ktor** — HTTP 客户端
- **kotlinx.serialization** — JSON 序列化
- **JVM** (Desktop) — macOS / Windows / Linux

## 项目结构

```
echoic-kmp/
├── composeApp/          # Compose Desktop 应用
│   └── src/
│       ├── commonMain/  # 共享 UI 代码
│       └── desktopMain/ # JVM 桌面平台实现
├── shared/              # 共享业务逻辑模块
│   └── src/
│       ├── commonMain/  # TTS 服务、模型定义、下载管理
│       └── desktopMain/ # JVM 平台实现（音频播放、Base64 等）
├── docs/                # 文档
├── build.gradle.kts
└── settings.gradle.kts
```

## 构建与运行

### 前置要求

- JDK 17+
- Gradle 8.x（项目自带 gradlew）

### 运行

```bash
./gradlew :composeApp:run
```

### 打包

```bash
# macOS DMG
./gradlew :composeApp:packageDmg

# Windows MSI
./gradlew :composeApp:packageMsi

# Linux DEB
./gradlew :composeApp:packageDeb
```

## 使用方式

1. 启动应用后，进入 **Providers** 页面配置 API Key
2. 在 **New Generation** 页面选择云端提供商或本地模型
3. 输入文本，点击生成即可获得语音

## 许可证

MIT License
