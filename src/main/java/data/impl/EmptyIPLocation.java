package data.impl;

import data.IPLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ZT 2022-12-20 14:08
 */
public class EmptyIPLocation implements IPLocation {

    private static Map<String, Object> getEmptyMap() {
        HashMap<String, Object> stringObjectHashMap = new HashMap<>();
        stringObjectHashMap.put("country_name", "");
        stringObjectHashMap.put("region_name", "");
        stringObjectHashMap.put("city_name", "");
        return stringObjectHashMap;
    }

    @Override
    public Map<String, Object> LocateBySingleIP(String ip) {
        return getEmptyMap();
    }

    @Override
    public Map<String, Object> LocateBySingleIPWithLoadBalance(String ip) {
        return getEmptyMap();
    }
}
