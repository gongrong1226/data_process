package dao.asndb;

import dao.IP2ASN;
import inet.ipaddr.AddressValueException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressSeqRange;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.ipv4.IPv4Address;
import inet.ipaddr.ipv4.IPv4AddressSection;
import inet.ipaddr.ipv4.IPv4AddressTrie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 整个占用约222156848 Bytes -> 211MB内存
 *
 * @author ZT 2022-12-13 16:43
 */
public class ASNFileDB implements IP2ASN {

    private static int START_ADDRESS_IDX = 0;
    private static int END_ADDRESS_IDX = 1;
    private static int AS_NUMBER_IDX = 2;
    private static int COUNTRY_CODE_IDX = 3;
    private static int AS_DESCRIPTION_IDX = 3;

    private final Logger logger = LoggerFactory.getLogger(ASNFileDB.class);

    private final File dbFile;

    private IPv4AddressTrie trie;

    public ASNFileDB(File dbFile) {
        this.dbFile = dbFile;
    }


    private static class AddressWithASN extends IPv4Address {
        private int asn;

        public AddressWithASN(IPv4AddressSection section, int asn) throws AddressValueException {
            super(section);
            this.asn = asn;
        }

        public AddressWithASN(Inet4Address inet4Address, int asn) {
            super(inet4Address);
            this.asn = asn;
        }

        public int getAsn() {
            return asn;
        }

        public void setAsn(int asn) {
            this.asn = asn;
        }

    }

    static class ASNFileItem {
        IPAddressSeqRange range;
        int asn;

        public ASNFileItem(IPAddressSeqRange range, int asn) {
            this.range = range;
            this.asn = asn;
        }

        public static List<IPAddressSeqRange> merge(List<ASNFileItem> items) {
            items.sort(Comparator.comparing(a -> a.range.getLower().getValue()));
            List<IPAddressSeqRange> list = new ArrayList<>();
            IPAddressSeqRange base = items.get(0).range;
            for (int i = 1; i < items.size(); i++) {
                IPAddressSeqRange append = items.get(i).range;
                IPAddressSeqRange result = base.join(append);
                // 无共同空间，把之前的base结果放入，新base就是当前索引的append
                if (result == null) {
                    list.add(base);
                    base = append;
                } else {
                    // 有共同空间，继续融合
                    base = result;
                }
            }
            list.add(base);
            return list;
        }

    }

    public void init() throws IOException {
        if (trie != null) {
            return;
        }
        logger.info("initializing asn file db ...");
        BufferedReader reader = new BufferedReader(new FileReader(dbFile));
        List<AddressWithASN> collect = reader.lines()
                .map((line) -> {
                    String[] split = line.split("\\s+");
                    IPAddress start = new IPAddressString(split[START_ADDRESS_IDX]).getAddress();
                    IPAddress end = new IPAddressString(split[END_ADDRESS_IDX]).getAddress();
                    IPAddressSeqRange range = start.toSequentialRange(end);
                    int asn = 0;
                    try {
                        asn = Integer.parseInt(split[AS_NUMBER_IDX]);
                    } catch (Exception e) {
                        logger.error(String.format("parse string to int error, string=%s, exception=%s", split[AS_NUMBER_IDX], e));
                    }
                    return new ASNFileItem(range, asn);
                })
                .filter(t -> t.asn != 0)
                .collect(Collectors.groupingBy(t -> t.asn))
                .entrySet()
                .stream()
                .map(entry -> {
                    List<ASNFileItem> value = entry.getValue();
                    int asn = entry.getKey();
                    List<IPAddressSeqRange> merge = ASNFileItem.merge(value);
                    List<AddressWithASN> list = new ArrayList<>();
                    for (IPAddressSeqRange ipAddressSeqRange : merge) {
                        for (IPAddress ipAddress : ipAddressSeqRange.spanWithPrefixBlocks()) {
                            list.add(new AddressWithASN(ipAddress.toIPv4().getSection(), asn));
                        }
                    }
                    return list;
                })
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        trie = new IPv4AddressTrie();
        for (AddressWithASN iPv4Addresses : collect) {
            trie.add(iPv4Addresses);
        }
        logger.info("asn file db initialized");
    }

    @Override
    public int queryASN(String IP) {
        IPv4Address iPv4Address = new IPAddressString(IP).getAddress().toIPv4();
        return queryASN(iPv4Address);
    }

    private int queryASN(IPv4Address iPv4Address) {
        if (trie == null) {
            synchronized (ASNFileDB.class) {
                if (trie == null) {
                    try {
                        init();
                    } catch (IOException e) {
                        logger.error(String.format("init asn file db error. Exception=%s", e));
                        System.exit(2);
                        return 0;
                    }
                }
            }
        }
        IPv4AddressTrie.IPv4TrieNode node = trie.longestPrefixMatchNode(iPv4Address);
        if (node == null) {
            logger.warn(String.format("IP=%s no asn in the db. By default 0.", iPv4Address));
            return 0;
        }
        AddressWithASN key1 = (AddressWithASN) node.getKey();
        return key1.asn;
    }

    @Override
    public int queryASN(int IP) {
        IPv4Address iPv4Address = new IPv4Address(IP);
        return queryASN(iPv4Address);
    }
}
