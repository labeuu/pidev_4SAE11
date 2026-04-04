'use strict';

const axios = require('axios');
const config = require('../config');

let heartbeatTimer = null;
let active = false;

function instanceId() {
  return `${config.eurekaAppName}:${config.port}`;
}

function appRegisterUrl() {
  return `${config.eurekaServerUrl}/apps/${encodeURIComponent(config.eurekaAppName)}`;
}

function instanceHeartbeatUrl() {
  return `${appRegisterUrl()}/${encodeURIComponent(instanceId())}`;
}

function buildRegistrationBody() {
  const app = config.eurekaAppName;
  const { port } = config;
  const host = config.eurekaInstanceHostname;
  const ip = config.eurekaInstanceIpAddr;
  const base = `http://${host}:${port}`;

  return {
    instance: {
      instanceId: instanceId(),
      hostName: host,
      app,
      ipAddr: ip,
      status: 'UP',
      port: { $: port, '@enabled': true },
      securePort: { $: 443, '@enabled': false },
      healthCheckUrl: `${base}/actuator/health`,
      statusPageUrl: `${base}/actuator/health`,
      homePageUrl: `${base}/`,
      vipAddress: app,
      secureVipAddress: app,
      dataCenterInfo: {
        '@class': 'com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo',
        name: 'MyOwn',
      },
      leaseInfo: {
        renewalIntervalInSecs: config.eurekaRenewalIntervalSecs,
        durationInSecs: config.eurekaDurationSecs,
      },
    },
  };
}

async function register() {
  await axios.post(appRegisterUrl(), buildRegistrationBody(), {
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    validateStatus: (s) => s === 200 || s === 201 || s === 204,
  });
}

async function sendHeartbeat() {
  await axios.put(instanceHeartbeatUrl(), '', {
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    validateStatus: (s) => s === 200 || s === 204,
  });
}

async function deregister() {
  await axios.delete(instanceHeartbeatUrl(), {
    headers: { Accept: 'application/json' },
    validateStatus: () => true,
  });
}

function heartbeatIntervalMs() {
  const secs = Math.max(10, config.eurekaRenewalIntervalSecs - 5);
  return secs * 1000;
}

async function start() {
  if (!config.eurekaEnabled) {
    console.log('[eureka] registration disabled (EUREKA_ENABLED=false)');
    return;
  }
  await register();
  active = true;
  console.log(`[eureka] registered ${config.eurekaAppName} at ${config.eurekaInstanceHostname}:${config.port}`);

  heartbeatTimer = setInterval(() => {
    sendHeartbeat().catch(async (err) => {
      console.warn('[eureka] heartbeat failed:', err.message);
      if (active && err.response?.status === 404) {
        try {
          await register();
          console.log('[eureka] re-registered after missing instance');
        } catch (e) {
          console.warn('[eureka] re-register failed:', e.message);
        }
      }
    });
  }, heartbeatIntervalMs());
}

async function stop() {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer);
    heartbeatTimer = null;
  }
  if (!config.eurekaEnabled || !active) {
    return;
  }
  active = false;
  try {
    await deregister();
    console.log('[eureka] deregistered');
  } catch (err) {
    console.warn('[eureka] deregister failed:', err.message);
  }
}

module.exports = {
  start,
  stop,
};
