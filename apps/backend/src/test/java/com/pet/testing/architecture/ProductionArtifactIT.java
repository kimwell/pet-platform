package com.pet.testing.architecture;

import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductionArtifactIT {
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
