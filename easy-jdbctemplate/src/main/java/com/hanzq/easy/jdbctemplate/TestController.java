package com.hanzq.easy.jdbctemplate;

import com.alibaba.fastjson.JSON;

import java.util.Base64;
import java.util.HashMap;

/**
 * Created by 韩志强(18297397903@163.com) on 2023/5/25
 */
public class TestController {
    public static void main(String[] args) {
        HashMap<String, Object> encodeMap = new HashMap<>();
        encodeMap.put("status","Success");
        encodeMap.put("privileges","use");
        encodeMap.put("type","es");
        encodeMap.put("typeDesc","Elasticsearch");

        JSON encodeJson = (JSON) JSON.toJSON(encodeMap);

        System.err.println(Base64.getEncoder().encodeToString(encodeJson.toString().getBytes()));
    }
}
