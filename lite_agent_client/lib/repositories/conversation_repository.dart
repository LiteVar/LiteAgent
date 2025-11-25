import 'package:lite_agent_client/models/local/conversation.dart';
import 'base_hive_repository.dart';

final conversationRepository = ConversationRepository();
final debugConversationRepository = DebugConversationRepository();

class ConversationRepository extends BaseHiveRepository<ConversationModel> {
  static final ConversationRepository _instance = ConversationRepository._internal();

  factory ConversationRepository() => _instance;

  ConversationRepository._internal() : super("conversation_box_key");

  Future<List<ConversationModel>> getConversationListFromBox() async {
    final list = (await getAll()).toList();
    list.sort((a, b) => (b.updateTime ?? 0) - (a.updateTime ?? 0));
    return list;
  }

  Future<void> removeConversation(String key) async => delete(key);

  Future<void> updateConversation(String key, ConversationModel agent) async => save(key, agent);

  Future<ConversationModel?> getConversationFromBox(String key) async => getData(key);
}

class DebugConversationRepository extends BaseHiveRepository<ConversationModel> {
  static final DebugConversationRepository _instance = DebugConversationRepository._internal();

  factory DebugConversationRepository() => _instance;

  DebugConversationRepository._internal() : super("debug_conversation_box_key");

  Future<void> updateDebugHistory(String key, ConversationModel agent) async => save(key, agent);

  Future<ConversationModel?> getDebugHistory(String key) async => getData(key);

  Future<void> removeDebugConversation(String key) async => delete(key);
}
