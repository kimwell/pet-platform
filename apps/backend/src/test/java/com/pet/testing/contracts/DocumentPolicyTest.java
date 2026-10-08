package com.pet.testing.contracts;

import com.pet.platform.shared.openapi.OpenApiConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class DocumentPolicyTest {
    // 与运行时相同的启动门禁，生产不能通过外部覆盖偷偷打开文档。
    @Test void environmentCannotEnableProductionDocumentsOrPublicBind() throws Exception {
        var method = OpenApiConfiguration.class.getDeclaredMethod("documentPolicy", org.springframework.core.env.Environment.class);
        method.setAccessible(true);
        for (var environment : java.util.List.of(
                new MockEnvironment().withProperty("pet.environment", "prod").withProperty("springdoc.api-docs.enabled", "true"),
                new MockEnvironment().withProperty("pet.environment", "prod").withProperty("springdoc.swagger-ui.enabled", "true"),
                new MockEnvironment().withProperty("pet.environment", "local").withProperty("springdoc.api-docs.enabled", "true").withProperty("server.address", "0.0.0.0"))) {
            var check = (org.springframework.beans.factory.config.BeanFactoryPostProcessor) method.invoke(null, environment);
            assertThrows(IllegalStateException.class, () -> check.postProcessBeanFactory(new org.springframework.beans.factory.support.DefaultListableBeanFactory()));
        }
    }
}
