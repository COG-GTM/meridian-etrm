package com.meridianenergy.etrm.web;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JsonUtilTest {

    @Test
    public void quotesStrings() {
        assertEquals("\"hello\\\"world\"", JsonUtil.quote("hello\"world"));
    }

    @Test
    public void rendersObjectsAndArrays() {
        Map<String, Object> values = new LinkedHashMap<String, Object>();
        values.put("count", 2);
        assertTrue(JsonUtil.object(values).contains("\"count\":2"));
        assertEquals("[1,2]", JsonUtil.array(Arrays.asList("1", "2")));
    }
}
