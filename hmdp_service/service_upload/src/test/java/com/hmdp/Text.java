package com.hmdp;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.hmdp.common.utils.SystemConstants;

import java.io.File;

public class Text {

    public static void main(String[] args) {
        // 获取后缀
        String suffix = StrUtil.subAfter("sub.jpg", ".", true);
        // 生成目录
        String name = UUID.randomUUID().toString();
        int hash = name.hashCode();
        int d1 = hash & 0xF;
        int d2 = (hash >> 4) & 0xF;
        // 判断目录是否存在
        File dir = new File(SystemConstants.IMAGE_UPLOAD_DIR, StrUtil.format("/blogs/{}/{}", d1, d2));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        // 生成文件名
        String format = StrUtil.format("/blogs/{}/{}/{}.{}", d1, d2, name, suffix);
    }

}
