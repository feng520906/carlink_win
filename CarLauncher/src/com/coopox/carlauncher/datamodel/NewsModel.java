package com.coopox.carlauncher.datamodel;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-9-26
 * 推送新闻数据模型
 */
public class NewsModel extends Model {
    @Column(name = "Title")
    public String title;

    @Column(name = "Content")
    public String content;

    @Column(name = "BeginTime")
    public long begin;  // 开始生效时间

    @Column(name = "EndTime")
    public long end;   // 失效时间

    /** 播报标志：
     * 0 - 不播报；
     * 1 - 仅播报标题
     * 2 - 仅播报内容
     * 3 - 播报标题及内容 */
    @Column(name = "PlayBack")
    public int playBack;
}
