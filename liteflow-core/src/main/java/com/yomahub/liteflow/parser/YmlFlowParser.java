package com.yomahub.liteflow.parser;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

/**
 * Yml格式解析器，转换为json格式进行解析
 * @author guodongqing
 * @since 2.5.0
 */
public abstract class YmlFlowParser extends JsonFlowParser{

    private final Logger LOG = LoggerFactory.getLogger(YmlFlowParser.class);

    @Override
    public void parse(String content) throws Exception {
        if (StrUtil.isBlank(content)){
            return;
        }
        JSONObject ruleObject = convertToJson(content);
        super.parse(ruleObject.toJSONString());
    }

    protected JSONObject convertToJson(String yamlString) {
        Yaml yaml= new Yaml();
        Map<String, Object> map = yaml.load(yamlString);
        JSONObject jsonObject = new JSONObject(map);
        return jsonObject;
    }
}
