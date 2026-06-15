package com.campus.system.common.util;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;

// 名称排序与标准化工具
public final class NameSortUtil {

    private static final HanyuPinyinOutputFormat PINYIN_FORMAT = new HanyuPinyinOutputFormat();

    static {
        PINYIN_FORMAT.setCaseType(HanyuPinyinCaseType.UPPERCASE);
        PINYIN_FORMAT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        PINYIN_FORMAT.setVCharType(HanyuPinyinVCharType.WITH_V);
    }

    private NameSortUtil() {}

    // 去除首尾空格后的展示名称。
    public static String trimName(String name) {
        if (name == null) {
            return null;
        }
        return name.trim();
    }

    // 忽略大小写的唯一比较值。
    public static String normalizeName(String name) {
        String trimmed = trimName(name);
        if (trimmed == null || trimmed.isEmpty()) {
            return trimmed;
        }
        return trimmed.toLowerCase();
    }

    // 生成拼音首字母排序键，非中文直接按名称排序。
    public static String sortKey(String name) {
        String trimmed = trimName(name);
        if (trimmed == null || trimmed.isEmpty()) {
            return "";
        }
        StringBuilder initials = new StringBuilder();
        for (char ch : trimmed.toCharArray()) {
            if (Character.toString(ch).matches("[\\u4E00-\\u9FA5]")) {
                try {
                    String[] pinyin = PinyinHelper.toHanyuPinyinStringArray(ch, PINYIN_FORMAT);
                    if (pinyin != null && pinyin.length > 0) {
                      initials.append(pinyin[0].charAt(0));
                    }
                } catch (Exception ignored) {
                    initials.append(ch);
                }
            } else {
                initials.append(Character.toUpperCase(ch));
            }
        }
        return initials + "|" + trimmed;
    }
}
