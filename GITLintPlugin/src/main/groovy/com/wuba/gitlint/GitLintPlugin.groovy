package com.wuba.gitlint

import com.android.tools.lint.XmlReporter
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

        /**
         * lintCheck task
         */
        project.task("lintCheck") << {
            println("Lint check BEGIN =====")
            String[] fileTypesWillFix
            // 获取接入方配置的 lintConfig 属性
            if (project.lintConfig != null) {
                String fileTypes = project.lintConfig.lintCheckFileType
                if (fileTypes != null) {
                    fileTypesWillFix = fileTypes.split(",")
                }
            }
            // 如果未设置，则使用默认的 fileTypes
            if (fileTypesWillFix == null || fileTypesWillFix.length == 0) {
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

            println("lint check files size:" + files.size())
            // 获取 ANDROID_HOME 环境变量
            println(System.getenv("ANDROID_HOME"))

            def cl = new LintToolClient()
            // LintCliFlags 用于设置 Lint 检查的一些标志
            def flag = cl.flags
            flag.setExitCode = true
            /**
             * HtmlReport
             * 输出 HTML 格式的报告
             * 输出路径: /{$rootDir}/lint-all-result.html
             */
            // 是否输出全部的扫描结果
            if (project.lintConfig != null && project.lintConfig.lintReportAll) {
                File outputResult = new File("lint-check-result-all.xml")
                def xmlReporter = new XmlReporter(cl, outputResult)
                flag.reporters.add(xmlReporter)
            }
            // 输出TXT格式的报告
            File lintResult = new File("lint-check-result.txt")
            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(lintResult), "UTF-8"))
            def txtReporter = new LintTxtReporter(cl, lintResult, writer, startIndex, endIndex)
            flag.reporters.add(txtReporter)

            /**
             * 执行 run 方法开始 lint 检查
             *
             * LintIssueRegistry()-> 自定义 Lint 检查规则
             * files -> 需要检查的文件文件
             * result 检查结果 设置 flag.setExitCode = true 时, 有错误的时候返回 1 反之返回 0
             */
            cl.run(new LintIssueRegistry(), files)
            println("lint issue numbers: " + txtReporter.issueNumber)

            // 根据报告中存在的问题进行判断是否需要回退
            if (txtReporter.issueNumber > 0) {
                // 回退 commit
                "git reset HEAD~1".execute(null, project.getRootDir())
            }
            println("============ Lint check END ===============")
        }

        /**
         * gradle task: 将 git hooks 脚本复制到 .git/hooks 文件夹下
         * 根据不同的系统类型复制不同的 git hooks 脚本(现支持 Windows、Linux 两种)
         */
        project.task("installGitHooks").doLast {
            println("OS Type:" + System.getProperty("os.name"))
            File postCommit
            String OSType = System.getProperty("os.name")
            if (OSType.contains("Windows")) {
                postCommit = new File(project.rootDir, "post-commit-windows")
            } else {
                postCommit = new File(project.rootDir, "post-commit")
            }

            project.copy {
                from(postCommit) {
                    rename {
                        String filename ->
                            "post-commit"
                    }
                }
                into new File(project.rootDir, ".git/hooks/")
            }
        }
    }
}

