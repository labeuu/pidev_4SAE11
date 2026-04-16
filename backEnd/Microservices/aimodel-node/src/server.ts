import "./types.js";
import { createServer } from "node:http";
import { createApp } from "./app.js";
import { config } from "./config.js";
import { deregisterFromEureka, registerWithEureka } from "./eureka.js";

const app = createApp();
const server = createServer(app);

let eurekaHeartbeat: NodeJS.Timeout | null = null;

async function shutdown(signal: string): Promise<void> {
  console.log(`Received ${signal}, shutting down AIMODEL service...`);

  if (eurekaHeartbeat) {
    clearInterval(eurekaHeartbeat);
    eurekaHeartbeat = null;
  }

  await deregisterFromEureka();

  await new Promise<void>((resolve, reject) => {
    server.close((error) => {
      if (error) {
        reject(error);
        return;
      }
      resolve();
    });
  });
}

server.listen(config.port, async () => {
  console.log(`AIMODEL Node service listening on port ${config.port}`);
  eurekaHeartbeat = await registerWithEureka();
});

for (const signal of ["SIGINT", "SIGTERM"] as const) {
  process.on(signal, () => {
    shutdown(signal)
      .then(() => process.exit(0))
      .catch((error: unknown) => {
        const message = error instanceof Error ? error.message : "Unknown shutdown error";
        console.error(message);
        process.exit(1);
      });
  });
}
