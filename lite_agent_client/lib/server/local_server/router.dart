import 'package:shelf_router/shelf_router.dart';

import 'controller.dart';

final Router apiRouter = Router();

void apiRoutes() {
  apiRouter.get('/version', agentController.getVersion);
  apiRouter.post('/init', agentController.initChat);
  apiRouter.get('/chat', agentController.chat);
  apiRouter.get('/history', agentController.history);
  apiRouter.get('/stop', agentController.stopChat);
  apiRouter.get('/clear', agentController.clearChat);
}
