class Constants {
  static const apiDesktopServerPath = '/liteAgent';
  static const apiServerPath = '';
  static const localFilePrefix = 'localFile::';

  static const configJsonString = "{\r\n  \"version\": \"0.2.0\",\r\n  \"server\": {\r\n    \"ip\": \"0.0.0.0\",\r\n    \"apiPathPrefix\": \"\/api\",\r\n    \"port\": 9527\r\n  },\r\n  \"log\": {\r\n    \"level\": \"INFO\"\r\n  }\r\n}";
}

class HiveTypeIds {
  static const int agentModelTypeId = 0;
  static const int toolModelTypeId = 1;
  static const int modelTypeId = 2;
  static const int messageModelTypeId = 3;
  static const int conversationModelTypeId = 4;
  static const int toolFunctionModelTypeId = 5;
  static const int thoughtModelTypeId = 6;
}

class OperationMode {
  static const int PARALLEL = 0;
  static const int SERIAL = 1;
  static const int REJECT = 2;
}

class ImportOperate {
  static const int operateNew = 0; // 新建
  static const int operateOverwrite = 1; // 覆盖
  static const int operateSkip = 2; // 跳过
}

