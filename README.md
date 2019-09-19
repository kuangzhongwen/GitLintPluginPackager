# GitLintPluginPackager

每次 git commit 都会通过 git hooks 触发 Lint 检查。检查结果会以 TXT 格式输出到项目根目录下，如果有问题，则会触发 git reset 命令回滚提交。

## git 增量检查的使用

在项目中引用插件 apply plugin: 'GitLintPlugin'

GitLintPlugin 的配置，与 apply plugin 都可以发在顶级 build.gradle 文件中

     gitLintConfig {
        // Lint 检查文件的类型，默认是 .java .kt .xml。可以自定义其他类型的文件
        lintCheckFileType = ".java,.kt,.xml" 
        // 默认是 false, 为 true 的时候会扫描 git commit 时候所有的代码并且输出扫描
        lintReportAll = false 
     } 
每次 git commit 都会通过 git hooks 触发 Lint 检查。检查结果会以 TXT 格式输出到项目根目录下，如果有问题，则会触发 git reset 命令回滚提交。
