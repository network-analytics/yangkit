package org.yangcentral.yangkit.model.impl.codec;

import org.yangcentral.yangkit.base.ErrorCode;
import org.yangcentral.yangkit.model.api.codec.BitsStringValueCodec;
import org.yangcentral.yangkit.model.api.codec.YangCodecException;
import org.yangcentral.yangkit.model.api.restriction.Restriction;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

public class BitsStringValueCodecImpl extends StringValueCodecImpl<List<String>> implements BitsStringValueCodec {
   public List<String> deserialize(Restriction<List<String>> restriction, String input) throws YangCodecException {
      String[] splitStr;
      try {
         splitStr = input.split(" ");
      } catch (PatternSyntaxException el) {
         throw new YangCodecException(ErrorCode.INVALID_VALUE.getFieldName());
      }
      List<String> ret = new ArrayList<>();
      int length = splitStr.length;

      for(int i = 0; i < length; ++i) {
         String str = splitStr[i];
         str = str.trim();
         if (str.length() > 0) {
            ret.add(str);
         }
      }

      boolean bool = restriction.evaluate(ret);
      if (!bool) {
         throw new YangCodecException(ErrorCode.INVALID_VALUE.getFieldName());
      } else {
         return ret;
      }
   }

   public String serialize(Restriction<List<String>> restriction, List<String> output) throws YangCodecException {
      boolean bool = restriction.evaluate(output);
      if (!bool) {
         throw new YangCodecException(ErrorCode.INVALID_VALUE.getFieldName());
      } else {
         StringBuilder sb = new StringBuilder();
         for (String str : output) {
            sb.append(str);
            sb.append(" ");
         }

         return sb.toString().trim();
      }
   }
}
