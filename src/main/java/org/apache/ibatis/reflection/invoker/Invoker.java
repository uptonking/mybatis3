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
package org.apache.ibatis.reflection.invoker;

import java.lang.reflect.InvocationTargetException;

/**
 * 基于反射调用对象的方法 接口
 * 用于封装getter/setter对应的Method对象，以及字段对应的Field对象
 *
 * @author Clinton Begin
 */
public interface Invoker {

    Object invoke(Object target, Object[] args) throws IllegalAccessException, InvocationTargetException;

    // 返回属性对应的类型
    Class<?> getType();

}
