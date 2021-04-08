package com.yomahub.liteflow.test.condition.cmp1;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;


@Component("d")
public class DCmp extends NodeComponent {
    @Override
    public void process() throws Exception {
        Thread.sleep(3000);
        System.out.println("Dcomp executed!");
    }
}
