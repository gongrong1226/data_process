package dao.tracefiledb;

import dao.TracerouteWriter;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import division.path.Traceroute;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class TraceDataFileWriterTest {


    public static String int2IP(int ipAddress) {
        return String.format("%d.%d.%d.%d", (ipAddress >> 24 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 8 & 0xff), (ipAddress & 0xff));
    }


    private static Traceroute.SimpleTraceData genRandomSimpleTraceData() {
        Random random = new Random();
        String dest = int2IP(random.nextInt());
        String response = int2IP(random.nextInt());
        Integer hop = 1000000;
        Integer RTT = 1000000;
        Instant time = Instant.now();
        long epochSecond = time.getEpochSecond();
        epochSecond = epochSecond * 1_000_000_000L + time.getNano();
        return new Traceroute.SimpleTraceData(dest, response, hop, RTT, epochSecond);
    }

    public static class FileAccessorTest {
        @Test
        public void TestWriteAndRead() {
//            String path = "/tmp/data_process/fileAccessorTest/";
            String path = "./fileAccessorTest";
            TraceDataFileWriter.FileAccessor fileAccessor;
            try {
                fileAccessor = new TraceDataFileWriter.FileAccessor(path);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            int size = 1000;
            List<Traceroute.SimpleTraceData> expected = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                var data = genRandomSimpleTraceData();
                expected.add(data);
                fileAccessor.write(data);
            }
            fileAccessor.flush();
            List<Traceroute.SimpleTraceData> actual = new ArrayList<>(size);
            while (fileAccessor.hasNext()) {
                Traceroute.SimpleTraceData next = fileAccessor.next();
                actual.add(next);
            }
            File file = new File(path);
            file.delete();
            Assert.assertArrayEquals(expected.toArray(), actual.toArray());
        }
    }

    public static class TracerouteBuilderTest {

        @Test
        public void TestBuildTraceroute() {
            List<Traceroute.SimpleTraceData> allTraces = new ArrayList<>();
            // 测试可达
            Traceroute.TracerouteBuilder tracerouteBuilder = new Traceroute.TracerouteBuilder();
            String dest1 = "1.0.0.1";
            List<Traceroute.SimpleTraceData> tmp = List.of(
                    new Traceroute.SimpleTraceData(dest1, "1.0.0.2", 2, 10, 111L),
                    new Traceroute.SimpleTraceData(dest1, "1.0.0.9", 9, 10, 111L),
                    new Traceroute.SimpleTraceData(dest1, "1.0.0.1", 10, 10, 1100L),
                    new Traceroute.SimpleTraceData(dest1, "1.0.0.3", 3, 10, 111L),
                    new Traceroute.SimpleTraceData(dest1, "1.0.0.8", 8, 10, 111L)
            );
            allTraces.addAll(tmp);
            ArrayList<Traceroute.SimpleTraceData> traces1 = new ArrayList<>(tmp);
            Traceroute expectedTraceroute1 = new Traceroute(dest1, "*|1.0.0.2|1.0.0.3|*|*|*|*|1.0.0.8|1.0.0.9|1.0.0.1", true, 1100L);
            Traceroute actualTraceroute1 = tracerouteBuilder.build(dest1, traces1);
            Assert.assertEquals(expectedTraceroute1, actualTraceroute1);

            // 测试不可达
            String dest2 = "2.0.0.1";
            tmp = List.of(
                    new Traceroute.SimpleTraceData(dest2, "2.0.0.2", 2, 10, 111L),
                    new Traceroute.SimpleTraceData(dest2, "2.0.0.9", 9, 10, 1100L),
                    new Traceroute.SimpleTraceData(dest2, "2.0.0.3", 3, 10, 111L),
                    new Traceroute.SimpleTraceData(dest2, "2.0.0.8", 8, 10, 111L)
            );
            allTraces.addAll(tmp);
            ArrayList<Traceroute.SimpleTraceData> traces2 = new ArrayList<>(tmp);
            Traceroute expectedTraceroute2 = new Traceroute(dest2, "*|2.0.0.2|2.0.0.3|*|*|*|*|2.0.0.8|2.0.0.9", false, 1100L);
            Traceroute actualTraceroute2 = tracerouteBuilder.build(dest2, traces2);
            Assert.assertEquals(expectedTraceroute2, actualTraceroute2);

            // 测试流分组逻辑
            Map<String, List<Traceroute.SimpleTraceData>> destTraces = allTraces.stream()
                    .filter(d -> d != null && d.getDest() != null)
                    .collect(Collectors.groupingBy(Traceroute.SimpleTraceData::getDest));
            //根据IP分组
            List<Traceroute> actualTraceroutes = destTraces.entrySet().stream()
                    .map(stringListEntry -> {
                        String dest = stringListEntry.getKey();
                        List<Traceroute.SimpleTraceData> value = stringListEntry.getValue();
                        return tracerouteBuilder.build(dest, value, new StringBuilder());
                    })
                    .collect(Collectors.toList());

            actualTraceroutes.sort(Comparator.comparing(Traceroute::getTraceroute));
            ArrayList<Traceroute> expetedTraceroutes = new ArrayList<>(List.of(expectedTraceroute1, expectedTraceroute2));
            expetedTraceroutes.sort(Comparator.comparing(Traceroute::getTraceroute));
            Assert.assertArrayEquals(expetedTraceroutes.toArray(), actualTraceroutes.toArray());

        }


    }

    @Test
    public void TestTraceDataFileWriter() {
        List<Traceroute.SimpleTraceData> allTraces = new ArrayList<>();
        // traceroute1
        String dest1 = "1.0.0.1";
        List<Traceroute.SimpleTraceData> tmp = List.of(
                new Traceroute.SimpleTraceData(dest1, "1.0.0.2", 2, 10, 111L),
                new Traceroute.SimpleTraceData(dest1, "1.0.0.9", 9, 10, 111L),
                new Traceroute.SimpleTraceData(dest1, "1.0.0.1", 10, 10, 1100L),
                new Traceroute.SimpleTraceData(dest1, "1.0.0.3", 3, 10, 111L),
                new Traceroute.SimpleTraceData(dest1, "1.0.0.8", 8, 10, 111L)
        );
        allTraces.addAll(tmp);
        ArrayList<Traceroute.SimpleTraceData> traces1 = new ArrayList<>(tmp);
        Traceroute expectedTraceroute1 = new Traceroute(dest1, "*|1.0.0.2|1.0.0.3|*|*|*|*|1.0.0.8|1.0.0.9|1.0.0.1", true, 1100L);
        //traceroute2
        String dest2 = "2.0.0.1";
        tmp = List.of(
                new Traceroute.SimpleTraceData(dest2, "2.0.0.2", 2, 10, 111L),
                new Traceroute.SimpleTraceData(dest2, "2.0.0.9", 9, 10, 1100L),
                new Traceroute.SimpleTraceData(dest2, "2.0.0.3", 3, 10, 111L),
                new Traceroute.SimpleTraceData(dest2, "2.0.0.8", 8, 10, 111L)
        );
        allTraces.addAll(tmp);
        ArrayList<Traceroute.SimpleTraceData> traces2 = new ArrayList<>(tmp);
        Traceroute expectedTraceroute2 = new Traceroute(dest2, "*|2.0.0.2|2.0.0.3|*|*|*|*|2.0.0.8|2.0.0.9", false, 1100L);
        ArrayList<Traceroute> expetedTraceroutes = new ArrayList<>(List.of(expectedTraceroute1, expectedTraceroute2));

        // 测试
        List<Traceroute> actualTraceroutes = new ArrayList<>();
        TracerouteWriter tracerouteWriter = new TracerouteWriter() {
            @Override
            public void write(Traceroute traceroute) {
                actualTraceroutes.add(traceroute);
            }

            @Override
            public void write(List<Traceroute> traceroutes) {
                actualTraceroutes.addAll(traceroutes);
            }
        };

        String root = "./__test_tracedata/";
        File rootFile = new File(root);
        if (rootFile.exists()) {
            try {
                FileUtils.deleteDirectory(rootFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        TraceDataFileWriter traceDataFileWriter = new TraceDataFileWriter(tracerouteWriter, root);
        try {
            traceDataFileWriter.init();
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (Traceroute.SimpleTraceData trace : allTraces) {
            traceDataFileWriter.write(trace);
        }
        // check 应该存在1-0和2-0文件
        traceDataFileWriter.flush();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        File[] files = rootFile.listFiles(File::isDirectory);
        Assert.assertNotNull(files);
        Assert.assertEquals(1, files.length);
        File[] files1 = files[0].listFiles();
        Assert.assertNotNull(files1);
        Assert.assertEquals(2, files1.length);
        Object[] filenames = Arrays.stream(files1).map(File::getName).sorted().toArray();
        Assert.assertArrayEquals(filenames, new String[]{"1-0", "2-0"});
        // check 构建
        traceDataFileWriter.buildAndClear();
        expetedTraceroutes.sort(Comparator.comparing(Traceroute::getTraceroute));
        actualTraceroutes.sort(Comparator.comparing(Traceroute::getTraceroute));
        Assert.assertArrayEquals(expetedTraceroutes.toArray(), actualTraceroutes.toArray());
    }
}