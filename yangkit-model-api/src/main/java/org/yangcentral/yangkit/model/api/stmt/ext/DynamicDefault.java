package org.yangcentral.yangkit.model.api.stmt.ext;

import org.yangcentral.yangkit.common.api.QName;
import org.yangcentral.yangkit.model.api.stmt.YangUnknown;

public interface DynamicDefault extends YangUnknown {
    QName YANG_KEYWORD = new QName("urn:huawei:yang:huawei-extension","dynamic-default");
}
