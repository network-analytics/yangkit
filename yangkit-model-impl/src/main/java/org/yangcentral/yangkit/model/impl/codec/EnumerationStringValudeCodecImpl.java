package org.yangcentral.yangkit.model.impl.codec;

import org.yangcentral.yangkit.base.ErrorCode;
import org.yangcentral.yangkit.model.api.codec.EnumerationStringValueCodec;
import org.yangcentral.yangkit.model.api.codec.YangCodecException;
import org.yangcentral.yangkit.model.api.restriction.Restriction;

public class EnumerationStringValudeCodecImpl extends StringValueCodecImpl<String> implements EnumerationStringValueCodec {
   public String deserialize(Restriction<String> restriction, String input) throws YangCodecException {
      if (!restriction.evaluate(input)) {
         throw new YangCodecException(ErrorCode.INVALID_VALUE.getFieldName());
      } else {
         return input;
      }
   }

   public String serialize(Restriction<String> restriction, String output) throws YangCodecException {
      if (!restriction.evaluate(output)) {
         throw new YangCodecException(ErrorCode.INVALID_VALUE.getFieldName());
      } else {
         return output;
      }
   }
}
