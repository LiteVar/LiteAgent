import 'package:get/get.dart';

class LocalTranslations extends Translations {
  @override
  Map<String, Map<String, String>> get keys => {
        'zh_CN': {
          'appTitle': 'Agent',
          'sure': '确定',
        },
        'en_US': {
          'appTitle': 'Agent',
          'sure': 'Sure',
        },
      };
}
