import 'package:hive/hive.dart';

import '../models/local_data_model.dart';

final conversationRepository = ConversationRepository();

class ConversationRepository {
  static final ConversationRepository _instance = ConversationRepository._internal();

  factory ConversationRepository() => _instance;

  ConversationRepository._internal();

  static const String conversationBoxKey = "conversation_box_key";
  Box<AgentConversationBean>? _box;

  Future<Box<AgentConversationBean>> get _conversationBox async => _box ??= await Hive.openBox<AgentConversationBean>(conversationBoxKey);

  Future<List<AgentConversationBean>> getConversationListFromBox() async {
    List<AgentConversationBean> list = [];
    list.addAll((await _conversationBox).values);
    list.sort((a, b) => (b.updateTime ?? 0) - (a.updateTime ?? 0));
    return list;
  }

  Future<void> removeConversation(String key) async {
    await (await _conversationBox).delete(key);
  }

  Future<void> updateConversation(String key, AgentConversationBean agent) async {
    await (await _conversationBox).put(key, agent);
  }

  Future<AgentConversationBean?> getConversationFromBox(String key) async {
    return (await _conversationBox).get(key);
  }

  Future<void> clear() async {
    await (await _conversationBox).clear();
  }

  static const String debugConversationBoxKey = "debug_conversation_box_key";
  Box<AgentConversationBean>? _debugBox;

  Future<Box<AgentConversationBean>> get _debugConversationBox async =>
      _debugBox ??= await Hive.openBox<AgentConversationBean>(debugConversationBoxKey);

  Future<void> updateAdjustmentHistory(String key, AgentConversationBean agent) async {
    await (await _debugConversationBox).put(key, agent);
  }

  Future<AgentConversationBean?> getAdjustmentHistory(String key) async {
    return (await _debugConversationBox).get(key);
  }

  Future<void> removeAdjustmentConversation(String key) async {
    await (await _debugConversationBox).delete(key);
  }
}
