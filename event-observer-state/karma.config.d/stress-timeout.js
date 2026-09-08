// Stress workloads intentionally exceed Mocha's two-second default on browser targets.
// Keep a finite bound: a stuck coroutine must still fail the suite.
config.set({
    client: {
        ...config.client,
        mocha: { ...config.client.mocha, timeout: 60000 }
    }
});
