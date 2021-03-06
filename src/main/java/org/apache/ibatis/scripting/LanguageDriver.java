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
package org.apache.ibatis.scripting;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.session.Configuration;

/**
 * 使用各种语言实现动态sql 接口
 */
public interface LanguageDriver {

    /**
     * 创建一个ParameterHandler对象，用于将实际参数赋值到JDBC语句中
     * Creates a ParameterHandler that passes the actual parameters to the the JDBC statement.
     *
     * @param mappedStatement The mapped statement that is being executed
     * @param parameterObject The input parameter object (can be null)
     * @param boundSql        The resulting SQL once the dynamic language has been executed.
     * @return DefaultParameterHandler
     * @author Frank D. Martinez [mnesarco]
     */
    ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql);

    /**
     * 将XML中读入的语句解析并返回一个sqlSource对象
     * Creates an  SqlSource that will hold the statement read from a mapper xml file.
     * It is called during startup, when the mapped statement is read from a class or an xml file.
     *
     * @param configuration The MyBatis configuration
     * @param script        XNode parsed from a XML file
     * @param parameterType input parameter type got from a mapper method or specified in the parameterType xml attribute. Can be null.
     * @return SqlSource
     */
    SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType);

    /**
     * 将注解中读入的语句解析并返回一个sqlSource对象
     * Creates an SqlSource that will hold the statement read from an annotation.
     * It is called during startup, when the mapped statement is read from a class or an xml file.
     *
     * @param configuration The MyBatis configuration
     * @param script        The content of the annotation
     * @param parameterType input parameter type got from a mapper method or specified in the parameterType xml attribute. Can be null.
     * @return SqlSource
     */
    SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType);

}
