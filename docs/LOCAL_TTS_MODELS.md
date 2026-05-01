# 本地 TTS 模型文档

## 概述

本地 TTS（Text-to-Speech）模型是指完全在设备端运行的语音合成引擎，无需联网或调用远程 API。

### 优势

- **离线可用**：无需网络连接，适合无网环境或网络不稳定场景
- **隐私保护**：文本数据不离开设备，适合处理敏感信息
- **无 API 费用**：一次部署，永久使用，无按量计费
- **低延迟**：本地推理，响应速度快
- **可控性强**：可自定义模型、调整参数

### 适用场景

- 离线阅读辅助
- 隐私敏感的语音播报
- 嵌入式/边缘设备语音交互
- 开发调试阶段的快速原型
- 无网络环境下的语音功能

---

## 推荐模型

### 最佳选择

#### 1. Sherpa-ONNX

| 属性 | 说明 |
|------|------|
| 优势 | 跨平台、支持多种模型架构、有官方 Kotlin/Java 绑定 |
| 支持平台 | macOS (ARM64/x64), Windows (x64/ARM64), Linux (x64/ARM64) |
| 集成方式 | JNI Java 绑定（官方提供） |
| 模型大小 | 15-300 MB |
| 中文支持 | 是（VITS AISHELL3、Piper Huayan 等模型） |
| GitHub | https://github.com/k2-fsa/sherpa-onnx |

**特点**：
- 官方维护的 Java/Kotlin JNI 绑定，集成最友好
- 支持多种 TTS 模型（VITS、Piper 等）
- 同时支持 ASR（语音识别）和说话人识别
- 推理速度快，资源占用合理

**可用模型**：
- `SHERPA_VITS_ZH` - 中文 VITS (AISHELL3)，约 80 MB
- `SHERPA_VITS_EN` - 英文 VITS (VCTK)，约 100 MB
- `SHERPA_PIPER_EN` - Piper 英文 (Amy)，约 60 MB
- `SHERPA_PIPER_ZH` - Piper 中文 (Huayan)，约 80 MB

---

#### 2. Piper TTS

| 属性 | 说明 |
|------|------|
| 优势 | 轻量级、ONNX 格式、模型小、推理快 |
| 支持平台 | 全平台 |
| 集成方式 | ONNX Runtime Java 绑定或 JNI |
| 模型大小 | 15-200 MB |
| 中文支持 | 是（Huayan 模型） |
| GitHub | https://github.com/rhasspy/piper |

**特点**：
- 基于 VITS 架构，模型体积小
- ONNX 格式，跨平台兼容性好
- 社区维护 30+ 语言的语音模型
- 推理速度极快

**可用模型**：
- `PIPER_EN_US_MEDIUM` - 英文 Amy，约 60 MB
- `PIPER_ZH_CN_HUAYAN` - 中文 Huayan，约 80 MB
- `PIPER_DE_THORSTEN` - 德文 Thorsten，约 60 MB
- `PIPER_FR_SIWISE` - 法文 Siwis，约 60 MB

---

#### 3. ChatTTS

| 属性 | 说明 |
|------|------|
| 优势 | 中文效果最好、对话风格自然、支持细粒度控制 |
| 支持平台 | 全平台（需要 Python 运行时） |
| 集成方式 | Python 子进程或 REST API |
| 模型大小 | 200-1000 MB |
| 中文支持 | 优秀（专为中文对话优化） |
| GitHub | https://github.com/2noise/ChatTTS |

**特点**：
- 中文语音自然度最高，适合对话场景
- 支持笑声、停顿等细粒度韵律控制
- 需要 Python 环境，部署稍复杂
- 模型较大，首次加载需要时间

**可用模型**：
- `CHAT_TTS_DEFAULT` - 默认模型，约 800 MB

---

### 备选方案

#### eSpeak NG
- **定位**：最轻量的备选方案
- **优势**：支持 80+ 语言，体积仅 1-10 MB
- **劣势**：基于共振峰合成，音质较机械
- **适用**：作为兜底方案或对音质要求不高的场景
- GitHub: https://github.com/espeak-ng/espeak-ng

#### MaryTTS
- **定位**：Java 原生 TTS 服务器
- **优势**：纯 Java 实现，可直接嵌入 JVM 项目，支持 SSML
- **劣势**：中文支持差，维护不太活跃
- **适用**：Java 项目集成、需要 SSML 支持的场景
- GitHub: https://github.com/marytts/marytts

#### Coqui TTS
- **定位**：高质量开源 TTS 工具包
- **优势**：支持 XTTS v2 语音克隆，多语言质量高
- **劣势**：需要 Python 环境，资源占用较大
- **适用**：需要语音克隆或多语言高质量合成的场景
- GitHub: https://github.com/coqui-ai/TTS

#### 其他模型

| 模型 | 说明 | GitHub |
|------|------|--------|
| VITS | 基础端到端模型，Piper/Coqui 的基础 | https://github.com/jaywalnut310/vits |
| Festival | 爱丁堡大学经典项目，主要支持英文 | https://github.com/festvox/festival |
| StyleTTS 2 | LJSpeech 基准上达到人类水平，需要 PyTorch | https://github.com/yl4569/StyleTTS2 |

---

## 集成指南

### Sherpa-ONNX 集成步骤

#### 1. 添加依赖

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.k2fsa.sherpa:sherpa-onnx:1.10.30")
}
```

#### 2. 下载模型

从 [releases](https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models) 下载所需模型，或使用代码自动下载。

```kotlin
// 模型目录结构示例
// models/
//   vits-zh-aishell3/
//     model.onnx
//     tokens.txt
//     lexicon.txt
```

#### 3. 初始化引擎

```kotlin
import com.k2fsa.sherpa.onnx.*

val config = OnlineTransducerModelConfig(
    encoder = "models/vits-zh-aishell3/encoder.onnx",
    decoder = "models/vits-zh-aishell3/decoder.onnx",
)

val ttsConfig = TtsConfig(
    model = config,
    tokens = "models/vits-zh-aishell3/tokens.txt",
    numThreads = 4,
)

val tts = Tts(ttsConfig)
```

#### 4. 调用合成 API

```kotlin
val audio = tts.generate(
    text = "你好，世界",
    speakerId = 0,
    speed = 1.0f
)

// audio.samples: FloatArray
// audio.sampleRate: Int
```

---

### Piper TTS 集成步骤

#### 1. 添加 ONNX Runtime 依赖

```kotlin
dependencies {
    implementation("com.microsoft.onnxruntime:onnxruntime:1.16.3")
}
```

#### 2. 下载模型

从 [HuggingFace](https://huggingface.co/rhasspy/piper-voices) 下载 ONNX 模型。

```kotlin
// 模型文件
// zh_CN-huayan-medium.onnx
// zh_CN-huayan-medium.onnx.json
```

#### 3. 加载模型

```kotlin
import ai.onnxruntime.*

val env = OrtEnvironment.getEnvironment()
val session = env.createSession("models/zh_CN-huayan-medium.onnx")

// 读取模型配置
val config = Json.parseToJsonElement(
    File("models/zh_CN-huayan-medium.onnx.json").readText()
).jsonObject
```

#### 4. 调用推理

```kotlin
// 文本预处理（音素转换）
val phonemes = textToPhonemes("你好")

// 构建输入张量
val inputTensor = OnnxTensor.createTensor(env, phonemeIds)

// 推理
val results = session.run(mapOf("input" to inputTensor))
val audio = (results[0].value as FloatArray)

// 后处理：声码器解码
val waveform = hifiGan.decode(audio)
```

---

### ChatTTS 集成步骤

#### 1. 安装 Python 依赖

```bash
pip install ChatTTS
pip install torch torchaudio
```

#### 2. 启动服务

```python
# server.py
import ChatTTS
from fastapi import FastAPI
import uvicorn

app = FastAPI()
chat = ChatTTS.Chat()
chat.load_models()

@app.post("/tts")
async def tts(text: str, speaker: int = 0):
    wavs = chat.infer([text], speaker=speaker)
    return {"audio": wavs[0].tolist()}
```

#### 3. 调用 API

```kotlin
// Kotlin 端调用
val response = httpClient.post("http://localhost:8000/tts") {
    parameter("text", "你好，今天天气怎么样？")
    parameter("speaker", 0)
}
val audioData = response.body<AudioResponse>()
```

---

## 平台兼容性矩阵

| 模型 | macOS ARM64 | macOS x64 | Windows x64 | Windows ARM64 | Linux x64 | Linux ARM64 |
|------|-------------|-----------|-------------|---------------|-----------|-------------|
| Sherpa-ONNX | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Piper | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| ChatTTS | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| VITS | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| eSpeak | ✅ | ✅ | ✅ | ⚠️ | ✅ | ✅ |
| MaryTTS | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Coqui | ✅ | ✅ | ✅ | ⚠️ | ✅ | ✅ |
| Festival | ✅ | ✅ | ✅ | ⚠️ | ✅ | ✅ |
| StyleTTS 2 | ✅ | ✅ | ✅ | ⚠️ | ✅ | ✅ |

> ⚠️ = 部分支持或需要额外配置

---

## 模型下载链接

| 模型 | 下载地址 |
|------|----------|
| Sherpa-ONNX | https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models |
| Piper | https://huggingface.co/rhasspy/piper-voices |
| ChatTTS | https://huggingface.co/2noise/ChatTTS |
| VITS | https://huggingface.co/jaywalnut310/vits |
| StyleTTS 2 | https://huggingface.co/yl4569/StyleTTS2-LJSpeech |

---

## 模型对比总览

| 模型 | 模型大小 | 中文质量 | 英文质量 | 推理速度 | 集成难度 | 推荐指数 |
|------|----------|----------|----------|----------|----------|----------|
| Sherpa-ONNX | 15-300 MB | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Piper | 15-200 MB | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| ChatTTS | 200-1000 MB | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| Coqui | 50-500 MB | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ |
| eSpeak | 1-10 MB | ⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| MaryTTS | 50-200 MB | ⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ |

---

## 代码结构参考

项目中本地 TTS 相关的核心代码位于：

```
shared/src/commonMain/kotlin/com/echoic/shared/model/
├── LocalTTSProvider.kt   # TTS 提供者枚举（Piper, Sherpa, ChatTTS 等）
├── LocalTTSModel.kt      # 具体模型枚举（包含模型 URL、大小、语言等）
└── TTSTag.kt             # 标签枚举（OFFLINE, CHINESE, FAST 等）
```

### LocalTTSProvider 主要属性

- `displayName` - 显示名称
- `subtitle` - 简短描述
- `githubURL` - GitHub 仓库地址
- `tags` - 特性标签列表
- `modelSizeMB` - 模型大小范围
- `supportedLanguages` - 支持的语言代码
- `integrationMethod` - 集成方式说明
- `notes` - 备注信息

### LocalTTSModel 主要属性

- `rawValue` - 模型标识符
- `displayName` - 显示名称
- `provider` - 所属 Provider
- `language` - 语言代码（en, zh, multi 等）
- `modelSizeMB` - 模型大小（MB）
- `tags` - 特性标签
- `modelURL` - 模型下载地址
- `notes` - 备注信息

---

## 注意事项

1. **模型文件较大**：建议按需下载，首次使用时动态获取，避免打包进应用
2. **首次加载延迟**：模型加载需要几秒钟，建议在后台线程预加载
3. **内存占用**：取决于模型大小，大模型（如 ChatTTS）可能占用 1-2 GB 内存
4. **Python 依赖**：ChatTTS、Coqui、StyleTTS 2 需要 Python 运行时，部署时需额外配置
5. **线程安全**：多数 TTS 引擎不是线程安全的，多线程调用时需要加锁
6. **音频格式**：注意采样率和位深的统一，避免播放时出现杂音

---

## 选型建议

| 场景 | 推荐模型 |
|------|----------|
| 优先中文质量 | ChatTTS |
| 优先集成便捷性 | Sherpa-ONNX |
| 优先轻量级 | Piper 或 eSpeak |
| 优先跨平台一致性 | Sherpa-ONNX |
| 优先语音克隆 | Coqui XTTS v2 |
| Java/Kotlin 原生集成 | MaryTTS 或 Sherpa-ONNX |
