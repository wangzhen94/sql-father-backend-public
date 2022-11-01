package com.yupi.sqlfather.sort;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.collections4.CollectionUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description: SortUtil
 *
 * @author wangzhen
 * @since 2022/10/19 15:52
 */
@Slf4j
public class SortUtil {
    // {类名: {fieldName: Field}}
    private static final Map<String, Map<String, Field>> clzNameFieldMap = new ConcurrentHashMap<>();

    private static final int NO_SWAP = -1;
    private static final int SWAP = 1;
    private static final HanyuPinyinOutputFormat FORMAT = new HanyuPinyinOutputFormat();
    private static final PropertyUtilsBean propertyUtils = new PropertyUtilsBean();


    /**
     * 自定义排序 > 前端分页的情况 > 使用stream流时调用
     *
     * @param clz           dto.clz 用于获取dto的所有get方法
     * @param sortFieldList 要排序的字段,以及排序规则。列表顺序代表优先级
     * @param <T>           dto
     * @return Comparator
     */
    public static <T> Comparator<T> sort(Class<T> clz, List<SortField> sortFieldList) {
        if (CollectionUtils.isNotEmpty(sortFieldList)) {
            return (e1, e2) -> {
                try {
                    Map<String, Field> nameFields = cacheClzNameFields(clz);
                    for (SortField sortField : sortFieldList) {
                        if (sortField.getAsc() == 0) {
                            continue;
                        }
                        Field field = nameFields.get(sortField.getFieldName());
                        if (field == null) {
                            throw new RuntimeException("字段名不正确");
                        }
                        Object v1 = field.get(e1);
                        Object v2 = field.get(e2);
                        boolean asc = sortField.getAsc() > 0;
                        //正序: null在后, 倒序: null在前
                        if (v1 == null && v2 == null) {
                            continue;
                        } else if (v1 == null) {
                            return asc ? NO_SWAP : SWAP;
                        } else if (v2 == null) {
                            return asc ? SWAP : NO_SWAP;
                        } else if (v1.equals(v2)) {
                            continue;
                        }
                        return comparator(v1, v2, asc, sortField.isContainZh());
                    }
                    return 0;
                } catch (IllegalAccessException e) {
                    log.error("{}", e.getMessage());
                    throw new RuntimeException("访问异常");
                }
            };
        }
        return (t, t2) -> 0;
    }

    public static <T> Comparator<T> sort(List<SortField> sortFieldList){
        if(CollectionUtils.isEmpty(sortFieldList)) return (a, b) -> 0;

        return (a, b) -> {
            try {
                for (SortField item : sortFieldList) {
                    if (item.getAsc() == 0) continue;

                    Object v1 = propertyUtils.getProperty(a, item.getFieldName());

                    Object v2 = propertyUtils.getProperty(b, item.getFieldName());

                    boolean asc = item.getAsc() > 0;

                    if (v1 == null && v2 == null)
                        continue;

                    if(v1 == null)
                        return asc ? NO_SWAP : SWAP;

                    if(v2 == null )
                        return asc ? SWAP : NO_SWAP;

                    if(v1.equals(v2))
                        continue;

                    return comparator(v1, v2, asc, item.isContainZh());
                }

            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("访问异常");
            }


            return 0;
        };
    }

    /**
     * 自定义排序 > 后端分页的情况 > 添加到queryWrapper.apply()
     *
     * @param sortFieldList 要排序的字段,以及排序规则。列表顺序代表优先级
     * @return 拼装好的sql
     */
    public static String sortSql(List<SortField> sortFieldList) {
        if (CollectionUtils.isEmpty(sortFieldList)) {
            return "";
        }
        return sortFieldList.stream()
                .filter(e -> e.getAsc() != 0)
                .collect(new SortSqlCollector());
    }

    private static Map<String, Field> cacheClzNameFields(Class<?> clz) {
        Map<String, Field> nameFields = clzNameFieldMap.get(clz.getName());
        if (nameFields == null) {
            Map<String, Field> nameFieldMap = new ConcurrentHashMap<>();
            clzNameFieldMap.put(clz.getName(), nameFieldMap);
            Field[] fields = clz.getDeclaredFields();

            Arrays.stream(fields)
                    .forEach(field -> {
                        field.setAccessible(true);
                        nameFieldMap.put(field.getName(), field);});
            return nameFieldMap;
        }
        return nameFields;
    }

    private static int comparator(Object v1, Object v2, boolean asc, boolean containZh) {
        if (v1 instanceof String) {
            String s1 = (String) v1;
            String s2 = (String) v2;
            if (containZh) {
                try {
                    String p1 = PinyinHelper.toHanYuPinyinString(s1, FORMAT, "", true);
                    String p2 = PinyinHelper.toHanYuPinyinString(s2, FORMAT, "", true);
                    return asc ? p1.compareTo(p2) : p2.compareTo(p1);
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    log.error("{}", e.getMessage());
                    return NO_SWAP;
                }
            } else {
                return asc ? s1.compareTo(s2) : s2.compareTo(s1);
            }
        } else {
            Comparable c1 = (Comparable) v1;
            Comparable c2 = (Comparable) v2;
            return asc ? c1.compareTo(c2) : c2.compareTo(c1);
        }
    }
}
