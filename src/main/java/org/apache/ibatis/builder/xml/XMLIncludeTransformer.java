/**
 * Copyright ${license.git.copyrightYears} the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ibatis.builder.xml;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.parsing.PropertyParser;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.session.Configuration;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * xml配置中的<include>标签解析
 *
 * @author Frank D. Martinez [mnesarco]
 */
public class XMLIncludeTransformer {

    private final Configuration configuration;
    private final MapperBuilderAssistant builderAssistant;

    public XMLIncludeTransformer(Configuration configuration, MapperBuilderAssistant builderAssistant) {
        this.configuration = configuration;
        this.builderAssistant = builderAssistant;
    }

    public void applyIncludes(Node source) {
        Properties variablesContext = new Properties();
        Properties configurationVariables = configuration.getVariables();
        if (configurationVariables != null) {
            variablesContext.putAll(configurationVariables);
        }
        applyIncludes(source, variablesContext, false);
    }

    /**
     * 递归解析所有include的sql
     * Recursively apply includes through all SQL fragments.
     *
     * @param source           Include node in DOM tree
     * @param variablesContext Current context for static variables with values
     */
    private void applyIncludes(Node source, final Properties variablesContext, boolean included) {

        if (source.getNodeName().equals("include")) {

            // 查找refid属性指向的sql节点，返回的是其深克隆的Node对象
            Node toInclude = findSqlFragment(getStringAttribute(source, "refid"), variablesContext);

            // 解析include节点下的property节点，将得到的键值对添加到variablesContext，并形成新的Properties对象返回，用于替换占位符
            Properties toIncludeContext = getVariablesContext(source, variablesContext);

            // 递归处理include节点，在<sql>节点中可能会使用<include>引用了其他sql片段
            applyIncludes(toInclude, toIncludeContext, true);

            if (toInclude.getOwnerDocument() != source.getOwnerDocument()) {
                toInclude = source.getOwnerDocument().importNode(toInclude, true);
            }

            // 将<include>节点替换成<sql>节点
            source.getParentNode().replaceChild(toInclude, source);

            /// 将<sql>节点的子节点添加到<sql>节点前面
            while (toInclude.hasChildNodes()) {
                toInclude.getParentNode().insertBefore(toInclude.getFirstChild(), toInclude);
            }

            // 删除<sql>节点
            toInclude.getParentNode().removeChild(toInclude);
        } else if (source.getNodeType() == Node.ELEMENT_NODE) {
            // 遍历当前SQL语句的子节点
            NodeList children = source.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                applyIncludes(children.item(i), variablesContext, included);
            }
        } else if (included && source.getNodeType() == Node.TEXT_NODE
                && !variablesContext.isEmpty()) {
            // 使用之前解析得到的 Properties 对象替换对应的占位符. replace variables ins all text nodes
            source.setNodeValue(PropertyParser.parse(source.getNodeValue(), variablesContext));
        }
    }

    private Node findSqlFragment(String refid, Properties variables) {
        refid = PropertyParser.parse(refid, variables);
        refid = builderAssistant.applyCurrentNamespace(refid, true);
        try {
            XNode nodeToInclude = configuration.getSqlFragments().get(refid);
            return nodeToInclude.getNode().cloneNode(true);
        } catch (IllegalArgumentException e) {
            throw new IncompleteElementException("Could not find SQL statement to include with refid '" + refid + "'", e);
        }
    }

    private String getStringAttribute(Node node, String name) {
        return node.getAttributes().getNamedItem(name).getNodeValue();
    }

    /**
     * Read placholders and their values from include node definition.
     *
     * @param node                      Include node instance
     * @param inheritedVariablesContext Current context used for replace variables in new variables values
     * @return variables context from include instance (no inherited values)
     */
    private Properties getVariablesContext(Node node, Properties inheritedVariablesContext) {
        Map<String, String> declaredProperties = null;
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                String name = getStringAttribute(n, "name");
                // Replace variables inside
                String value = PropertyParser.parse(getStringAttribute(n, "value"), inheritedVariablesContext);
                if (declaredProperties == null) {
                    declaredProperties = new HashMap<String, String>();
                }
                if (declaredProperties.put(name, value) != null) {
                    throw new BuilderException("Variable " + name + " defined twice in the same include definition");
                }
            }
        }
        if (declaredProperties == null) {
            return inheritedVariablesContext;
        } else {
            Properties newProperties = new Properties();
            newProperties.putAll(inheritedVariablesContext);
            newProperties.putAll(declaredProperties);
            return newProperties;
        }
    }
}
