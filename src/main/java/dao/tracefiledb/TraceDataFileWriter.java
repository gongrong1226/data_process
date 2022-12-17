package dao.tracefiledb;

import com.zfoo.protocol.ProtocolManager;
import dao.TracerouteWriter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledHeapByteBuf;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import division.path.Traceroute;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


/**
 * 将TraceData顺序写文件
 * 所有数据文件的根目录为：traceDataFilePath
 * 在这个根目录之下，按照 时间戳--IP的AB段 这两级目录进行构造。
 * 例如：/traceDataFilePath/2022-12-10-15-08-00/192-168
 *
 * @author ZT 2022-12-10 15:08
 * @see dao.tracefiledb.TraceDataFileWriter#traceDataFilePath
 */
public class TraceDataFileWriter extends AbstractTraceDataWriter {

    private final Logger logger = LoggerFactory.getLogger(TraceDataFileWriter.class);

    private static final String DEPRECATED = "DEPRECATED";

    private DatabaseProxy databaseProxy;

    /**
     * 所有Trace文件的根目录
     */
    private final String traceDataFilePath;

    public TraceDataFileWriter(TracerouteWriter tracerouteWriter, String traceDataFilePath) {
        super(tracerouteWriter);
        this.traceDataFilePath = traceDataFilePath;
    }

    /**
     * 初始化数据库、holder等等
     *
     * @throws IOException e
     */
    public void init() throws IOException {
        // 遍历
        File file = new File(traceDataFilePath);
        if (!file.exists()) {
            Files.createDirectories(Paths.get(traceDataFilePath));
            logger.info(String.format("Create directory %s successful", traceDataFilePath));
        }
        File[] files = file.listFiles(f -> f.isDirectory() && !f.getName().contains(DEPRECATED));
        if (files == null || files.length == 0) {
            databaseProxy = new DatabaseProxy(TraceDatabase.newTraceDatabase(traceDataFilePath));
        } else if (files.length == 1) {
            databaseProxy = new DatabaseProxy(new TraceDatabase(files[0].getAbsolutePath()));
        } else {
            Arrays.sort(files, (f1, f2) -> (int) (f1.lastModified() - f2.lastModified()));
            databaseProxy = new DatabaseProxy(new TraceDatabase(files[files.length - 1].getAbsolutePath()));
        }
    }

    /**
     * 用作原子化获取对象与访问，相当于以下代码的原子化
     * TraceDatabase d = getDatabase(); // getDatabase是新旧更替时的原子化
     * d.access();
     * 如果上述不是原子化，那么在新旧更替，等待旧数据库所有操作完成时，有可能会存在以下这个情况，
     * 导致创建完traceroute以后还有线程去对这个数据库写入
     * time  thread1                      thread2
     * |                               d = getDatabase()
     * |   d = getDatabase()
     * |   d.waitForFree()
     * |      ...build traceroute
     * |      ...delete old
     * |       done
     * |                               d.write(traceData)
     */
    static final class DatabaseProxy {
        private TraceDatabase database;

        public DatabaseProxy(TraceDatabase database) {
            this.database = database;
        }

        /**
         * 代理写，加上了访问计数操作
         *
         * @param simpleTraceData data
         */
        public void write(Traceroute.SimpleTraceData simpleTraceData) {
            TraceDatabase traceDatabase = accessDatabase();
            traceDatabase.writeData(simpleTraceData);
            traceDatabase.decAccess();
        }

        public void flush() {
            TraceDatabase traceDatabase = accessDatabase();
            traceDatabase.flush();
            traceDatabase.decAccess();
        }

        private synchronized TraceDatabase accessDatabase() {
            database.incAccess();
            return database;
        }

        /**
         * 在需要最后整理成traceroute时调用
         * 这里调用之后，其他写线程不可能再访问到这个数据库，因此old的access已经固定，不会再增加，只会减少
         *
         * @param database database
         * @return database
         */
        public synchronized TraceDatabase changeDatabase(TraceDatabase database) {
            TraceDatabase old = this.database;
            this.database = database;
            return old;
        }
    }


    /**
     * 对应每一个文件的访问
     */
    static final class FileAccessor implements Iterator<Traceroute.SimpleTraceData> {
        static {
            ProtocolManager.initProtocol(Set.of(Traceroute.SimpleTraceData.class));
        }

        private final Logger logger = LoggerFactory.getLogger(FileAccessor.class);
        private static final int PAGE_SIZE = 4096;
        private static final int INITIAL_CAPACITY = PAGE_SIZE;
        private static final int MAX_CAPACITY = 8 * PAGE_SIZE;

        /**
         * 32K, 如果有1亿个连续IP，那么就会有100,000,000/65536=1,525个B段， 占用32K*1,525=47.68MB内存
         * TODO 优化成Pooled直接内存或者map
         */
        private final ByteBuf serializeByteBuf = new UnpooledHeapByteBuf(ByteBufAllocator.DEFAULT, INITIAL_CAPACITY, MAX_CAPACITY);
        /**
         * 最多65536个IP，如果每个IP有10个可见跳，每一跳对应的一个SimpleTraceData大概50 bytes，则总共约有31.25MB大小
         * TODO 优化成Pooled直接内存或者map
         */
        private ByteBuf deserializeByteBuf;

        private final File file;

        public FileAccessor(String fileName) throws IOException {
            this(new File(fileName));
        }

        public FileAccessor(File file) throws IOException {
            this.file = file;
            if (!file.exists()) {
                if (file.createNewFile()) {
                    logger.info(String.format("Create file %s successful.", file.getAbsolutePath()));
                } else {
                    logger.error(String.format("Create file %s failed.", file.getAbsolutePath()));
                }
            }
        }

        public synchronized void write(Traceroute.SimpleTraceData data) {
            // 剩余容量不足就先写文件
            if (MAX_CAPACITY - serializeByteBuf.writerIndex() < 100) {
                flush();
            }
            ProtocolManager.write(serializeByteBuf, data);
        }

        public synchronized void flush() {
            try {
                FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.APPEND);
                fileChannel.write(serializeByteBuf.nioBuffer());
                serializeByteBuf.clear();
                fileChannel.close();
            } catch (IOException e) {
                logger.error(String.format("flush byteBuf into file %s failed. Exception=%s", file.toPath(), e));
            }
        }


        @Override
        public boolean hasNext() {
            if (deserializeByteBuf == null) {
                deserializeByteBuf = new UnpooledHeapByteBuf(ByteBufAllocator.DEFAULT,
                        (int) file.length(), (int) file.length());
                FileChannel fileChannel;
                try {
                    fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
                    deserializeByteBuf.writeBytes(fileChannel, 0, (int) file.length());
                    fileChannel.close();
                } catch (IOException e) {
                    logger.error(String.format("read file %s failed. Exception=%s", file.getAbsolutePath(), e));
                    return false;
                }
            }
            if (deserializeByteBuf.readableBytes() == 0) {
                // help gc
                deserializeByteBuf = null;
                return false;
            }
            return true;
        }

        @Override
        public Traceroute.SimpleTraceData next() {
            return (Traceroute.SimpleTraceData) ProtocolManager.read(deserializeByteBuf);
        }

        public File getFile() {
            return file;
        }
    }

    static final class TraceDatabase {
        private final Logger logger = LoggerFactory.getLogger(TraceDatabase.class);

        private final File root;
        // <filename, accessor>  也就是 <IPB段, accessor>
        private final ConcurrentMap<String, FileAccessor> fileMap;
        private final AtomicInteger accessCount;


        /**
         * 需要保证rootPath存在
         *
         * @param rootPath path
         */
        public TraceDatabase(String rootPath) {
            root = new File(rootPath);
            fileMap = new ConcurrentHashMap<>();
            accessCount = new AtomicInteger();
            initDatabase();
        }

        /**
         * 扫描rootPath下已有的数据文件，初始化fileMap
         */
        private void initDatabase() {
            File[] files = root.listFiles();
            if (files == null) {
                return;
            }
            for (File file : files) {
                try {
                    FileAccessor fileAccessor = new FileAccessor(file.getAbsolutePath());
                    fileMap.put(file.getName(), fileAccessor);
                } catch (IOException e) {
                    logger.error(String.format("new FileAccessor error=%s, filepath=%s", e, file.getAbsolutePath()));
                    e.printStackTrace();
                }
            }
        }

        /**
         * 在获取到的时候就加1
         */
        public void incAccess() {
            accessCount.incrementAndGet();
        }

        /**
         * 在使用完了之后一定要记得归还
         */
        public synchronized void decAccess() {
            int i = accessCount.decrementAndGet();
            if (i < 0) {
                logger.error(String.format("Error accessCount, should not be there. accessCount=%d", i));
                throw new RuntimeException("Error Access Exception");
            }
            if (i == 0) {
                this.notifyAll();
            }
        }

        /**
         * 等待访问清零
         *
         * @throws InterruptedException ..
         */
        public synchronized void waitDatabaseFree() throws InterruptedException {
            int i = accessCount.get();
            if (i == 0) {
                return;
            }
            this.wait();
            for (FileAccessor value : fileMap.values()) {
                value.flush();
            }
        }


        /**
         * 提取IP前16位作为文件名
         *
         * @param IP ip
         * @return key/filename
         */
        private static String getFileName(String IP) {
            String[] split = IP.split("\\.");
            return split[0] + "-" + split[1];
        }

        private FileAccessor getFileAccessor(String IP) {
            String fileName = getFileName(IP);
            FileAccessor fileAccessor = fileMap.get(fileName);
            if (fileAccessor == null) {
                Path absPath = Path.of(root.getAbsolutePath(), fileName);
                try {
                    fileAccessor = new FileAccessor(absPath.toFile());
                    fileMap.putIfAbsent(fileName, fileAccessor);
                    fileAccessor = fileMap.get(fileName);
                } catch (IOException e) {
                    logger.error(String.format("Create new FileAccessor error=%s, absPath=%s", e, absPath));
                    return null;
                }
            }
            return fileAccessor;
        }

        public void writeData(Traceroute.SimpleTraceData simpleTraceData) {
            FileAccessor fileAccessor = getFileAccessor(simpleTraceData.getDest());
            if (fileAccessor == null) {
                logger.error(String.format("Cannot get fileAccessor for IP %s", simpleTraceData.getDest()));
                return;
            }
            fileAccessor.write(simpleTraceData);
        }

        public void flush() {
            fileMap.forEach((k, v) -> v.flush());
        }

        public String getRootPath() {
            return root.getAbsolutePath();
        }

        public List<FileAccessor> getAllFileAccessors() {
            return new ArrayList<>(fileMap.values());
        }

        /**
         * 使用当前时间戳作为数据库根目录名称，再初始化
         *
         * @return database
         */
        public static TraceDatabase newTraceDatabase(String parentPath) throws IOException {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");//设置日期格式
            String date = df.format(new Date());
            Path path = Paths.get(parentPath, date);
            Files.createDirectories(path);
            return new TraceDatabase(path.toString());
        }

        public void hardDelete() throws IOException {
            FileUtils.deleteDirectory(root);
        }

        public boolean softDelete() {
            Path path = root.toPath();
            String s = path.getFileName().toString();
            s = String.format("%s_%s", s, DEPRECATED);
            Path parent = path.getParent();
            File file = Path.of(parent.toString(), s).toFile();
            return root.renameTo(file);
        }
    }


    /**
     * 确保对当前数据库的所有访问已经结束，并且之后不会再访问到该数据库
     *
     * @return database
     */
    private TraceDatabase waitAllFileReady() {
        logger.info("[waiting for AllFileReady]");
        try {
            // 准备一个新的文件夹， 也就是一个新的数据库
            TraceDatabase newTraceDatabase = TraceDatabase.newTraceDatabase(traceDataFilePath);
            TraceDatabase oldTraceDatabase = databaseProxy.changeDatabase(newTraceDatabase);
            oldTraceDatabase.waitDatabaseFree();
            return oldTraceDatabase;
        } catch (InterruptedException | IOException e) {
            logger.error(String.format("[waitDatabaseFree] error, exception=%s", e));
            return null;
        }
    }

    /**
     * 需要保证线程安全
     *
     * @param simpleTraceData data
     */
    @Override
    protected void write(Traceroute.SimpleTraceData simpleTraceData) {
        databaseProxy.write(simpleTraceData);
    }


    @Override
    protected void buildTraceroute() {
    }

    @Override
    protected void clearOldTraceData() {

    }

    /**
     * 1. 构建traceroute并写入数据库
     * <p>
     * 2. 清理旧的
     */
    @Override
    public void buildAndClear() {
        // 需要等待所有的老文件都已经写完，不丢数据
        TraceDatabase traceDatabase = waitAllFileReady();
        if (traceDatabase == null) {
            logger.error("waitAllFileReady returns null.");
            return;
        }
        logger.info("building traceroute...");
        long startTime = System.currentTimeMillis();
        // 获取所有文件
        List<FileAccessor> allFileAccessors = traceDatabase.getAllFileAccessors();
        Traceroute.TracerouteBuilder tracerouteBuilder = new Traceroute.TracerouteBuilder();
        for (FileAccessor fileAccessor : allFileAccessors) {
            logger.info(String.format("building traceroute -- file %s", fileAccessor.getFile().getAbsolutePath()));
            long fileStartTime = System.currentTimeMillis();
            // 获取该文件下的所有trace数据
            List<Traceroute.SimpleTraceData> list = new ArrayList<>(32768);
            while (fileAccessor.hasNext()) {
                Traceroute.SimpleTraceData next = fileAccessor.next();
                list.add(next);
            }
            // 根据IP分组
            Map<String, List<Traceroute.SimpleTraceData>> destTraces = list.stream()
                    .filter(d -> d != null && d.getDest() != null)
                    .collect(Collectors.groupingBy(Traceroute.SimpleTraceData::getDest));
//                    // 跳数少的在前面
//                    .sorted(Comparator.comparingInt(SimpleTraceData::getHop))
//                    // 以dest为key，进行groupby
//                    .collect(Collectors.groupingBy(SimpleTraceData::getDest, LinkedHashMap::new, Collectors.toList()));
            // 对每个IP的traceroute路径进行重组
            // TODO 如果这里太慢，转换成并行流处理
            long beforeBuildTime = System.currentTimeMillis();
            List<Traceroute> traceroutes = destTraces.entrySet().stream()
                    .parallel()
                    .map(stringListEntry -> {
                        String dest = stringListEntry.getKey();
                        List<Traceroute.SimpleTraceData> value = stringListEntry.getValue();
                        return tracerouteBuilder.build(dest, value, new StringBuilder());
                    })
                    .collect(Collectors.toList());
            long beforeWrite = System.currentTimeMillis();
            long buildCost = beforeWrite - beforeBuildTime;
            tracerouteWriter.write(traceroutes);
            long currentTimeMillis = System.currentTimeMillis();
            long fileCost = currentTimeMillis - fileStartTime;
            long writeCost = currentTimeMillis - beforeWrite;
            logger.info(String.format("built traceroute -- file %s cost %s ms, includes %s ms for building and %s ms for writing database",
                    fileAccessor.getFile().getAbsolutePath(), fileCost, buildCost, writeCost));
        }
        long allCost = System.currentTimeMillis() - startTime;
        logger.info(String.format("build traceroute %s end cost %s ms", traceDatabase.getRootPath(), allCost));

        // 最后一步，删除旧的trace数据
        try {
            traceDatabase.hardDelete();
        } catch (IOException e) {
            boolean b = traceDatabase.softDelete();
            logger.error(String.format("database %s hardDelete failed. Plan B softDelete %s", traceDatabase.getRootPath(), b));
        }
    }

    /**
     * IGNORE
     *
     * @return null
     */
    @Override
    protected TracerouteIterator getTracerouteIterator() {
        return null;
    }

    public void flush() {
        databaseProxy.flush();
    }
}
