import 'package:opentool_dart/opentool_dart.dart';
import 'package:retrofit/retrofit.dart';
import 'package:dio/dio.dart';
import '../model/model.dart';

part 'http_service.g.dart';

@RestApi()
abstract class HttpService {
  factory HttpService(Dio dio) {
    return _HttpService(dio);
  }

  @GET("/version")
  Future<Version> getVersion({@Header("Accept") String accept = "application/json"});

  @POST("/initSession")
  Future<Session> initSession({@Query("agentId") required String agentId, @Header("Accept") String accept = "application/json"});

  @POST("/callback")
  Future<void> callback({@Query("sessionId") required String sessionId, @Body() required ToolReturn toolReturn});

  @GET("/history")
  Future<List<AgentMessage>> getHistory({@Query("sessionId") required String sessionId, @Header("Accept") String accept = "application/json"});

  @GET("/stop")
  Future<void> stop({@Query("sessionId") required String sessionId, @Query("taskId") String? taskId});

  @GET("/clear")
  Future<void> clear({@Query("sessionId") required String sessionId});

  @GET("/opentool-list")
  Future<List<OpenToolInfo>> listOpenTools();

  @GET("/agent/list")
  Future<List<AgentInfo>> listAgent({@Header("Accept") String accept = "application/json"});

  @GET("/agent")
  Future<AgentInfo> getAgent({@Query("agentId") required String agentId, @Header("Accept") String accept = "application/json"});

  @POST("/agent/create")
  Future<AgentId> createAgent({@Body() required Agent agent, @Header("Accept") String accept = "application/json"});

  @POST("/agent/update")
  Future<AgentId> updateAgent({@Query("agentId") required String agentId, @Body() required Agent agent, @Header("Accept") String accept = "application/json"});

  @GET("/agent/delete")
  Future<AgentId> deleteAgent({@Query("agentId") required String agentId, @Header("Accept") String accept = "application/json"});
}
