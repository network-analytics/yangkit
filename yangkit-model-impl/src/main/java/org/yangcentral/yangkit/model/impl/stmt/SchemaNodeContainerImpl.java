package org.yangcentral.yangkit.model.impl.stmt;

import org.yangcentral.yangkit.base.ErrorCode;
import org.yangcentral.yangkit.base.YangContext;
import org.yangcentral.yangkit.common.api.QName;
import org.yangcentral.yangkit.common.api.validate.ValidatorResult;
import org.yangcentral.yangkit.common.api.validate.ValidatorResultBuilder;
import org.yangcentral.yangkit.model.api.schema.SchemaTreeType;
import org.yangcentral.yangkit.model.api.schema.YangSchemaContext;
import org.yangcentral.yangkit.model.api.stmt.*;
import org.yangcentral.yangkit.model.api.stmt.ext.YangData;
import org.yangcentral.yangkit.model.api.stmt.ext.YangStructure;
import org.yangcentral.yangkit.util.ModelUtil;

import java.util.*;

public class SchemaNodeContainerImpl implements SchemaNodeContainer {
   private List<SchemaNode> schemaNodes = new ArrayList<>();
   private YangContext yangContext;
   private SchemaNodeContainer self;

   public SchemaNodeContainerImpl(SchemaNodeContainer self) {
      this.self = self;
   }

   public YangContext getYangContext() {
      return this.yangContext;
   }

   public void setYangContext(YangContext yangContext) {
      this.yangContext = yangContext;
   }

   public List<SchemaNode> getSchemaNodeChildren() {
      return Collections.unmodifiableList(this.schemaNodes);
   }

   public ValidatorResult addSchemaNodeChild(SchemaNode schemaNode) {
      ValidatorResultBuilder validatorResultBuilder = new ValidatorResultBuilder();
      if (!(schemaNode instanceof VirtualSchemaNode)) {
         SchemaNode child = this.getTreeNodeChild(schemaNode.getIdentifier());
         if (child != null) {
            validatorResultBuilder.addRecord(ModelUtil.reportDuplicateError(child, schemaNode));
            schemaNode.setErrorStatement(true);
            return validatorResultBuilder.build();
         }

         if (this.self != null) {
            SchemaNode parent;
            if (schemaNode instanceof Action) {
               if (!(this.self instanceof SchemaNode)) {
                  validatorResultBuilder.addRecord(ModelUtil.reportError(schemaNode,
                          ErrorCode.ACTION_NOT_TOP.getFieldName()));
                  return validatorResultBuilder.build();
               }

               parent = (SchemaNode)this.self;
               SchemaNodeContainer closestAncestorNode = parent.getClosestAncestorNode();
               if (!(this.self instanceof DataNode) && closestAncestorNode instanceof YangSchemaContext) {
                  validatorResultBuilder.addRecord(ModelUtil.reportError(schemaNode,
                          ErrorCode.ACTION_NOT_TOP.getFieldName()));
                  return validatorResultBuilder.build();
               }

               if (parent.getSchemaTreeType() == SchemaTreeType.INPUTTREE
                       || parent.getSchemaTreeType() == SchemaTreeType.OUTPUTTREE
                       || parent.getSchemaTreeType() == SchemaTreeType.NOTIFICATIONTREE) {
                  validatorResultBuilder.addRecord(ModelUtil.reportError(schemaNode,
                          ErrorCode.ACTION_IN_DATATREE.getFieldName()));
                  return validatorResultBuilder.build();
               }
            } else if (schemaNode instanceof Notification && this.self instanceof SchemaNode) {
               parent = (SchemaNode)this.self;
               if (parent.getSchemaTreeType() == SchemaTreeType.INPUTTREE
                       || parent.getSchemaTreeType() == SchemaTreeType.OUTPUTTREE
                       || parent.getSchemaTreeType() == SchemaTreeType.NOTIFICATIONTREE) {
                  validatorResultBuilder.addRecord(ModelUtil.reportError(schemaNode,
                          ErrorCode.NOTIFICATION_NOT_IN_DATATREE.getFieldName()));
                  return validatorResultBuilder.build();
               }
            }
         }
      }

      this.schemaNodes.add(schemaNode);
      if (this.self != null) {
         schemaNode.setParentSchemaNode(this.self);
      }
      //set tree type
      if(this.self instanceof SchemaNode){
         //inherit parent's tree type by default
         schemaNode.setSchemaTreeType(((SchemaNode) self).getSchemaTreeType());
         if(schemaNode instanceof Input ){
            schemaNode.setSchemaTreeType(SchemaTreeType.INPUTTREE);
         } else if(schemaNode instanceof Output ){
            schemaNode.setSchemaTreeType(SchemaTreeType.OUTPUTTREE);
         }

      } else {
         //set tree type according to schema node
         if(schemaNode instanceof DataDefinition){
            schemaNode.setSchemaTreeType(SchemaTreeType.DATATREE);
         } else if(schemaNode instanceof Notification){
            schemaNode.setSchemaTreeType(SchemaTreeType.NOTIFICATIONTREE);
         }
      }
      if( (self instanceof YangData)
              || (self instanceof YangStructure)){
         schemaNode.setSchemaTreeType(SchemaTreeType.YANGDATATREE);
      }

      return validatorResultBuilder.build();
   }

   public ValidatorResult addSchemaNodeChildren(List<SchemaNode> schemaNodes) {
      ValidatorResultBuilder validatorResultBuilder = new ValidatorResultBuilder();

      for (SchemaNode node : schemaNodes) {
         ValidatorResult result = this.addSchemaNodeChild(node);
         validatorResultBuilder.merge(result);
      }

      return validatorResultBuilder.build();
   }

   public SchemaNode getSchemaNodeChild(QName identifier) {
      for (SchemaNode schemaNode : this.schemaNodes) {
         if (schemaNode.getIdentifier().equals(identifier)) {
            return schemaNode;
         }
      }
      return null;
   }

   public DataNode getDataNodeChild(QName identifier) {
      for (SchemaNode schemaNode : this.schemaNodes) {
         if (schemaNode instanceof DataNode) {
            if(schemaNode.getIdentifier().equals(identifier)){
               return (DataNode) schemaNode;
            }

         }else if ((schemaNode instanceof VirtualSchemaNode)
                 || (schemaNode instanceof Choice)
                 || (schemaNode instanceof Case)){
            SchemaNodeContainer schemaNodeContainer = (SchemaNodeContainer) schemaNode;
            DataNode node = schemaNodeContainer.getDataNodeChild(identifier);
            if (node != null) {
               return node;
            }
         }
      }

      return null;
   }

   public List<DataNode> getDataNodeChildren() {
      List<DataNode> dataNodeChildren = new ArrayList<>();
      for (SchemaNode schemaNode : this.schemaNodes) {
         if (schemaNode instanceof DataNode) {
            dataNodeChildren.add((DataNode) schemaNode);
         }
         else if ((schemaNode instanceof VirtualSchemaNode)
                 || (schemaNode instanceof Choice)
                 || (schemaNode instanceof Case)){
            SchemaNodeContainer schemaNodeContainer = (SchemaNodeContainer) schemaNode;
            dataNodeChildren.addAll(schemaNodeContainer.getDataNodeChildren());
         }
      }

      return dataNodeChildren;

   }

   @Override
   public List<SchemaNode> getTreeNodeChildren() {
      List<SchemaNode> treeNodeChildren = new ArrayList<>();
      for (SchemaNode schemaNode : this.schemaNodes) {
         if (schemaNode instanceof TreeNode) {
            treeNodeChildren.add(schemaNode);
         } else {
            SchemaNodeContainer schemaNodeContainer = (SchemaNodeContainer) schemaNode;
            treeNodeChildren.addAll(schemaNodeContainer.getTreeNodeChildren());
         }
      }

      return treeNodeChildren;
   }

   @Override
   public SchemaNode getTreeNodeChild(QName identifier) {
      for (SchemaNode schemaNode : this.schemaNodes) {
         if (schemaNode instanceof TreeNode) {
            if(schemaNode.getIdentifier().equals(identifier)){
               return  schemaNode;
            }

         }else {
            SchemaNodeContainer schemaNodeContainer = (SchemaNodeContainer) schemaNode;
            SchemaNode node = schemaNodeContainer.getTreeNodeChild(identifier);
            if (node != null) {
               return node;
            }
         }
      }

      return null;
   }

   @Override
   public List<SchemaNode> getEffectiveSchemaNodeChildren(boolean ignoreNamespace) {
      List<SchemaNode> effectiveSchemaNodes = new ArrayList<>();

      for(SchemaNode schemaNode:schemaNodes){
         if(!schemaNode.isActive()){
            continue;
         }
         if(schemaNode.getContext().getNamespace() == null){
            continue;
         }
         if(!ignoreNamespace && (this.getYangContext() != null) && (this.getYangContext().getNamespace() != null)
                 && !this.getYangContext().getNamespace().equals(schemaNode.getContext().getNamespace())){
            continue;
         }
         if(schemaNode instanceof VirtualSchemaNode){
            VirtualSchemaNode virtualSchemaNode = (VirtualSchemaNode) schemaNode;
            effectiveSchemaNodes.addAll(virtualSchemaNode.getEffectiveSchemaNodeChildren(ignoreNamespace));
            continue;
         }
         effectiveSchemaNodes.add(schemaNode);

      }
      return effectiveSchemaNodes;
   }

   public void removeSchemaNodeChild(QName identifier) {
      SchemaNode target = null;

      for (SchemaNode schemaNode : this.schemaNodes) {
         if (schemaNode instanceof VirtualSchemaNode) {
            VirtualSchemaNode virtualSchemaNode = (VirtualSchemaNode) schemaNode;
            virtualSchemaNode.removeSchemaNodeChild(identifier);
         } else if (schemaNode.getIdentifier().equals(identifier)) {
            target = schemaNode;
         }
      }

      if (null != target) {
         this.schemaNodes.remove(target);
      }

   }

   public void removeSchemaNodeChild(SchemaNode schemaNode) {
      if (this.schemaNodes.contains(schemaNode)) {
         this.schemaNodes.remove(schemaNode);
      } else {

         for (SchemaNode node : this.schemaNodes) {
            if (node instanceof VirtualSchemaNode) {
               VirtualSchemaNode virtualSchemaNode = (VirtualSchemaNode) node;
               virtualSchemaNode.removeSchemaNodeChild(schemaNode);
            }
         }

      }
   }

   public void removeSchemaNodeChildren(){
      schemaNodes.clear();
   }

   public SchemaNode getMandatoryDescendant() {
      for (SchemaNode child : this.schemaNodes) {
         if (!(child instanceof MandatorySupport) && !(child instanceof MultiInstancesDataNode)) {
            SchemaNode schemaNode;
            if (child instanceof Container) {
               Container container = (Container) child;
               if (container.getPresence() == null) {
                  schemaNode = container.getMandatoryDescendant();
                  if (schemaNode != null) {
                     return schemaNode;
                  }
               }
            } else if (child instanceof SchemaNodeContainer) {
               SchemaNodeContainer schemaNodeContainer = (SchemaNodeContainer) child;
               schemaNode = schemaNodeContainer.getMandatoryDescendant();
               if (schemaNode != null) {
                  return schemaNode;
               }
            }
         } else if (child.isMandatory()) {
            return child;
         }
      }

     return null;
   }
   @Override
   public int hashCode() {
      return Objects.hash(schemaNodes);
   }
}
