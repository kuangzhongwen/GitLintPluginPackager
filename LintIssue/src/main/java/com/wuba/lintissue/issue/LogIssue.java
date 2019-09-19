package com.wuba.lintissue.issue;

import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;

/**
 * lint 规则：log 输出校验
 *
 * @author kuang on 2019/09/19
 */
public class LogIssue extends Detector implements Detector.UastScanner {

    public static final Issue ISSUE = Issue.create(
        "Log",
        "请使用项目中提供的 Log 工具",
        "避免在项目中直接使用 android.log 以及 system.out",
        Category.PERFORMANCE,
        5,
        Severity.ERROR,
        new Implementation(LogIssue.class, Scope.JAVA_FILE_SCOPE)
    );
}
