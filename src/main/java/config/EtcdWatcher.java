package config;

import base.Constants;
import config.cfg.DatabaseConfig;
import io.etcd.jetcd.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public enum EtcdWatcher {
    DATABASE_CONFIG_WATCHER(Constants.ETCD_KEY_DATABASE, Constants.ETCD_HOST);

    private String etcdKey;
    private String hosts;
    private Client etcdClient;
    private Watch databaseConfigWatcher;
    private KV databaseConfig;
    private Logger logger = LoggerFactory.getLogger(EtcdWatcher.class);

    EtcdWatcher(String etcdKey, String hosts) {
        this.etcdKey = etcdKey;
        this.hosts = hosts;
        try {
            String[] endpoints = this.hosts.split(",");
            for (int i = 0; i < endpoints.length; i++) {
                if (!endpoints[i].startsWith("http://")) {
                    endpoints[i] = "http://" + endpoints[i];
                }
            }
            logger.info("Now connecting to etcd: "+String.join(";", endpoints));
            this.etcdClient = Client.builder().endpoints(endpoints).build();
            logger.info("connected to etcd "+ this.etcdClient);
            this.databaseConfigWatcher = this.etcdClient.getWatchClient();
            if (this.databaseConfigWatcher != null) {
                logger.info("get watcher to db");
            }
            this.databaseConfig = this.etcdClient.getKVClient();
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage());
        }
    }

    public DatabaseConfig getDatabaseConfig() {
        List<KeyValue> config = null;
        while (true) {
            try {
                config = this.databaseConfig.get(ByteSequence.from(this.etcdKey.getBytes())).get().getKvs();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            if (config != null) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error("error: " + e.getMessage());
                return null;
            }
        }
        logger.info("get config success "+ config);
        return DatabaseConfig.fromList(config);
    }

    public DatabaseConfig watchDatabaseConfig(Function<KeyValue, DatabaseConfig> callback) {
        AtomicReference<DatabaseConfig> cfg = new AtomicReference<>(DatabaseConfig.DEFAULT_CONFIG);
        //TODO 检测不到配置更新
        databaseConfigWatcher.watch(ByteSequence.from(this.etcdKey.getBytes()), listener -> listener.getEvents().forEach(event -> {
            switch (event.getEventType()) {
                case DELETE -> {
                    cfg.set(callback.apply(event.getKeyValue()));
                    logger.info(this.etcdKey + " deleted");
                }
                case PUT -> {
                    cfg.set(callback.apply(event.getKeyValue()));
                    logger.info(this.etcdKey + " has been changed");
                }
                default -> {
                }
            }
        }));
        return cfg.get();
    }
}
