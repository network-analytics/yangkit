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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dom4j.DocumentException;
import org.yangcentral.yangkit.common.api.exception.Severity;
import org.yangcentral.yangkit.common.api.validate.ValidatorRecord;
import org.yangcentral.yangkit.common.api.validate.ValidatorResult;
import org.yangcentral.yangkit.common.api.validate.ValidatorResultBuilder;
import org.yangcentral.yangkit.data.api.model.YangDataDocument;
import org.yangcentral.yangkit.data.codec.json.YangDataDocumentJsonParser;
import org.yangcentral.yangkit.model.api.schema.YangSchemaContext;
import org.yangcentral.yangkit.model.api.stmt.Module;
import org.yangcentral.yangkit.parser.YangParserException;
import org.yangcentral.yangkit.parser.YangYinParser;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Usecase on how to use yang to validate YANG-push notification
 *
 */
public class App7 {
    public static void main(String[] args) throws IOException, YangParserException, DocumentException {

        // Parsing YANGs
        YangSchemaContext schemaContext = YangYinParser.parse(App7.class.getClassLoader().getResource("App7/yang").getFile());
        ValidatorResult result = schemaContext.validate();
        System.out.println("Schema context is valid : " + result.isOk() + "; " + schemaContext.getModules().size());
        for (ValidatorRecord<?, ?> record : result.getRecords()) {
            if (record.getSeverity() == Severity.ERROR) {
                System.out.println(record.getErrorMsg().getMessage() + " - " + record.getBadElement());
            }
        }

        // Parsing JSON
        JsonNode jsonNode = new ObjectMapper().readTree(new File(App7.class.getClassLoader().getResource("App7/msg.json").getFile()));
        ValidatorResultBuilder validatorResultBuilder = new ValidatorResultBuilder();
        YangDataDocument doc = new YangDataDocumentJsonParser(schemaContext).parse(jsonNode, validatorResultBuilder);
        doc.update();
        ValidatorResult validatorResult = validatorResultBuilder.build();
        System.out.println("Is JSON valid? " + validatorResult.isOk());
        for (ValidatorRecord<?, ?> record : validatorResult.getRecords()) {
            System.out.println(record.getSeverity() + ":" + record.getErrorMsg().getMessage() + " - " + record.getBadElement());
        }

        ValidatorResult validatorResult1 = doc.validate();
        System.out.println("Is JSON valid? " + validatorResult1.isOk());
        for (ValidatorRecord<?, ?> record : validatorResult1.getRecords()) {
            System.out.println(record.getSeverity() + ":" + record.getErrorMsg().getMessage());
        }
    }
}
