# LiteAgent SDK for LabVIEW

English · [中文](README-zh_CN.md)

The LiteAgent SDK for LabVIEW is used for interacting with LiteAgent in LabVIEW applications.

## Features

- Initialize an Agent session
- Send client messages to the Agent
- Subscribe to Agent messages, including: Agent messages, chunk messages during word-by-word typing, SSE Done and Error, and Function Call callback requests
- Send Function Call callback results
- Stop the current session
- Clear the current session

## Installation

- From VIPM Search `LiteAgentSDK`

![VIPM.png](img/VIPM.png)

## Usage

- See `/Example/Example.vi`

![example.png](img/example.png)

- Rewrite the `/Example/Execute.vi`  and return the ToolReturn
