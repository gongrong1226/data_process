package config.holder;

import base.Constants;
import config.cfg.DatabaseConfig;
import config.EtcdWatcher;
import io.etcd.jetcd.KeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * @author atractylodis
 */
public enum DatabaseConfigHolder {
    DEFAULT_CONFIG_HOLDER(kv -> {
        System.out.println("change change change");
        DatabaseConfig cfg = DatabaseConfig.DEFAULT_CONFIG;
        String key = kv.getKey().toString();
        String value = kv.getValue().toString();
        switch (key){
            case Constants.KEY_INFLUXDB_URL -> cfg.setUrl(value);
            case Constants.KEY_INFLUXDB_TOKEN -> cfg.setToken(value);
            case Constants.KEY_INFLUXDB_ORG -> cfg.setOrg(value);
            case Constants.KEY_INFLUXDB_BUCKET -> cfg.setBucket(value);
            case Constants.KEY_INFLUXDB_CLIENT_POOL_SIZE -> cfg.setClientPoolSize(Integer.parseInt(value));
            case Constants.KEY_INFLUXDB_BATCH_SIZE -> cfg.setBatchSize(Integer.parseInt(value));
            case Constants.KEY_INFLUXDB_FLUSH_INTERVAL -> cfg.setFlushInterval(Integer.parseInt(value));
            case Constants.KEY_INFLUXDB_JITTER_INTERVAL -> cfg.setJitterInterval(Integer.parseInt(value));
            case Constants.KEY_INFLUXDB_RETRY_INTERVAL -> cfg.setRetryInterval(Integer.parseInt(value));
            case Constants.KEY_INFLUXDB_MAX_RETRIES -> cfg.setMaxRetries(Integer.parseInt(value));
            case Constants.KEY_INFLUXDB_MAX_RETRY_DELAY -> cfg.setMaxRetryDelay(Integer.parseInt(value));
            case Constants.KEY_INFLUXDB_MAX_RETRY_TIME -> cfg.setMaxRetryTime(Integer.parseInt(value));
            default -> {

            }
        }
        return cfg;
    });

    private volatile DatabaseConfig databaseConfig;
    private EtcdWatcher etcdWatcher;
    private Function<KeyValue, DatabaseConfig> configCallback;
    private ExecutorService watcherWorker;
    private boolean needReload = false;
    private Lock lock;
    private Logger logger = LoggerFactory.getLogger(DatabaseConfigHolder.class);

    DatabaseConfigHolder(Function<KeyValue, DatabaseConfig> callback) {
        this.configCallback = callback;
        this.etcdWatcher = EtcdWatcher.DATABASE_CONFIG_WATCHER;
        this.databaseConfig = etcdWatcher.getDatabaseConfig();
        if (this.databaseConfig == null) {
            this.databaseConfig = DatabaseConfig.DEFAULT_CONFIG;
            logger.warn("cannot find configuration, use default_config");
        }
        this.watcherWorker = Executors.newSingleThreadExecutor();
        logger.info("get watcher worker");
        this.watcherWorker.execute(() -> {
            logger.info("start watch db config");
            while(true) {
                DatabaseConfig newCfg = this.etcdWatcher.watchDatabaseConfig(this.configCallback);
                this.watchCallBack(newCfg);
            }
        });
        this.lock = new ReentrantLock();
    }

    public void watchCallBack(DatabaseConfig config) {
        if (isSameConfig(this.databaseConfig, config)) return;
        this.databaseConfig = config;
        this.needReload = true;
        logger.info("detect configuration has changed");
//        this.configCallback.accept(config);
    }

    public boolean isNeedReload() {
        lock.lock();
        boolean need = this.needReload;
        this.needReload = false;
        lock.unlock();
        return need;
    }

    private boolean isSameConfig(DatabaseConfig old, DatabaseConfig newConfig) {
        return old.getUrl().equals(newConfig.getUrl()) &&
                old.getToken().equals(newConfig.getToken()) &&
                old.getOrg().equals(newConfig.getOrg()) &&
                old.getBucket().equals(newConfig.getBucket());
    }

    public DatabaseConfig getDatabaseConfig() {
        return databaseConfig;
    }
}
