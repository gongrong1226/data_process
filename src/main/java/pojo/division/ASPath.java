package pojo.division;

import division.PrintablePath;

import java.awt.print.Printable;
import java.util.List;

/**
 * @author ZT 2022-12-06 21:54
 */
public class ASPath implements PrintablePath {

    private List<AS> path;

    private String pathString;

    public ASPath(List<AS> path) {
        this.path = path;
    }

    public List<AS> getPath() {
        return path;
    }

    public void setPath(List<AS> path) {
        this.path = path;
    }

    public String getPathString() {
        if (pathString == null || pathString.length() == 0) {
            StringBuilder sb = new StringBuilder();
            for (AS as : path) {
                sb.append(as.getASN());
                sb.append("|");
            }
            sb.deleteCharAt(sb.length() - 1);
            pathString = sb.toString();
        }
        return pathString;
    }

    @Override
    public String toString() {
        return pathString;
    }
}
