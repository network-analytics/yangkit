package org.yangcentral.yangkit.model.impl.codec;

import org.yangcentral.yangkit.base.ErrorCode;
import org.yangcentral.yangkit.model.api.codec.UInt64StringValueCodec;
import org.yangcentral.yangkit.model.api.codec.YangCodecException;
import org.yangcentral.yangkit.model.api.restriction.Restriction;

import java.math.BigInteger;

public class UInt64StringValueCodecImpl extends StringValueCodecImpl<BigInteger> implements UInt64StringValueCodec {
   public BigInteger deserialize(Restriction<BigInteger> restriction, String input) throws YangCodecException {
      BigInteger bi;
      try {
         bi = new BigInteger(input);
      } catch (NumberFormatException e) {
         throw new YangCodecException(ErrorCode.INVALID_VALUE.getFieldName());
      }
      if (!restriction.evaluate(bi)) {
         throw new YangCodecException(ErrorCode.INVALID_VALUE.getFieldName());
      } else {
         return bi;
      }
   }

   public String serialize(Restriction<BigInteger> restriction, BigInteger output) throws YangCodecException {
      if (!restriction.evaluate(output)) {
         throw new YangCodecException(ErrorCode.INVALID_VALUE.getFieldName());
      } else {
         return output.toString();
      }
   }
}
