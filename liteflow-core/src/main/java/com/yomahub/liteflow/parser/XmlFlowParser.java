package com.yomahub.liteflow.parser;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.common.LocalDefaultFlowConstant;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.entity.flow.Chain;
import com.yomahub.liteflow.entity.flow.Condition;
import com.yomahub.liteflow.entity.flow.Executable;
import com.yomahub.liteflow.entity.flow.Node;
import com.yomahub.liteflow.exception.ExecutableItemNotFoundException;
import com.yomahub.liteflow.exception.ParseException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.spring.ComponentScanner;
import com.yomahub.liteflow.util.SpringAware;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

/**
 * xml形式的解析器
 * @author Bryan.Zhang
 */
public abstract class XmlFlowParser extends FlowParser{

	private final Logger LOG = LoggerFactory.getLogger(XmlFlowParser.class);

	public void parse(String content) throws Exception {
		if (StrUtil.isBlank(content)){
			return;
		}
		Document document = DocumentHelper.parseText(content);
		parse(document);
	}

	//xml形式的主要解析过程
	public void parse(Document document) throws Exception {
		try {
			Element rootElement = document.getRootElement();

			//判断是以spring方式注册节点，还是以xml方式注册
			if (ComponentScanner.nodeComponentMap.isEmpty()) {
				// 解析node节点
				List<Element> nodeList = rootElement.element("nodes").elements("node");
				String id;
				String clazz;
				for (Element e : nodeList) {
					id = e.attributeValue("id");
					clazz = e.attributeValue("class");
					FlowBus.addNode(id, clazz);
				}
			} else {
				for (Entry<String, NodeComponent> componentEntry : ComponentScanner.nodeComponentMap.entrySet()) {
					if (!FlowBus.containNode(componentEntry.getKey())){
						FlowBus.addNode(componentEntry.getKey(), new Node(componentEntry.getKey(), componentEntry.getValue().getClass().getName(), componentEntry.getValue()));
					}
				}
			}

			// 解析chain节点
			List<Element> chainList = rootElement.elements("chain");
			for (Element e : chainList) {
				parseOneChain(e);
			}
		} catch (Exception e) {
			String errorMsg = "FlowParser parser exception";
			LOG.error(errorMsg, e);
			throw new ParseException(errorMsg);
		}
	}

	/**
	 * 解析一个chain的过程
	 * @param e chain节点
	 * @throws Exception
	 */
	private void parseOneChain(Element e) throws Exception {
		String condArrayStr;
		String[] condArray;
		String group;
		String errorResume;
		Condition condition;
		Element condE;
		List<Executable> chainNodeList;
		List<Condition> conditionList;

		String chainName = e.attributeValue("name");
		// 增加循环加载组件时的加载过滤
		if (FlowBus.containChain(chainName)){
			return;
		}

		conditionList = new ArrayList<>();
		for (Iterator<Element> it = e.elementIterator(); it.hasNext();) {
			condE = it.next();
			condArrayStr = condE.attributeValue("value");
			errorResume = condE.attributeValue("errorResume");
			group = condE.attributeValue("group");
			if (StrUtil.isBlank(condArrayStr)) {
				continue;
			}
			if (StrUtil.isBlank(group)) {
				group = LocalDefaultFlowConstant.DEFAULT;
			}
			if (StrUtil.isBlank(errorResume)) {
				errorResume = Boolean.TRUE.toString();
			}
			condition = new Condition();
			chainNodeList = new ArrayList<>();
			condArray = condArrayStr.split(",");
			RegexEntity regexEntity;
			String itemExpression;
			String item;
			//这里解析的规则，优先按照node去解析，再按照chain去解析
			for (int i = 0; i < condArray.length; i++) {
				itemExpression = condArray[i].trim();
				regexEntity = parseNodeStr(itemExpression);
				item = regexEntity.getItem();
				if (FlowBus.containNode(item)) {
					Node node = FlowBus.getNode(item);
					chainNodeList.add(node);
					//这里判断是不是条件节点，条件节点会含有realItem，也就是括号里的node
					if (regexEntity.getRealItemArray() != null) {
						for (String key : regexEntity.getRealItemArray()) {
							if (FlowBus.containNode(key)){
								Node condNode = FlowBus.getNode(key);
								node.setCondNode(condNode.getId(), condNode);
							} else if (hasChain(e,key)) {
								Chain chain = FlowBus.getChain(key);
								node.setCondNode(chain.getChainName(), chain);
							}
						}
					}
				} else if (hasChain(e,item)) {
					Chain chain = FlowBus.getChain(item);
					chainNodeList.add(chain);
				} else {
					String errorMsg = StrUtil.format("executable node[{}] is not found!",regexEntity.getItem());
					throw new ExecutableItemNotFoundException(errorMsg);
				}
			}
			condition.setErrorResume(errorResume.equals(Boolean.TRUE.toString()));
			condition.setGroup(group);
			condition.setConditionType(condE.getName());
			condition.setNodeList(chainNodeList);
			super.buildBaseFlowConditions(conditionList,condition);
		}
		FlowBus.addChain(chainName, new Chain(chainName,conditionList));
	}

	//判断在这个FlowBus元数据里是否含有这个chain
	//因为chain和node都是可执行器，在一个规则文件上，有可能是node，有可能是chain
	private boolean hasChain(Element e,String chainName) throws Exception{
		Element rootElement = e.getParent();
		List<Element> chainList = rootElement.elements("chain");
		for (Element ce : chainList) {
			String ceName = ce.attributeValue("name");
			if (ceName.equals(chainName)) {
				if (!FlowBus.containChain(chainName)) {
					parseOneChain(ce);
				}
				return true;
			}
		}
		return false;
	}
}
