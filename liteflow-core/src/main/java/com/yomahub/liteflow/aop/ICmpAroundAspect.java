/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/10/22
 */
package com.yomahub.liteflow.aop;

import com.yomahub.liteflow.entity.data.Slot;

public interface ICmpAroundAspect {

    void beforeProcess(Slot slot);

    void afterProcess(Slot slot);
}
