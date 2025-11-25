<div align="center">
  <img src="assets/images/logo_banner.png" alt="Lite Agent Client Logo" width="400"/>
  
  # Lite Agent Client
  
  ![Version](https://img.shields.io/badge/version-2.0.0-blue.svg)
  [![Flutter](https://img.shields.io/badge/Flutter-3.4.3+-02569B?logo=flutter)](https://flutter.dev)
  [![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
  
  **A Powerful Cross-Platform AI Agent Desktop Client**
  
  Support local deployment and cloud synchronization for building, debugging, and using AI Agents with ease
  
  English · [中文](README-zh_CN.md)
  
</div>

---

## ✨ Features

### 🖥️ Local Mode
- **🔐 Privacy First** - No login required, all data stored locally
- **🤖 Agent Management** - Create, edit, and delete local AI Agents
- **🧠 Model Configuration** - Support any OpenAI API-compatible LLM
- **🔧 Tool Integration** - Configure custom tools and functions for Agents
- **🐛 Debug Mode** - Real-time debugging of Agent behavior and responses
- **💬 Chat Interface** - Interact with configured Agents

### ☁️ Cloud Mode
- **🔄 Data Sync** - Synchronize Agents and conversations with cloud server
- **👥 Multi-Device Support** - Seamlessly switch between different devices
- **🔒 Account System** - Support user login and permission management

---

## 🚀 Quick Start

### Requirements

- **Flutter SDK**: >= 3.4.3
- **Dart SDK**: >= 3.4.3
- **Platform Support**: 
  - ✅ macOS 10.14+
  - ✅ Windows 10+

### Install Dependencies

```bash
flutter pub get
```

### Run Application

```bash
# Development mode
flutter run

# macOS
flutter run -d macos

# Windows
flutter run -d windows
```

---

## 📦 Build Release

### macOS

```bash
flutter build macos --release
```

Build output: `build/macos/Build/Products/Release/`

### Windows

```bash
flutter build windows --release
```

Build output: `build/windows/x64/runner/Release/`

> **💡 Tip**: First build may take longer to download dependencies

---

## 📖 User Guide

### Local Mode Workflow

#### 1️⃣ Configure Language Model

1. Click **"Model"** tab in the left navigation
2. Click **"New Model"** button
3. Fill in model information:
   - **Name**: Custom model name
   - **Base URL**: API endpoint
   - **API Key**: Your API key
4. Click **"Save"** to complete

#### 2️⃣ Create Agent

1. Click **"Agent"** tab in the left navigation
2. Click **"New Local Agent"** button
3. Configure Agent information:
   - **Name**: Agent name
   - **Description**: Agent functionality description
   - **System Prompt**: Define Agent's behavior and role
   - **Model Selection**: Select previously created model
4. Click **"Save"**

#### 3️⃣ Configure Tools (Optional)

1. Click **"Tools"** tab in Agent details page
2. Click **"Add Tool"** to select or create tools
3. Configure tool parameters and description
4. Save tool configuration

#### 4️⃣ Debug Agent

1. Enter Agent debugging page
2. Select the model to use
3. Input test messages to view responses
4. Adjust prompts and parameters based on results

#### 5️⃣ Start Chatting

1. Return to **"Chat"** tab
2. Click **"Start Chat"**
3. Select the configured Agent
4. Start interacting with AI!

### Cloud Mode Workflow

#### 1️⃣ Login Account

1. Click **"Login/Settings"** in the top right corner
2. Enter the following information:
   - **Server Address**: Cloud service address
   - **Account**: Registered username
   - **Password**: Account password
3. Click **"Login"**

#### 2️⃣ Use Cloud Agent

1. Click **"Start Chat"**
2. Select a cloud Agent from the list (marked with cloud icon)
3. Start conversation

> **📝 Note**: Detailed configuration of cloud Agents needs to be done in the browser management interface

---

## 🛠️ Tech Stack

### Core Framework
- **Flutter 3.x** - Cross-platform UI framework
- **GetX** - State management and routing
- **Hive** - Lightweight local database

### Network & Data
- **Dio** - HTTP client
- **lite_agent_core_dart_server** - Agent core service
- **JSON Serializable** - Data serialization

### UI Components
- **Material Design 3** - Design language
- **Flutter Markdown** - Markdown rendering
- **EasyLoading** - Loading indicators
- **Styled Toast** - Message notifications

### Functional Modules
- **Window Manager** - Window management
- **File Picker** - File selection
- **Record & Audioplayers** - Audio recording and playback
- **URL Launcher** - Browser integration

---

## 🔧 Development

### Project Structure

```
lib/
├── config/           # Configuration files (routes, constants, translations)
├── models/           # Data models
│   ├── dto/         # Data transfer objects
│   ├── local/       # Local storage models
│   └── mcp/         # MCP protocol models
├── modules/         # Feature modules
│   ├── home/        # Home page
│   ├── chat/        # Chat
│   ├── agent/       # Agent management
│   ├── model/       # Model management
│   ├── tool/        # Tool management
│   └── ...
├── repositories/    # Data repository layer
├── server/          # Service layer
│   ├── api_server/  # Cloud API
│   └── local_server/# Local service
├── utils/           # Utility classes
└── widgets/         # Common components
```

### Code Generation

The project uses `build_runner` to generate serialization code:

```bash
# Generate .g.dart files
dart run build_runner build

# Watch for changes and auto-generate
dart run build_runner watch

# Delete conflicts and regenerate
dart run build_runner build --delete-conflicting-outputs
```

### Data Model Development

Use [JSON to Dart](https://caijinglong.github.io/json2dart/index_ch.html) online tool to quickly generate data models.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE)

---