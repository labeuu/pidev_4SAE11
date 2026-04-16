import axios from "axios";
import { config, getPublicHost } from "./config.js";

const HEARTBEAT_INTERVAL_MS = 30000;

function instanceId(): string {
  return `${config.appName}:${getPublicHost()}:${config.port}`;
}

function appUrl(path = ""): string {
  return `http://${getPublicHost()}:${config.port}${path}`;
}

function registrationPayload() {
  return {
    instance: {
      instanceId: instanceId(),
      hostName: getPublicHost(),
      app: config.appName,
      ipAddr: getPublicHost(),
      status: "UP",
      overriddenstatus: "UNKNOWN",
      port: { $: config.port, "@enabled": true },
      securePort: { $: 443, "@enabled": false },
      countryId: 1,
      dataCenterInfo: {
        "@class": "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo",
        name: "MyOwn",
      },
      homePageUrl: appUrl("/actuator/info"),
      statusPageUrl: appUrl("/actuator/info"),
      healthCheckUrl: appUrl("/actuator/health"),
      vipAddress: config.appName,
      secureVipAddress: config.appName,
      isCoordinatingDiscoveryServer: false,
      metadata: {
        "management.port": String(config.port),
      },
    },
  };
}

async function heartbeat(): Promise<void> {
  await axios.put(
    `${config.eurekaBaseUrl}/apps/${config.appName}/${encodeURIComponent(instanceId())}`,
    undefined,
    { timeout: config.readTimeoutMs },
  );
}

export async function registerWithEureka(): Promise<NodeJS.Timeout | null> {
  try {
    await axios.post(`${config.eurekaBaseUrl}/apps/${config.appName}`, registrationPayload(), {
      timeout: config.readTimeoutMs,
      headers: { "Content-Type": "application/json" },
    });

    return setInterval(() => {
      heartbeat().catch((error: unknown) => {
        const message = error instanceof Error ? error.message : "Unknown heartbeat error";
        console.error(`Eureka heartbeat failed: ${message}`);
      });
    }, HEARTBEAT_INTERVAL_MS);
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unknown registration error";
    console.error(`Eureka registration failed: ${message}`);
    return null;
  }
}

export async function deregisterFromEureka(): Promise<void> {
  try {
    await axios.delete(
      `${config.eurekaBaseUrl}/apps/${config.appName}/${encodeURIComponent(instanceId())}`,
      { timeout: config.readTimeoutMs },
    );
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unknown deregistration error";
    console.error(`Eureka deregistration failed: ${message}`);
  }
}
