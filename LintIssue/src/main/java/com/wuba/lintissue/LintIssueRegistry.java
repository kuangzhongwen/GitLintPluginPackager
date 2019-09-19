package com.wuba.lintissue;

import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.Issue;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * 自定义 lint 规则注册
 *
 * @author kuang on 2019/09/19
 */
public class LintIssueRegistry extends IssueRegistry {

    @NotNull
    @Override
    public List<Issue> getIssues() {
        return null;
    }
}
