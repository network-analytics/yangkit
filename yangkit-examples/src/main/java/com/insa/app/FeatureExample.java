/*
 * Copyright 2023 INSA Lyon.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.insa.app;

import org.dom4j.DocumentException;
import org.yangcentral.yangkit.common.api.exception.Severity;
import org.yangcentral.yangkit.common.api.validate.ValidatorRecord;
import org.yangcentral.yangkit.common.api.validate.ValidatorResult;
import org.yangcentral.yangkit.common.api.validate.ValidatorResultBuilder;
import org.yangcentral.yangkit.data.api.model.ContainerData;
import org.yangcentral.yangkit.data.api.model.YangDataDocument;
import org.yangcentral.yangkit.data.codec.json.ContainerDataJsonCodec;
import org.yangcentral.yangkit.data.codec.json.YangDataDocumentJsonParser;
import org.yangcentral.yangkit.model.api.schema.YangSchema;
import org.yangcentral.yangkit.model.api.schema.YangSchemaContext;
import org.yangcentral.yangkit.model.api.schema.ModuleSet;
import org.yangcentral.yangkit.model.api.schema.YangModuleDescription;
import org.yangcentral.yangkit.model.api.stmt.Container;
import org.yangcentral.yangkit.model.api.stmt.DataDefinition;
import org.yangcentral.yangkit.model.api.stmt.Feature;
import org.yangcentral.yangkit.model.api.stmt.Module;
import org.yangcentral.yangkit.model.api.stmt.SchemaNode;
import org.yangcentral.yangkit.parser.YangParserException;
import org.yangcentral.yangkit.parser.YangYinParser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;

/**
 * Simple example demonstrating YANG feature concepts with Yangkit
 */
public class FeatureExample {

    public static void main(String[] args) throws IOException, YangParserException, DocumentException {

        System.out.println("=== Simple YANG Feature Example ===\n");

        // Parse our example YANG module with features
        URL yangUrl = FeatureExample.class.getClassLoader().getResource("FeatureExample/yang");
        String yangDir = yangUrl.getFile();

        YangSchemaContext schemaContext = YangYinParser.parse(yangDir);
        ValidatorResult result = schemaContext.validate();

        if (!result.isOk()) {
            System.out.println("Schema validation failed:");
            for (ValidatorRecord<?, ?> record : result.getRecords()) {
                System.out.println("  Error: " + record.getErrorMsg().getMessage());
            }
            return;
        }

        // Find our example module
        Optional<Module> moduleOpt = schemaContext.getModule("example-features", null);
        if (!moduleOpt.isPresent()) {
            System.out.println("example-features module not found");
            return;
        }

        Module exampleModule = moduleOpt.get();
        boolean valid = false;

        // Test 1: Enable no features
        System.out.println("1. With no features enabled:");
        YangSchema noFeatureSchema = createSchemaWithFeature(exampleModule, null);
        schemaContext.setYangSchema(noFeatureSchema);
        showElementActivity(exampleModule);
        valid = validateJson("basic-config.json", schemaContext);
        System.out.println("Validation result: " + (valid ? "valid" : "invalid"));

        // Test 2: Enable advanced-config feature
        System.out.println("\n2. With 'advanced-config' feature enabled:");
        YangSchema advancedSchema = createSchemaWithFeature(exampleModule, "advanced-config");
        schemaContext.setYangSchema(advancedSchema);
        showElementActivity(exampleModule);
        valid = validateJson("advanced-config.json", schemaContext);
        System.out.println("Validation result: " + (valid ? "valid" : "invalid"));

        // Test 3: Enable monitoring feature
        System.out.println("\n3. With 'monitoring' feature enabled:");
        YangSchema monitoringSchema = createSchemaWithFeature(exampleModule, "monitoring");
        schemaContext.setYangSchema(monitoringSchema);
        showElementActivity(exampleModule);
        valid = validateJson("monitoring-config.json", schemaContext);
        System.out.println("Validation result: " + (valid ? "valid" : "invalid"));

        // Test 4: All features enabled
        System.out.println("\n4. With all features enabled:");
        // Apparently, if no schema is specified, all features are enabled by default.
        // This is the same in Libyang.
        schemaContext.setYangSchema(null);
        showElementActivity(exampleModule);
        valid = validateJson("all-features.json", schemaContext);
        System.out.println("Validation result: " + (valid ? "valid" : "invalid"));

    }

    private static void showElementActivity(Module module) {
        for (DataDefinition dataDefinition : module.getDataDefChildren()) {
            showElementStatus(dataDefinition, "  ");
        }
    }

    private static void showElementStatus(DataDefinition element, String indent) {
        if (element instanceof SchemaNode) {
            SchemaNode schemaNode = (SchemaNode) element;
            boolean isActive = schemaNode.isActive();
            String status = isActive ? "✅" : "🚫";
            System.out.println(indent + element.getArgStr() + " - " + status);
        }

        // Recursively show child elements
        if (element instanceof org.yangcentral.yangkit.model.api.stmt.DataDefContainer) {
            var container = (org.yangcentral.yangkit.model.api.stmt.DataDefContainer) element;
            for (DataDefinition child : container.getDataDefChildren()) {
                showElementStatus(child, indent + "  ");
            }
        }
    }

    private static YangSchema createSchemaWithFeature(Module module, String featureName) {
        return createSchemaWithFeatures(module, featureName);
    }

    private static YangSchema createSchemaWithFeatures(Module module, String... featureNames) {
        YangSchema schema = new YangSchema();
        schema.setName("test-schema");

        ModuleSet moduleSet = new ModuleSet();
        moduleSet.setName("test-module-set");

        YangModuleDescription moduleDesc = new YangModuleDescription(module.getModuleId());

        for (String featureName : featureNames) {
            moduleDesc.addFeature(featureName);
        }

        moduleSet.addModule(moduleDesc);
        schema.addModuleSet(moduleSet);

        return schema;
    }

    private static boolean validateJson(String jsonFile, YangSchemaContext schemaContext) throws IOException {
        // My validation
        JsonNode jsonNode = new ObjectMapper().readTree(new File(
                FeatureExample.class.getClassLoader().getResource("FeatureExample/json/" + jsonFile).getFile()));
        ValidatorResultBuilder validatorResultBuilder = new ValidatorResultBuilder();
        YangDataDocument doc = new YangDataDocumentJsonParser(schemaContext).parse(jsonNode, validatorResultBuilder);

        ValidatorResult validatorResult = validatorResultBuilder.build();
        if (!validatorResult.isOk()) {
            System.out.println("Built-in print: " + validatorResult.print(Severity.ERROR));
        }
        doc.update();

        ValidatorResult validatorResult1 = doc.validate();
        if (!validatorResult1.isOk()) {
            System.out.println("Built-in print: " + validatorResult1.print(Severity.ERROR));
        }

        return validatorResult.isOk() && validatorResult1.isOk();
    }

}
