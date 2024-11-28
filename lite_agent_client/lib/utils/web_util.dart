import 'package:lite_agent_client/repositories/account_repository.dart';
import 'package:url_launcher/url_launcher.dart';

class WebUtil {
  static void openUrl(String url) async {
    final Uri uri = Uri.parse(url);
    if (!await launchUrl(uri)) {
      throw Exception('Could not launch $uri');
    }
  }

  static void openUserSettingUrl() async {
    String serverUrl = await accountRepository.getApiServerUrl();
    String workSpaceId = await accountRepository.getWorkSpaceId();
    if (serverUrl.isNotEmpty && workSpaceId.isNotEmpty) {
      String url = "$serverUrl/dashboard/$workSpaceId";
      openUrl(url);
    }
  }

  static void openAgentAdjustUrl(String agentId) async {
    String serverUrl = await accountRepository.getApiServerUrl();
    if (serverUrl.isNotEmpty && agentId.isNotEmpty) {
      String url = "$serverUrl/agent/$agentId";
      openUrl(url);
    }
  }
}
