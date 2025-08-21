import 'dart:async';
import 'dart:io';
import 'package:dart_openai_sdk/dart_openai_sdk.dart';
import 'package:lite_agent_core_dart/lite_agent_core.dart';

class AudioService {
  
  AudioService._();

  static void _configureOpenAI(LLMConfig llmConfig) {
    OpenAI.baseUrl = llmConfig.baseUrl;
    OpenAI.apiKey = llmConfig.apiKey;
  }

  /// 语音转文本
  /// [llmConfig] LLM配置
  /// [audioFile] 音频文件
  /// [model] 使用的ASR模型，默认使用llmConfig中的model
  /// [language] 音频语言（ISO-639-1格式），可选
  /// [prompt] 引导模型的提示文本，可选
  /// [responseFormat] 响应格式，默认为json
  /// [temperature] 采样温度，默认为0
  static Future<String> speechToText({
    required LLMConfig llmConfig,
    required File audioFile,
    String? model,
    String? language,
    String? prompt,
    OpenAIAudioResponseFormat? responseFormat,
    double? temperature,
  }) async {
    try {
      _configureOpenAI(llmConfig);
      
      String audioModel = model ?? llmConfig.model;
      
      OpenAIAudioModel transcription = await OpenAI.instance.audio.createTranscription(
        file: audioFile,
        model: audioModel,
        language: language,
        prompt: prompt,
        responseFormat: responseFormat ?? OpenAIAudioResponseFormat.json,
        temperature: temperature ?? 0.0,
      );

      return transcription.text;
    } catch (e) {
      throw Exception('语音转文本失败: $e');
    }
  }

  /// 文本转语音
  /// [llmConfig] LLM配置
  /// [text] 要转换的文本内容
  /// [model] 使用的TTS模型，默认使用llmConfig中的model
  /// [voice] 语音类型，默认为nova
  /// [speed] 语音速度，范围0.25-4.0，默认为1.0
  /// [outputDirectory] 输出目录
  /// [outputFileName] 输出文件名，默认使用时间戳
  static Future<File> textToSpeech({
    required LLMConfig llmConfig,
    required String text,
    String? model,
    String? voice,
    double? speed,
    Directory? outputDirectory,
    String? outputFileName,
  }) async {
    try {
      _configureOpenAI(llmConfig);
      
      String audioModel = model ?? llmConfig.model;
      
      String fileName = outputFileName ?? DateTime.now().microsecondsSinceEpoch.toString();
      
      File speechFile = await OpenAI.instance.audio.createSpeech(
        model: audioModel,
        input: text,
        voice: voice ?? "nova",
        responseFormat: OpenAIAudioSpeechResponseFormat.mp3,
        speed: speed ?? 1.0,
        outputDirectory: outputDirectory,
        outputFileName: fileName,
      );

      return speechFile;
    } catch (e) {
      throw Exception('文本转语音失败: $e');
    }
  }

  /// 支持的TTS语音类型列表
  static List<String> supportedVoices() {
    return [
      'alloy',    // 中性音色
      'echo',     // 男性音色  
      'fable',    // 英式口音
      'onyx',     // 深沉男性
      'nova',     // 女性音色
      'shimmer',  // 温和女性
    ];
  }

  /// 支持的音频响应格式列表
  static List<OpenAIAudioSpeechResponseFormat> supportedSpeechFormats() {
    return [
      OpenAIAudioSpeechResponseFormat.mp3,
      OpenAIAudioSpeechResponseFormat.opus,
      OpenAIAudioSpeechResponseFormat.aac,
      OpenAIAudioSpeechResponseFormat.flac,
    ];
  }

  /// 支持的转录响应格式列表
  static List<OpenAIAudioResponseFormat> supportedTranscriptionFormats() {
    return [
      OpenAIAudioResponseFormat.json,
      OpenAIAudioResponseFormat.text,
      OpenAIAudioResponseFormat.srt,
      OpenAIAudioResponseFormat.verbose_json,
      OpenAIAudioResponseFormat.vtt,
    ];
  }
}
