package dao;

import pojo.division.Traceroute;

import java.util.List;

/**
 *
 * @author ZT 2022-12-09 21:20
 */
public interface TracerouteWriter {

    void write(Traceroute traceroute);

    void write(List<Traceroute> traceroutes);

}
