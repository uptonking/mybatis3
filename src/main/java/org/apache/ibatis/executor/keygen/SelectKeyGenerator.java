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
package org.apache.ibatis.executor.keygen;

import java.sql.Statement;
import java.util.List;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.RowBounds;

/**
 * 对于Oracle这类不支持自动生成自增主键的数据库，SelectKeyGenerator可以生成主键
 *
 * @author Clinton Begin
 * @author Jeff Butler
 */
public class SelectKeyGenerator implements KeyGenerator {

    public static final String SELECT_KEY_SUFFIX = "!selectKey";

    // 标识<selectKey>节点中定义的SQL语句是在insert语句之前执行还是之后执行
    private boolean executeBefore;

    // 该SQL语句用于获取insert语句中使用的主键
    private MappedStatement keyStatement;

    public SelectKeyGenerator(MappedStatement keyStatement, boolean executeBefore) {
        this.executeBefore = executeBefore;
        this.keyStatement = keyStatement;
    }

    @Override
    public void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
        if (executeBefore) {
            processGeneratedKeys(executor, ms, parameter);
        }
    }

    @Override
    public void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
        if (!executeBefore) {
            processGeneratedKeys(executor, ms, parameter);
        }
    }

    // 执行<selectKey>节点中配置的SQL语句，获取insert语句中用到的主键井映射成对象
    // 然后按照配置，将主键对象中对应的属性设置到用户参数中
    private void processGeneratedKeys(Executor executor, MappedStatement ms, Object parameter) {
        try {
            if (parameter != null && keyStatement != null && keyStatement.getKeyProperties() != null) {

                // 获取<selectKey>节点的keyProperties配置的属性名称，它表示主键对应的属性
                String[] keyProperties = keyStatement.getKeyProperties();
                final Configuration configuration = ms.getConfiguration();
                // 创建用户传入的实参对象对应的 MetaObject 对象
                final MetaObject metaParam = configuration.newMetaObject(parameter);

                if (keyProperties != null) {
                    // Do not close keyExecutor. The transaction will be closed by parent executor.
                    // 创建 Executor 对象，并执行 keyStatement 字段中记录的 SQL 语句 ， 并得到主键对象
                    Executor keyExecutor = configuration.newExecutor(executor.getTransaction(), ExecutorType.SIMPLE);
                    List<Object> values = keyExecutor.query(keyStatement, parameter, RowBounds.DEFAULT, Executor.NO_RESULT_HANDLER);
                    if (values.size() == 0) {
                        throw new ExecutorException("SelectKey returned no data.");
                    } else if (values.size() > 1) {
                        throw new ExecutorException("SelectKey returned more than one value.");
                    } else {

                        // 创建主键对象对应的 MetaObject 对象
                        MetaObject metaResult = configuration.newMetaObject(values.get(0));

                        if (keyProperties.length == 1) {

                            if (metaResult.hasGetter(keyProperties[0])) {
                                // 从主键对象中获取指定属性，设直到用户参数的对应属性中
                                setValue(metaParam, keyProperties[0], metaResult.getValue(keyProperties[0]));
                            } else {
                                // no getter for the property - maybe just a single value object
                                // 若主键对象不包含指定属性的getter方法，可能是一个基本类型，直接将主键对象设置到用户参数中
                                setValue(metaParam, keyProperties[0], values.get(0));
                            }

                        } else {

                            // 处理主键有多列的情况，从主键对象中取出指定属性，并设直到用户参数的对应属性中
                            handleMultipleProperties(keyProperties, metaParam, metaResult);
                        }
                    }
                }
            }
        } catch (ExecutorException e) {
            throw e;
        } catch (Exception e) {
            throw new ExecutorException("Error selecting key or setting result to parameter object. Cause: " + e, e);
        }
    }

    private void handleMultipleProperties(String[] keyProperties, MetaObject metaParam, MetaObject metaResult) {
        String[] keyColumns = keyStatement.getKeyColumns();

        if (keyColumns == null || keyColumns.length == 0) {
            // no key columns specified, just use the property names
            for (String keyProperty : keyProperties) {
                setValue(metaParam, keyProperty, metaResult.getValue(keyProperty));
            }
        } else {
            if (keyColumns.length != keyProperties.length) {
                throw new ExecutorException("If SelectKey has key columns, the number must match the number of key properties.");
            }
            for (int i = 0; i < keyProperties.length; i++) {
                setValue(metaParam, keyProperties[i], metaResult.getValue(keyColumns[i]));
            }
        }
    }

    private void setValue(MetaObject metaParam, String property, Object value) {
        if (metaParam.hasSetter(property)) {
            metaParam.setValue(property, value);
        } else {
            throw new ExecutorException("No setter found for the keyProperty '" + property + "' in " + metaParam.getOriginalObject().getClass().getName() + ".");
        }
    }
}
