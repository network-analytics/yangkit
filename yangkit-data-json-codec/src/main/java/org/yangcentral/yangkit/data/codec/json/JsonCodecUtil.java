package org.yangcentral.yangkit.data.codec.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.yangcentral.yangkit.common.api.FName;
import org.yangcentral.yangkit.common.api.QName;
import org.yangcentral.yangkit.common.api.exception.ErrorMessage;
import org.yangcentral.yangkit.common.api.exception.ErrorTag;
import org.yangcentral.yangkit.common.api.validate.ValidatorRecord;
import org.yangcentral.yangkit.common.api.validate.ValidatorRecordBuilder;
import org.yangcentral.yangkit.common.api.validate.ValidatorResult;
import org.yangcentral.yangkit.common.api.validate.ValidatorResultBuilder;
import org.yangcentral.yangkit.common.impl.validate.ValidatorRecordImpl;
import org.yangcentral.yangkit.data.api.exception.YangDataException;
import org.yangcentral.yangkit.data.api.model.YangData;
import org.yangcentral.yangkit.data.api.model.YangDataContainer;
import org.yangcentral.yangkit.data.api.model.YangDataDocument;
import org.yangcentral.yangkit.data.api.operation.YangDataOperator;
import org.yangcentral.yangkit.data.impl.operation.YangDataOperatorImpl;
import org.yangcentral.yangkit.model.api.schema.YangSchemaContext;
import org.yangcentral.yangkit.model.api.stmt.*;
import org.yangcentral.yangkit.model.api.stmt.Module;

import java.net.URI;
import java.util.*;

public class JsonCodecUtil {
    public static String generateJsonPathArgumentFromJson(JsonNode jsonNode, String valueSearched) {
        if (jsonNode.isValueNode() && !jsonNode.asText().equals(valueSearched)) {
            return null;
        } else {
            if (jsonNode.isContainerNode()) {
                if (jsonNode.isObject()) {
                    Iterator<Map.Entry<String, JsonNode>> elements = jsonNode.fields();
                    while (elements.hasNext()) {
                        Map.Entry<String, JsonNode> element = elements.next();
                        String res =  generateJsonPathArgumentFromJson(element.getValue(), valueSearched);
                        if (res != null) {
                            return "." + element.getKey() + res;
                        }
                    }
                } else {
                    int i = 0;
                    Iterator<JsonNode> elements = jsonNode.elements();
                    while (elements.hasNext()) {
                        JsonNode element = elements.next();
                        String res = generateJsonPathArgumentFromJson(element, valueSearched);
                        if (res != null) {
                            return "(" + i + ")" + res;
                        }
                        i++;
                    }
                }
            }
        }
        return "";
    }

    public static String getJsonPath(JsonNode jsonNode) {
        return "";
    }

    public static JsonNode convertValidatorResultToJson(ValidatorResult validatorResult){
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode newJsonNode = mapper.createObjectNode();
        if(validatorResult.getRecords() == null){
            return newJsonNode;
        }
        for(ValidatorRecord record: validatorResult.getRecords()){
            ObjectNode nodeRecord = mapper.createObjectNode();
            if(record.getBadElement() != null){
                nodeRecord.put("value", record.getBadElement().toString());
            }
            if(record.getErrorMsg() != null){
                nodeRecord.put("error-msg", record.getErrorMsg().getMessage());
            }
            nodeRecord.put("severity", record.getSeverity().toString());
            nodeRecord.put("error-tag",  record.getErrorTag().getName());
            newJsonNode.set(record.getErrorPath().toString(), nodeRecord);
        }
        return newJsonNode;
    }

    public static QName getQNameFromJsonField(String jsonField, YangDataContainer parent){
        FName fName = new FName(jsonField);
        String moduleName = fName.getPrefix();
        URI ns = null;
        if(moduleName == null) {
            if(parent instanceof YangDataDocument){
                YangDataDocument yangDataDocument = (YangDataDocument) parent;
                ns = yangDataDocument.getQName().getNamespace();
            } else {
                YangData<?> yangData = (YangData<?>) parent;
                ns = yangData.getQName().getNamespace();
            }
        } else {
            YangSchemaContext schemaContext = null;
            if(parent instanceof YangDataDocument){
                YangDataDocument yangDataDocument = (YangDataDocument) parent;
                schemaContext = yangDataDocument.getSchemaContext();
            } else {
                YangData<?> yangData = (YangData<?>) parent;
                schemaContext = yangData.getContext().getDocument().getSchemaContext();
            }
            Optional<Module> moduleOp = schemaContext.getLatestModule(moduleName);
            if(!moduleOp.isPresent()){
                return null;
            }
            ns = moduleOp.get().getMainModule().getNamespace().getUri();
        }

        return new QName(ns,fName.getLocalName());
    }
    public static QName getQNameFromJsonField(String jsonField, YangSchemaContext schemaContext){
        FName fName = new FName(jsonField);
        String moduleName = fName.getPrefix();
        URI ns = null;
        if(moduleName == null) {
            return null;
        }
        Optional<Module> moduleOp = schemaContext.getLatestModule(moduleName);
        if(!moduleOp.isPresent()){
            return null;
        }
        ns = moduleOp.get().getMainModule().getNamespace().getUri();

        return new QName(ns,fName.getLocalName());
    }

    public static String getJsonFieldFromQName(QName qName,YangSchemaContext schemaContext) {
        List<Module> modules = schemaContext.getModule(qName.getNamespace());
        if(modules.isEmpty()) {
            return qName.getQualifiedName();
        }
        String moduleName = modules.get(0).getMainModule().getArgStr();
        return moduleName + ":" + qName.getLocalName();
    }
    public static String getJsonFieldFromQName(QName qName,YangDataContainer parent) {
        QName parentQName = null;
        YangSchemaContext schemaContext = null;
        if(parent instanceof YangDataDocument){
            YangDataDocument yangDataDocument = (YangDataDocument) parent;
            parentQName = yangDataDocument.getQName();
            schemaContext = yangDataDocument.getSchemaContext();
        } else {
            YangData<?> yangData = (YangData<?>) parent;
            parentQName = yangData.getQName();
            schemaContext = yangData.getContext().getDocument().getSchemaContext();
        }
        if(parentQName != null && parentQName.getNamespace().equals(qName.getNamespace())){
            return qName.getLocalName();
        }
        List<Module> modules = schemaContext.getModule(qName.getNamespace());
        if(modules.isEmpty()) {
            return qName.getQualifiedName();
        }
        String moduleName = modules.get(0).getMainModule().getArgStr();
        return moduleName + ":" + qName.getLocalName();
    }
    public static  void serializeChildren(ObjectNode element, YangDataContainer yangDataContainer) {
        ObjectMapper mapper = new ObjectMapper();
        List<YangData<?>> children = yangDataContainer.getDataChildren();
        if (null == children) {
            return;
        }
        for (YangData child : children) {
            if (null == child || child.isDummyNode()) {
                continue;
            }
            String fieldName = JsonCodecUtil.getJsonFieldFromQName(child.getQName(),yangDataContainer);
            JsonNode childElement = YangDataJsonCodec
                    .getInstance(child.getSchemaNode())
                    .serialize(child);
            if((child.getSchemaNode() instanceof YangList)
                    || (child.getSchemaNode() instanceof LeafList)) {
                ArrayNode arrayNode = (ArrayNode) element.get(fieldName);
                if(arrayNode == null) {
                    arrayNode = mapper.createArrayNode();
                    element.put(fieldName,arrayNode);
                }
                arrayNode.add(childElement);
            } else {
                element.put(fieldName, childElement);
            }
        }
    }
/*
    public static void processAttribute(String key, JsonNode attributeValue, YangDataContainer yangDataContainer) {
        List<Attribute> attributeList = new ArrayList<>();
        if (key.equals("@")) {
            attributeList = getAttributeList(attributeValue);
            if (attributeList.size() > 0) {
                for (Attribute attribute : attributeList) {
                    ((YangDataEntity) yangDataContainer).addAttribute(attribute);
                }
            }
        } else {
            String ckey = key.substring(1);
            QName qName = JsonCodecUtil.getQNameFromJsonField(ckey,yangDataContainer);
            attributeList = getAttributeList(attributeValue);
            if (yangDataContainer.getDataChildren(qName) == null) {
                attributeCache.put(qName, attributeList);
            }
            if (attributeList.size() > 0) {

                List<YangData<?>> children = yangDataContainer.getDataChildren(qName);
                for (int i = 0; i < children.size(); i++) {
                    children.get(i).addAttribute(attributeList.get(i));
                }
            }
        }
    }

    public static void jsonObjectAddAttribute(JsonNode attributeValue, List<Attribute> list) {
        Iterator<Map.Entry<String, JsonNode>> childEntries = attributeValue.fields();
        while (childEntries.hasNext()) {
            Map.Entry<String, JsonNode> childEntry = childEntries.next();
            String childEntryKey = childEntry.getKey();
            QName qName = JsonCodecUtil.getQNameFromJsonField(childEntryKey,yangSchemaContext);
            String attr = childEntry.getValue().asText();
            Attribute attribute = new Attribute(qName, attr);
            list.add(attribute);
        }

    }


    public static List<Attribute> getAttributeList(JsonNode attributeValue) {
        List<Attribute> attributeList = new ArrayList<>();
        if (attributeValue.isObject()) {
            jsonObjectAddAttribute(attributeValue, attributeList);
        } else if (attributeValue.isArray()) {
            ArrayNode attributeArray = (ArrayNode) attributeValue;
            for (int i = 0; i < attributeArray.size(); i++) {
                JsonNode childElement = attributeArray.get(i);
                if (childElement.isNull()) {
                    continue;
                } else if (childElement.isObject()) {
                    ObjectNode attributeObject = (ObjectNode) childElement;
                    jsonObjectAddAttribute(attributeObject, attributeList);
                }
            }
        }
        return attributeList;
    }

 */
    public static ValidatorResult buildChildData(YangDataContainer yangDataContainer, JsonNode child, SchemaNode childSchemaNode){
        return buildChildData(yangDataContainer, child, childSchemaNode, new ExtraValidationDataJsonCodec());
    }

    public static ValidatorResult buildChildData(YangDataContainer yangDataContainer, JsonNode child, SchemaNode childSchemaNode, ExtraValidationDataJsonCodec extraValidationData){
        ValidatorResultBuilder validatorResultBuilder = new ValidatorResultBuilder();
        if((childSchemaNode instanceof YangList) || (childSchemaNode instanceof LeafList)) {
            if(!child.isArray() && !extraValidationData.isNodeInJsonArray(child)){
                ValidatorRecordBuilder<String, JsonNode> recordBuilder = new ValidatorRecordBuilder<>();
                recordBuilder.setErrorTag(ErrorTag.BAD_ELEMENT);
                recordBuilder.setErrorPath(extraValidationData.getJsonPath(child));
                recordBuilder.setBadElement(child);
                recordBuilder.setErrorMessage(new ErrorMessage("bad element: expected an array and get an element."));
                validatorResultBuilder.addRecord(recordBuilder.build());
                return validatorResultBuilder.build();
            }
        }
        if(child.isArray() && !child.toString().equals("[null]")) {
            if((childSchemaNode instanceof YangList) || (childSchemaNode instanceof LeafList)) {
                int size = child.size();
                for (int i =0;i < size;i++) {
                    JsonNode childElement = child.get(i);
                    extraValidationData.addJsonChildArray(childElement);
                    extraValidationData.addJsonChild(child, childElement, Integer.toString(i));
                    validatorResultBuilder.merge(buildChildData(yangDataContainer,childElement,childSchemaNode, extraValidationData));
                }
            } else {
                ValidatorRecordBuilder<String, JsonNode> recordBuilder = new ValidatorRecordBuilder<>();
                recordBuilder.setErrorTag(ErrorTag.BAD_ELEMENT);
                recordBuilder.setErrorPath(extraValidationData.getJsonPath(child));
                recordBuilder.setBadElement(child);
                recordBuilder.setErrorMessage(new ErrorMessage(
                        "bad element:" + child.toString()));
                validatorResultBuilder.addRecord(recordBuilder.build());
            }
            return validatorResultBuilder.build();
        }
        YangDataJsonCodec sonCodec = YangDataJsonCodec.getInstance(childSchemaNode);
        YangData<?> sonData = sonCodec.deserialize(child, validatorResultBuilder);
        if (null == sonData) {
            return validatorResultBuilder.build();
        }
        try {
            YangData<?> oldData = yangDataContainer.getDataChild(sonData.getIdentifier());
            if (oldData != null) {
                YangDataOperator dataOperator = new YangDataOperatorImpl(yangDataContainer);
                dataOperator.merge((YangData<? extends DataNode>) sonData, false);
            } else {
                yangDataContainer.addDataChild(sonData, false);
            }

        } catch (YangDataException e) {
            ValidatorRecordBuilder<String, JsonNode> recordBuilder = new ValidatorRecordBuilder<>();
            recordBuilder.setErrorTag(e.getErrorTag());
            recordBuilder.setErrorPath(child.toString());
            recordBuilder.setBadElement(child);
            recordBuilder.setErrorMessage(e.getErrorMsg());
            validatorResultBuilder.addRecord(recordBuilder.build());
            return validatorResultBuilder.build();
        }
        sonData = yangDataContainer.getDataChild(sonData.getIdentifier());
        if (sonData instanceof YangDataContainer) {
            validatorResultBuilder.merge(buildChildrenData((YangDataContainer) sonData, child, extraValidationData));
        }
        return validatorResultBuilder.build();
    }

    public static ValidatorResult buildChildrenData(YangDataContainer yangDataContainer, JsonNode element) {
        ExtraValidationDataJsonCodec extraValidationData = new ExtraValidationDataJsonCodec();
        ValidatorResult validatorResult = buildChildrenData(yangDataContainer, element, extraValidationData);
        if(validatorResult.getRecords() == null){
            return validatorResult;
        }
        ValidatorResultBuilder validatorResultBuilderWithErrorPath = new ValidatorResultBuilder();
        for(ValidatorRecord record : validatorResult.getRecords()){
            ValidatorRecordBuilder<String, JsonNode> recordBuilder = new ValidatorRecordBuilder<>();
            JsonNode tempJsonNode = (JsonNode) record.getBadElement();
            recordBuilder.setErrorTag(record.getErrorTag());
            recordBuilder.setErrorPath(record.getErrorPath() != null ? record.getErrorPath().toString() : extraValidationData.getJsonPath(tempJsonNode));
            recordBuilder.setBadElement(tempJsonNode);
            recordBuilder.setErrorMessage(record.getErrorMsg());
            validatorResultBuilderWithErrorPath.addRecord(recordBuilder.build());
        }
        return validatorResultBuilderWithErrorPath.build();
    }
    public static ValidatorResult buildChildrenData(YangDataContainer yangDataContainer, JsonNode element, ExtraValidationDataJsonCodec extraValidationData) {
        ValidatorResultBuilder validatorResultBuilder = new ValidatorResultBuilder();
        SchemaNodeContainer schemaNodeContainer = null;
        if (yangDataContainer instanceof YangDataDocument) {
            schemaNodeContainer = ((YangDataDocument) yangDataContainer).getSchemaContext();
        } else {
            YangData<?> yangData = (YangData<?>) yangDataContainer;
            schemaNodeContainer = (SchemaNodeContainer) yangData.getSchemaNode();
        }
        Iterator<Map.Entry<String, JsonNode>> fields = element.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode child = field.getValue();
            extraValidationData.addJsonChild(element, child, fieldName);
            if (fieldName.startsWith("@")) {
               // processAttribute(fieldName, child, yangDataContainer);
                continue;
            }
            QName qName = JsonCodecUtil.getQNameFromJsonField(fieldName,yangDataContainer);
            SchemaNode sonSchemaNode = schemaNodeContainer.getTreeNodeChild(qName);
            if (sonSchemaNode == null || !sonSchemaNode.isActive()) {
                ValidatorRecordBuilder<String, JsonNode> recordBuilder = new ValidatorRecordBuilder<>();
                recordBuilder.setErrorTag(ErrorTag.UNKNOWN_ELEMENT);
                recordBuilder.setErrorPath(extraValidationData.getJsonPath(child));
                recordBuilder.setBadElement(child);
                recordBuilder.setErrorMessage(new ErrorMessage(
                        "unrecognized element:" + child.toString()));
                validatorResultBuilder.addRecord(recordBuilder.build());
                continue;
            }

            validatorResultBuilder.merge(buildChildData(yangDataContainer,child,sonSchemaNode, extraValidationData));

        }
        return validatorResultBuilder.build();
    }
}
