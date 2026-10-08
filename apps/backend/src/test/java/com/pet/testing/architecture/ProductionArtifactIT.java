package com.pet.testing.architecture;

import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductionArtifactIT {
    @Test void customerProductionClassesContainNoTechnicalWechatCredentialsOrProbe() throws Exception {
        try(var jar=new JarFile("target/pet-platform-backend-0.0.0-SNAPSHOT.jar")){
            for(var entry:jar.stream().filter(e->e.getName().startsWith("BOOT-INF/classes/") && !e.isDirectory()).toList()) {
                String raw=new String(jar.getInputStream(entry).readAllBytes(),java.nio.charset.StandardCharsets.ISO_8859_1);
                for(String fixture:List.of("OnlyTestSecretInput","OnlyTestCode","OnlyTestOpenId","OnlyTestSessionKey","__customer-test","testWechatGateway","RESPONSES")) assertFalse(raw.contains(fixture),entry.getName());
            }
        }
    }
    @Test void packagedJarContainsNoTestClassResourceMigrationOrDependency() throws Exception {
        var tests = StructureRulesTest.classes(Path.of("target/test-classes"));
        var resources = new HashSet<String>();
        try (var paths = Files.walk(Path.of("src/test/resources"))) {
            paths.filter(Files::isRegularFile).forEach(p -> resources.add(Path.of("src/test/resources").relativize(p).toString()));
        }
        try (var jar = new JarFile("target/pet-platform-backend-0.0.0-SNAPSHOT.jar")) {
            var entries = new HashSet<String>();
            jar.stream().forEach(e -> entries.add(e.getName()));
            assertEquals(List.of(), StructureRulesTest.artifactViolations(entries, tests, resources));
        }
    }
}
