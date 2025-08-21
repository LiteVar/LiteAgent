import 'dart:math';

import 'package:lite_agent_client/utils/shared_preferences_uitl.dart';

import 'snowflake.dart';

SnowFlakeUtil snowFlakeUtil = SnowFlakeUtil();

class SnowFlakeUtil {
  int workId = 15;
  int datacenterId = 16;

  void init() async {
    var workId = await SPUtil.getInt("workId");
    var datacenterId = await SPUtil.getInt("datacenterId");
    if (workId != null && datacenterId != null && workId <= 31 && workId >= 0 && datacenterId <= 31 && datacenterId >= 0) {
      this.workId = workId;
      this.datacenterId = datacenterId;
    } else {
      Random random = Random();
      this.workId = random.nextInt(32); //random number 0-31
      this.datacenterId = random.nextInt(32);//random number 0-31
      SPUtil.setInt("workId", this.workId);
      SPUtil.setInt("datacenterId", this.datacenterId);
    }
  }

  String getId() {
    return Snowflake(workId, datacenterId).getId().toString();
  }
}
