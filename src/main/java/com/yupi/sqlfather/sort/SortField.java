package com.yupi.sqlfather.sort;

import lombok.Data;

/**
 * Description: SortField
 *
 * @author wangzhen
 * @since 2022/10/19 15:51
 */
@Data
public class SortField {
    private String fieldName;
    // 1:升序 -1:降序 0:不排序
    private int asc;
    // 包含中文, 如果是中文字段要转成汉语拼音排序
    private boolean containZh;
}
