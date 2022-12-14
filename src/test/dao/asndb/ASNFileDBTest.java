package dao.asndb;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressSeqRange;
import inet.ipaddr.IPAddressString;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ASNFileDBTest {

    public static class ASNFileItemTest {

        public static List<ASNFileDB.ASNFileItem> getItemList(IPAddressSeqRange... args) {
            List<ASNFileDB.ASNFileItem> list = new ArrayList<>();
            for (IPAddressSeqRange arg : args) {
                list.add(new ASNFileDB.ASNFileItem(arg, 0));
            }
            return list;
        }

        @Test
        public void mergeTest1() {
            IPAddressSeqRange range0 = new IPAddressString("1.0.128.0").getAddress().toSequentialRange(new IPAddressString("1.0.140.255").getAddress());

            IPAddress address1 = new IPAddressString("1.1.128.0").getAddress();
            IPAddress address2 = new IPAddressString("1.1.140.255").getAddress();
            IPAddressSeqRange range1 = address1.toSequentialRange(address2);

            IPAddress address3 = new IPAddressString("1.1.141.0").getAddress();
            IPAddress address4 = new IPAddressString("1.1.178.255").getAddress();
            IPAddressSeqRange range2 = address3.toSequentialRange(address4);

            List<IPAddressSeqRange> actual = ASNFileDB.ASNFileItem.merge(getItemList(range0, range1, range2));
            List<IPAddressSeqRange> expected = List.of(
                    range0,
                    new IPAddressString("1.1.128.0").getAddress().toSequentialRange(new IPAddressString("1.1.178.255").getAddress()));
            Assert.assertArrayEquals(expected.toArray(), actual.toArray());

        }

        @Test
        public void mergeTest2() {
            IPAddressSeqRange range_1 = new IPAddressString("1.0.126.0").getAddress().toSequentialRange(new IPAddressString("1.0.126.255").getAddress());
            IPAddressSeqRange range0 = new IPAddressString("1.0.128.0").getAddress().toSequentialRange(new IPAddressString("1.0.140.255").getAddress());

            IPAddressSeqRange range1 = new IPAddressString("1.1.128.0").getAddress().toSequentialRange(new IPAddressString("1.1.140.255").getAddress());

            IPAddressSeqRange range2 = new IPAddressString("1.1.141.0").getAddress().toSequentialRange(new IPAddressString("1.1.178.255").getAddress());

            IPAddressSeqRange range3 = new IPAddressString("1.2.141.0").getAddress().toSequentialRange(new IPAddressString("12.178.255").getAddress());

            List<IPAddressSeqRange> actual = ASNFileDB.ASNFileItem.merge(getItemList(range1, range0, range2, range_1, range3));
            List<IPAddressSeqRange> expected = List.of(
                    range_1,
                    range0,
                    new IPAddressString("1.1.128.0").getAddress().toSequentialRange(new IPAddressString("1.1.178.255").getAddress()),
                    range3);
            Assert.assertArrayEquals(expected.toArray(), actual.toArray());
        }

        @Test
        public void mergeTest3() {
            IPAddressSeqRange range0 = new IPAddressString("1.1.1.0").getAddress().toSequentialRange(new IPAddressString("1.1.127.255").getAddress());

            IPAddressSeqRange range1 = new IPAddressString("1.1.128.0").getAddress().toSequentialRange(new IPAddressString("1.1.140.255").getAddress());

            IPAddressSeqRange range2 = new IPAddressString("1.1.141.0").getAddress().toSequentialRange(new IPAddressString("1.1.178.255").getAddress());

            List<IPAddressSeqRange> actual = ASNFileDB.ASNFileItem.merge(getItemList(range1, range0, range2));
            List<IPAddressSeqRange> expected = List.of(
                    new IPAddressString("1.1.1.0").getAddress().toSequentialRange(new IPAddressString("1.1.178.255").getAddress()));
            Assert.assertArrayEquals(expected.toArray(), actual.toArray());
        }

        @Test
        public void mergeTest4() {
            IPAddressSeqRange range0 = new IPAddressString("1.1.1.0").getAddress().toSequentialRange(new IPAddressString("1.1.127.255").getAddress());

            List<IPAddressSeqRange> actual = ASNFileDB.ASNFileItem.merge(getItemList(range0));
            List<IPAddressSeqRange> expected = List.of(
                    new IPAddressString("1.1.1.0").getAddress().toSequentialRange(new IPAddressString("1.1.127.255").getAddress()));
            Assert.assertArrayEquals(expected.toArray(), actual.toArray());
        }

        @Test

        public void mergeTest5() {
            IPAddressSeqRange range0 = new IPAddressString("1.1.1.0").getAddress().toSequentialRange(new IPAddressString("1.1.127.255").getAddress());
            IPAddressSeqRange range1 = new IPAddressString("2.1.1.0").getAddress().toSequentialRange(new IPAddressString("2.1.127.255").getAddress());

            List<IPAddressSeqRange> actual = ASNFileDB.ASNFileItem.merge(getItemList(range0, range1));
            List<IPAddressSeqRange> expected = List.of(
                    range0,
                    range1);
            Assert.assertArrayEquals(expected.toArray(), actual.toArray());
        }
    }

}