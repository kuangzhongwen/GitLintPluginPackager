package com.wuba.gitlint

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 通过自定义的 Lint 规则检查增量代码，hook git 流程，如 lint 检查出 error, 则终止 git 流程，回滚代码.
 *
 * @author kuang on 2019/09/17
 */
class GitLintPlugin implements Plugin<Project> {

    /**
     * 需要检测的文件类型 - 默认
     */
    def fileTypes = [".java", ".kt", ".xml"] as String[]

    @Override
    void apply(Project project) {
        // 将 LintConfig 类作为 Project 的一个属性值，key 为 lintConfig
        project.extensions.create("lintConfig", LintConfig.class)

        // 新建一个 lintCheck task
        project.task("lintCheck") << {
            println("task lintCheck BEGIN =====")
            String[] fileTypesWillFix
            // 获取接入方配置的 lintConfig 属性
            if (project.lintConfig != null) {
                String fileTypes = project.lintConfig.lintCheckFileType
                if (fileTypes != null) {
                    fileTypesWillFix = fileTypes.split(",")
                }
            }
            // 如果未设置，则使用默认的 fileTypes
            if (fileTypesWillFix == null || fileTypesWillFix.length <= 0) {
                fileTypesWillFix = fileTypes
            }

            // 获取 commit 变更
            List<String> commitChange = Utils.getCommitChange(project)
            List<File> files = new ArrayList<>()
            File file
            List<Integer> startIndex = new ArrayList<>()
            List<Integer> endIndex = new ArrayList<>()

            for (String path : commitChange) {
                println("commit change file path: " + path)
                if (Utils.isMatchFile(fileTypesWillFix, path)) {
                    file = new File(path)
                    files.add(file)
                    Utils.getFileChangeStatus(path, project, startIndex, endIndex)
                }
            }

            println("need checked files size:" + files.size())
            println(System.getenv("ANDROID_HOME"))
        }
    }
}

