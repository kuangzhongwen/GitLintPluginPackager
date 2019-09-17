package com.wuba.gitlint

import com.android.tools.lint.LintCliClient
import com.android.tools.lint.Warning
import com.android.tools.lint.client.api.LintRequest
import com.android.tools.lint.detector.api.Project

/**
 * 重写 LintClient
 * 用来创建 LintRequest 以便 Lint 扫描时使用,这里在 LintRequest 中加入了 git 提交的增量文件
 * LintCliClient run() 方法是 Lint 扫描的入口
 *
 * @author kuang on 2019/09/17
 */
class LintToolClient extends LintCliClient {

    @Override
    protected LintRequest createLintRequest(List<File> files) {
        LintRequest request = super.createLintRequest(files)
        for (Project project : request.getProjects()) {
            for (File file : files) {
                project.addFile(file)
            }
        }
        return new LintRequest(this, files)
    }

    /**
     * 获取扫描文件得到的结果
     * Warning 类包含了文件路径、问题描述、问题所在文件的行号等信息
     *
     * @return Warnings
     */
    List<Warning> getIssueWarnings() {
        return warnings
    }
}