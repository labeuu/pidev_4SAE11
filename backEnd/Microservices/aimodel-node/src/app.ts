import express from "express";
import { aiRouter } from "./controllers/aiController.js";
import { errorHandler, notFoundHandler } from "./middleware/errorHandler.js";
import { gatewayOnlyMiddleware } from "./middleware/gatewayOnly.js";
import { jwtAuthMiddleware } from "./middleware/jwtAuth.js";

export function createApp() {
  const app = express();

  app.use(express.json());
  app.use(gatewayOnlyMiddleware);
  app.use(jwtAuthMiddleware);

  app.get("/actuator/health", (_request, response) => {
    response.json({ status: "UP" });
  });

  app.get("/actuator/info", (_request, response) => {
    response.json({ app: { name: "AIMODEL" } });
  });

  app.use("/api/ai", aiRouter);
  app.use(notFoundHandler);
  app.use(errorHandler);

  return app;
}
