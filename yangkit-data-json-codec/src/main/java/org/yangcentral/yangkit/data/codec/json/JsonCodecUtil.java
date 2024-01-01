package org.yangcentral.yangkit.data.codec.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.yangcentral.yangkit.common.api.Attribute;
import org.yangcentral.yangkit.common.api.FName;
import org.yangcentral.yangkit.common.api.QName;
import org.yangcentral.yangkit.common.api.exception.ErrorMessage;
import org.yangcentral.yangkit.common.api.exception.ErrorTag;
import org.yangcentral.yangkit.common.api.validate.ValidatorRecord;
import org.yangcentral.yangkit.common.api.validate.ValidatorRecordBuilder;
import org.yangcentral.yangkit.common.api.validate.ValidatorResult;
import org.yangcentral.yangkit.common.api.validate.ValidatorResultBuilder;
import org.yangcentral.yangkit.data.api.exception.YangDataException;
import org.yangcentral.yangkit.data.api.model.YangData;
import org.yangcentral.yangkit.data.api.model.YangDataContainer;
import org.yangcentral.yangkit.data.api.model.YangDataDocument;
import org.yangcentral.yangkit.data.api.model.YangDataEntity;
import org.yangcentral.yangkit.data.api.operation.YangDataOperator;
import org.yangcentral.yangkit.data.impl.operation.YangDataOperatorImpl;
import org.yangcentral.yangkit.model.api.restriction.Empty;
import org.yangcentral.yangkit.model.api.schema.YangSchemaContext;
import org.yangcentral.yangkit.model.api.stmt.*;
import org.yangcentral.yangkit.model.api.stmt.Module;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.text.ParseException;
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

    public static Map<JsonNode, String> jsonPath = new HashMap<>();
    public static Map<JsonNode, JsonNode> jsonNodeParent = new HashMap<>();
    public static String getJsonPath(JsonNode jsonNode) {
        StringBuilder path = new StringBuilder();
        JsonNode parent = jsonNodeParent.get(jsonNode);
        while(parent != null){
            path.insert(0, jsonPath.get(jsonNode) + "/");
            jsonNode = parent;
            parent = jsonNodeParent.get(jsonNode);
        }
        path.insert(0, "/");
        path.deleteCharAt(path.length() - 1);
        return path.toString();
    }

    public static JsonNode mergeJsonValidatorResult(JsonNode jsonNode, ValidatorResult validatorResult){
        JsonNode newJsonNode = jsonNode.deepCopy();
        ObjectMapper mapper = new ObjectMapper();
        for(ValidatorRecord record: validatorResult.getRecords()){
            ObjectNode temp = mapper.createObjectNode();
            String path = record.getErrorPath().toString();
            String container = path.substring(0,path.lastIndexOf("/"));
            String key = path.substring(path.lastIndexOf("/")+1);
            JsonNode valueNode = newJsonNode.at(path);
            String valueTxt = valueNode.textValue();
            if(!valueNode.isTextual()){
                valueTxt = valueNode.toString();
            }
            temp.put("value", valueTxt);
            if(record.getErrorMsg() != null){
                temp.put("error-msg", record.getErrorMsg().getMessage());
            }
            temp.put("error-tag", record.getErrorTag().getName());
            JsonNode jsonContainer = newJsonNode.at(container);
            if(jsonContainer instanceof ArrayNode){
                ((ArrayNode) jsonContainer).set(Integer.parseInt(key), temp);
            }else{
                ((ObjectNode)(jsonContainer)).set(key, temp);
            }
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
        ValidatorResultBuilder validatorResultBuilder = new ValidatorResultBuilder();

        boolean doArrayValidation = true;
        if(childSchemaNode instanceof Container){
            doArrayValidation = false;
        }
        else if(childSchemaNode instanceof Leaf){
            Leaf leaf = (Leaf)childSchemaNode;
            doArrayValidation = leaf.getType() instanceof Empty;
        }

        if(child.isArray() && doArrayValidation) {
            if((childSchemaNode instanceof YangList) || (childSchemaNode instanceof LeafList)) {
                int size = child.size();
                for (int i =0;i < size;i++) {
                    JsonNode childElement = child.get(i);
                    JsonCodecUtil.jsonNodeParent.put(childElement,child);
                    JsonCodecUtil.jsonPath.put(childElement, Integer.toString(i));
                    validatorResultBuilder.merge(buildChildData(yangDataContainer,childElement,childSchemaNode));
                }
            } else {
                ValidatorRecordBuilder<String, JsonNode> recordBuilder = new ValidatorRecordBuilder<>();
                recordBuilder.setErrorTag(ErrorTag.BAD_ELEMENT);
                recordBuilder.setErrorPath(JsonCodecUtil.getJsonPath(child));
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
            validatorResultBuilder.merge(buildChildrenData((YangDataContainer) sonData, child));
        }
        return validatorResultBuilder.build();
    }

    public static ValidatorResult buildChildrenData(YangDataContainer yangDataContainer, JsonNode element) {
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
            JsonCodecUtil.jsonPath.put(child, fieldName);
            JsonCodecUtil.jsonNodeParent.put(child, element);
            if (fieldName.startsWith("@")) {
               // processAttribute(fieldName, child, yangDataContainer);
                continue;
            }
            QName qName = JsonCodecUtil.getQNameFromJsonField(fieldName,yangDataContainer);
            SchemaNode sonSchemaNode = schemaNodeContainer.getTreeNodeChild(qName);
            if (sonSchemaNode == null || !sonSchemaNode.isActive()) {
                ValidatorRecordBuilder<String, JsonNode> recordBuilder = new ValidatorRecordBuilder<>();
                recordBuilder.setErrorTag(ErrorTag.UNKNOWN_ELEMENT);
                recordBuilder.setErrorPath(JsonCodecUtil.getJsonPath(child));
                recordBuilder.setBadElement(child);
                recordBuilder.setErrorMessage(new ErrorMessage(
                        "unrecognized element:" + child.toString()));
                validatorResultBuilder.addRecord(recordBuilder.build());
                continue;
            }

            validatorResultBuilder.merge(buildChildData(yangDataContainer,child,sonSchemaNode));

        }
        return validatorResultBuilder.build();
    }
}
