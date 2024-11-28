class Constants {
  static const apiServerPath = '/liteAgent';

  static const configJsonString = "{\r\n  \"version\": \"0.2.0\",\r\n  \"server\": {\r\n    \"ip\": \"0.0.0.0\",\r\n    \"apiPathPrefix\": \"\/api\",\r\n    \"port\": 9527\r\n  },\r\n  \"log\": {\r\n    \"level\": \"INFO\"\r\n  }\r\n}";
}

class HiveTypeIds {
  static const int agentBeanTypeId = 0;
  static const int toolBeanTypeId = 1;
  static const int modelBeanTypeId = 2;
  static const int messageBeanTypeId = 3;
  static const int agentConversationBeanTypeId = 4;
}
