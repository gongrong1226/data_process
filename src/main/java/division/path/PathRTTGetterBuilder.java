package division.path;

import java.util.ArrayList;
import java.util.List;

/**
 * 应对有多种get方式的情况
 * @author ZT 2022-12-19 11:26
 */
public class PathRTTGetterBuilder {

    private final List<PathRTTGetter> list;

    public PathRTTGetterBuilder() {
        list = new ArrayList<>();
    }

    public PathRTTGetterBuilder addLast(PathRTTGetter pathRTTGetter) {
        list.add(pathRTTGetter);
        return this;
    }

    private static class Node implements PathRTTGetter {
        Node next;
        PathRTTGetter pathRTTGetter;

        public Node(Node next, PathRTTGetter pathRTTGetter) {
            this.next = next;
            this.pathRTTGetter = pathRTTGetter;
        }

        @Override
        public int getRTT(PrintablePath path) {
            int rtt = pathRTTGetter.getRTT(path);
            if (rtt < 0 && next != null) {
                return next.getRTT(path);
            }
            return rtt;
        }
    }

    public PathRTTGetter build() {
        Node dummyHead = new Node(null, null);
        Node tail = dummyHead;
        for (PathRTTGetter pathRTTGetter : list) {
            tail.next = new Node(null, pathRTTGetter);
            tail = tail.next;
        }
        return dummyHead.next;
    }
}
