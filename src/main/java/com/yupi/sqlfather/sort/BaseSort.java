package com.yupi.sqlfather.sort;

import lombok.Data;

import java.util.List;

/**
 * Description: BaseSort
 *
 * @author wangzhen
 * @since 2022/10/19 15:57
 */
@Data
public class BaseSort {
    // 要排序的字段列表, 优先级高的列表在前
    List<SortField> sortFieldList;
}
