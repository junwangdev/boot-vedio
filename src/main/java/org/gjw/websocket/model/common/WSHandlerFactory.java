package org.gjw.websocket.model.common;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.extra.spring.SpringUtil;
import org.gjw.websocket.model.interfaces.AbstractWSHandler;
import org.gjw.websocket.model.interfaces.WSMessageAnalyzer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author guojunwang
 * Description
 * Date 2023/4/2 11:17
 */
@Component
public class WSHandlerFactory implements CommandLineRunner {

    private static Map<Class<? extends AbstractWSHandler>, List<WSMessageAnalyzer>> analyzerMap = new ConcurrentHashMap<>(64);

    @Override
    public void run(String... args) throws Exception {
        Map<String, WSMessageAnalyzer> analyzerBeanMap = SpringUtil.getBeansOfType(WSMessageAnalyzer.class);

        if(CollUtil.isNotEmpty(analyzerBeanMap)){
            Map<? extends Class<? extends AbstractWSHandler>, List<WSMessageAnalyzer>> analyzerMap = analyzerBeanMap.values().stream()
                    .filter(analyzer -> Objects.nonNull(analyzer.properties()))
                    .collect(Collectors.groupingBy(analyzer -> analyzer.properties().getWebSocketHandler()));

            WSHandlerFactory.analyzerMap.putAll(analyzerMap);
        }
    }

    public static List<WSMessageAnalyzer> getAnalyzerList(Class<? extends AbstractWSHandler> handlerClass){
        return analyzerMap.get(handlerClass);
    }

}
