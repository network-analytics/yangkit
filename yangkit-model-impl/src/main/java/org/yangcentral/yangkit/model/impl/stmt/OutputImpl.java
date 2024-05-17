package org.yangcentral.yangkit.model.impl.stmt;

import org.yangcentral.yangkit.base.BuildPhase;
import org.yangcentral.yangkit.base.YangBuiltinKeyword;
import org.yangcentral.yangkit.base.YangContext;
import org.yangcentral.yangkit.base.YangElement;
import org.yangcentral.yangkit.common.api.QName;
import org.yangcentral.yangkit.common.api.validate.ValidatorResult;
import org.yangcentral.yangkit.common.api.validate.ValidatorResultBuilder;
import org.yangcentral.yangkit.model.api.schema.SchemaPath;
import org.yangcentral.yangkit.model.api.stmt.DataDefinition;
import org.yangcentral.yangkit.model.api.stmt.DataNode;
import org.yangcentral.yangkit.model.api.stmt.Grouping;
import org.yangcentral.yangkit.model.api.stmt.Must;
import org.yangcentral.yangkit.model.api.stmt.Output;
import org.yangcentral.yangkit.model.api.stmt.SchemaNode;
import org.yangcentral.yangkit.model.api.stmt.SchemaNodeContainer;
import org.yangcentral.yangkit.model.api.stmt.Typedef;
import org.yangcentral.yangkit.model.api.stmt.YangBuiltinStatement;
import org.yangcentral.yangkit.model.api.stmt.YangStatement;
import org.yangcentral.yangkit.model.impl.schema.AbsoluteSchemaPath;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OutputImpl extends SchemaNodeImpl implements Output {
   private final DataDefContainerImpl dataDefContainer = new DataDefContainerImpl();
   private final GroupingDefContainerImpl groupingDefContainer = new GroupingDefContainerImpl();
   private final SchemaNodeContainerImpl schemaNodeContainer = new SchemaNodeContainerImpl(this);
   private final TypedefContainerImpl typedefContainer = new TypedefContainerImpl();
   private final MustSupportImpl mustSupport = new MustSupportImpl();
   private QName identifier;

   public OutputImpl(String argStr) {
      super("output");
   }

   public void setContext(YangContext context) {
      super.setContext(context);
      this.dataDefContainer.setYangContext(context);
      this.groupingDefContainer.setYangContext(context);
      this.schemaNodeContainer.setYangContext(context);
      this.typedefContainer.setYangContext(context);
      this.mustSupport.setContextNode(this);
      this.mustSupport.setSelf(this);
   }

   public List<DataDefinition> getDataDefChildren() {
      return this.dataDefContainer.getDataDefChildren();
   }

   public DataDefinition getDataDefChild(String name) {
      return this.dataDefContainer.getDataDefChild(name);
   }

   public ValidatorResult addDataDefChild(DataDefinition dataDefinition) {
      return this.dataDefContainer.addDataDefChild(dataDefinition);
   }

   public List<Grouping> getGroupings() {
      return this.groupingDefContainer.getGroupings();
   }

   public Grouping getGrouping(String name) {
      return this.groupingDefContainer.getGrouping(name);
   }

   public List<SchemaNode> getSchemaNodeChildren() {
      return this.schemaNodeContainer.getSchemaNodeChildren();
   }

   public ValidatorResult addSchemaNodeChild(SchemaNode schemaNode) {
      return this.schemaNodeContainer.addSchemaNodeChild(schemaNode);
   }

   public ValidatorResult addSchemaNodeChildren(List<SchemaNode> schemaNodes) {
      return this.schemaNodeContainer.addSchemaNodeChildren(schemaNodes);
   }

   public SchemaNode getSchemaNodeChild(QName identifier) {
      return this.schemaNodeContainer.getSchemaNodeChild(identifier);
   }

   public DataNode getDataNodeChild(QName identifier) {
      return this.schemaNodeContainer.getDataNodeChild(identifier);
   }

   public List<DataNode> getDataNodeChildren() {
      return this.schemaNodeContainer.getDataNodeChildren();
   }

   @Override
   public List<SchemaNode> getTreeNodeChildren() {
      return schemaNodeContainer.getTreeNodeChildren();
   }

   @Override
   public SchemaNode getTreeNodeChild(QName identifier) {
      return schemaNodeContainer.getTreeNodeChild(identifier);
   }

   public void removeSchemaNodeChild(QName identifier) {
      this.schemaNodeContainer.removeSchemaNodeChild(identifier);
   }

   public void removeSchemaNodeChild(SchemaNode schemaNode) {
      this.schemaNodeContainer.removeSchemaNodeChild(schemaNode);
   }

   public SchemaNode getMandatoryDescendant() {
      return this.schemaNodeContainer.getMandatoryDescendant();
   }

   public List<Typedef> getTypedefs() {
      return this.typedefContainer.getTypedefs();
   }

   public Typedef getTypedef(int index) {
      return this.typedefContainer.getTypedef(index);
   }

   public Typedef getTypedef(String defName) {
      return this.typedefContainer.getTypedef(defName);
   }

   public QName getYangKeyword() {
      return YangBuiltinKeyword.OUTPUT.getQName();
   }

   public Must getMust(int index) {
      return this.mustSupport.getMust(index);
   }

   public Must getMust(String condition) {
      return this.mustSupport.getMust(condition);
   }

   public List<Must> getMusts() {
      return this.mustSupport.getMusts();
   }

   public void setMusts(List<Must> musts) {
      this.mustSupport.setMusts(musts);
   }

   public ValidatorResult addMust(Must must) {
      return this.mustSupport.addMust(must);
   }

   public void removeMust(String condition) {
      this.mustSupport.removeMust(condition);
   }

   public ValidatorResult updateMust(Must must) {
      return this.mustSupport.updateMust(must);
   }

   public ValidatorResult validateMusts() {
      return this.mustSupport.validateMusts();
   }
   @Override
   public boolean checkChild(YangStatement subStatement) {
      boolean result = super.checkChild(subStatement);
      if(!result){
         return false;
      }
      YangBuiltinKeyword builtinKeyword = YangBuiltinKeyword.from(subStatement.getYangKeyword());
      switch (builtinKeyword){
         case TYPEDEF:{
            return getTypedef(subStatement.getArgStr()) == null;
         }
         case GROUPING:{
            return getGrouping(subStatement.getArgStr()) == null;
         }
         case CONTAINER:
         case LIST:
         case LEAF:
         case LEAFLIST:
         case ANYDATA:
         case ANYXML:
         case CHOICE: {
            return !getContext().getSchemaNodeIdentifierCache().containsKey(subStatement.getArgStr());
         }
         case MUST:{
            return getMust(subStatement.getArgStr()) == null;
         }
         default:{
            return true;
         }
      }
   }

   @Override
   protected void clearSelf() {
      this.dataDefContainer.removeDataDefs();
      this.groupingDefContainer.removeGroupings();
      this.typedefContainer.removeTypedefs();
      mustSupport.removeMusts();
      this.schemaNodeContainer.removeSchemaNodeChildren();
      identifier = null;
      super.clearSelf();
   }

   protected ValidatorResult initSelf() {
      ValidatorResultBuilder validatorResultBuilder = new ValidatorResultBuilder(super.initSelf());

      for (YangElement subElement : this.getSubElements()) {
         if (subElement instanceof YangBuiltinStatement) {
            YangBuiltinStatement builtinStatement = (YangBuiltinStatement) subElement;
            YangBuiltinKeyword builtinKeyword = YangBuiltinKeyword.from(builtinStatement.getYangKeyword());
            switch (builtinKeyword) {
               case ANYDATA:
               case ANYXML:
               case CHOICE:
               case CONTAINER:
               case LEAF:
               case LEAFLIST:
               case LIST:
               case USES:
                  DataDefinition dataDefinition = (DataDefinition) builtinStatement;
                  validatorResultBuilder.merge(this.addDataDefChild(dataDefinition));
                  break;
               case GROUPING:
                  Grouping grouping = (Grouping) builtinStatement;
                  validatorResultBuilder.merge(this.groupingDefContainer.addGrouping(grouping));
                  break;
               case TYPEDEF:
                  Typedef typedef = (Typedef) builtinStatement;
                  validatorResultBuilder.merge(this.typedefContainer.addTypedef(typedef));
                  break;
               case MUST: {
                  Must must = (Must) builtinStatement;
                  validatorResultBuilder.merge(this.mustSupport.addMust(must));
                  break;
               }
            }
         }
      }

      return validatorResultBuilder.build();
   }

   protected ValidatorResult validateSelf() {
      ValidatorResultBuilder validatorResultBuilder = new ValidatorResultBuilder(super.validateSelf());
      validatorResultBuilder.merge(this.validateMusts());
      return validatorResultBuilder.build();
   }

   protected ValidatorResult buildSelf(BuildPhase phase) {
      ValidatorResultBuilder validatorResultBuilder = new ValidatorResultBuilder(super.buildSelf(phase));
      if (Objects.requireNonNull(phase) == BuildPhase.SCHEMA_BUILD) {
         for (DataDefinition dataDefinition : this.getDataDefChildren()) {
            validatorResultBuilder.merge(this.addSchemaNodeChild(dataDefinition));
         }
      }
      return validatorResultBuilder.build();
   }

   public boolean isConfig() {
      return false;
   }

   public QName getIdentifier() {
      if (this.identifier != null) {
         return this.identifier;
      } else {
         this.identifier = new QName(this.getContext().getNamespace(), this.getArgStr());
         return this.identifier;
      }
   }

//   public SchemaPath.Absolute getSchemaPath() {
//      SchemaPath.Absolute schemaPath = super.getSchemaPath();
//      if (null == schemaPath) {
//         SchemaNodeContainer parent = this.getParentSchemaNode();
//         if (parent instanceof SchemaNode) {
//            SchemaPath parentSchemaPath = ((SchemaNode)parent).getSchemaPath();
//            if(parentSchemaPath == null){
//               return null;
//            }
//            schemaPath = new AbsoluteSchemaPath(parentSchemaPath.getPath());
//            schemaPath.addStep(this.getIdentifier());
//         } else {
//            schemaPath = new AbsoluteSchemaPath();
//            schemaPath.addStep(this.getIdentifier());
//         }
//      }
//
//      return schemaPath;
//   }

   @Override
   public List<SchemaNode> getEffectiveSchemaNodeChildren(boolean ignoreNamespace) {
      return schemaNodeContainer.getEffectiveSchemaNodeChildren(ignoreNamespace);
   }
   public List<YangStatement> getEffectiveSubStatements() {
      List<YangStatement> statements = new ArrayList<>();
      statements.addAll(getEffectiveSchemaNodeChildren());
      statements.addAll(this.groupingDefContainer.getGroupings());
      statements.addAll(this.typedefContainer.getTypedefs());
      statements.addAll(this.mustSupport.getMusts());
      statements.addAll(super.getEffectiveSubStatements());
      return statements;
   }
}
