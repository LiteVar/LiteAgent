# lite_agent_client

English · [中文](README-zh_CN.md)

Local end: The local agent sets up LLM by itself, which can be used without logging in and does not
involve creating a workspace.
Cloud: Users need to log in to their own account and server address to display cloud data. In
desktop, cloud based functions only provide conversation and chat related functions. Other functions
require modification of cloud based agents, tools, models, debugging, and personal information. When
triggered, the system browser will automatically open and enter the user interface.

## How to build

- macOS

```bash 
flutter build macos --release
```

- Windows

```bash 
flutter build windows --release
```

- Linux

```bash 
flutter build linux --release
```

## How to use

- Local end

1. Click the "New Model" button in the large model tab, enter the name of the large model interface,
   BaseURL, Apikey, and complete the creation;
2. Click the "New Local Agent" button in the Agent Tab, fill in the Agent information, and complete
   the creation;
3. Enter the Agent debugging page, select the large model, add tools (optional), click save to start
   debugging;
4. After debugging, return to the chat tab and click "Start Chat" to select the corresponding agent
   for chatting.

- Cloud

1. Click on "Login/Settings", enter the registered account, password, and server address;
2. Click "Start Chat" and select the cloud agent to chat.