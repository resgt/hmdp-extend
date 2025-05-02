package com.hmdp.model.utils;

import lombok.Data;

import java.io.Serializable;

/**
 * 邮件类
 */
@Data
public class Mail implements Serializable {
    // 收件人
    private String toOne;
    // 邮件主题
    private String subject;
    // 邮件内容
    private String context;
}
